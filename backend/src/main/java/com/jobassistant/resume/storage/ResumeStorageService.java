package com.jobassistant.resume.storage;

import com.jobassistant.shared.BadRequestException;
import com.jobassistant.shared.FileProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ResumeStorageService {

    private static final Set<String> ALLOWED_PDF_MIMES = Set.of(
            "application/pdf",
            "application/x-pdf",
            "application/octet-stream"
    );

    private static final Set<String> ALLOWED_DOCX_MIMES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream",
            "application/zip"
    );

    private final Path storageDir;
    private final long maxUploadSizeBytes;

    public ResumeStorageService(
            @Value("${app.storage.resumes-dir:./infra/storage/resumes}") String resumesDir,
            @Value("${app.storage.max-upload-size-bytes:10485760}") long maxUploadSizeBytes
    ) {
        this.storageDir = Path.of(resumesDir).toAbsolutePath().normalize();
        this.maxUploadSizeBytes = maxUploadSizeBytes;
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível inicializar o diretório privado de armazenamento de currículos.", e);
        }
    }

    public StoredResumeFile validateAndStore(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("O arquivo do currículo não pode estar vazio.");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new BadRequestException("O arquivo excede o tamanho máximo permitido de 10 MB.");
        }

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        String fileType;
        String extension;

        if (lowerName.endsWith(".pdf")) {
            fileType = "PDF";
            extension = "pdf";
        } else if (lowerName.endsWith(".docx")) {
            fileType = "DOCX";
            extension = "docx";
        } else {
            throw new BadRequestException("Formato de arquivo não suportado. Envie apenas currículos em PDF ou DOCX.");
        }

        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase(Locale.ROOT).trim()
                : "application/octet-stream";

        if ("PDF".equals(fileType) && !ALLOWED_PDF_MIMES.contains(contentType)) {
            throw new BadRequestException("MIME type inválido para arquivo PDF: " + contentType);
        }
        if ("DOCX".equals(fileType) && !ALLOWED_DOCX_MIMES.contains(contentType)) {
            throw new BadRequestException("MIME type inválido para arquivo DOCX: " + contentType);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new FileProcessingException("Falha ao ler os bytes do arquivo enviado.", e);
        }

        validateMagicBytes(bytes, fileType);

        String storageKey = UUID.randomUUID() + "." + extension;
        Path targetPath = storageDir.resolve(storageKey).normalize();
        if (!targetPath.startsWith(storageDir)) {
            throw new BadRequestException("Caminho de armazenamento inválido.");
        }

        try {
            Files.write(targetPath, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new FileProcessingException("Erro ao armazenar currículo no diretório privado.", e);
        }

        return new StoredResumeFile(originalFilename, storageKey, fileType, bytes.length, bytes);
    }

    private void validateMagicBytes(byte[] bytes, String fileType) {
        if (bytes.length < 5) {
            throw new BadRequestException("O arquivo enviado é inválido ou está corrompido.");
        }
        if ("PDF".equals(fileType)) {
            // Assinatura %PDF-
            if (!(bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46 && bytes[4] == 0x2D)) {
                throw new BadRequestException("O conteúdo do arquivo não corresponde a um documento PDF válido.");
            }
        } else if ("DOCX".equals(fileType)) {
            // Assinatura PK\x03\x04 (ZIP OpenXML)
            if (!(bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04)) {
                throw new BadRequestException("O conteúdo do arquivo não corresponde a um documento DOCX válido.");
            }
        }
    }

    private String sanitizeOriginalFilename(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new BadRequestException("O nome do arquivo não foi informado.");
        }
        String justName = Path.of(rawName).getFileName().toString().trim();
        return justName.replaceAll("[\\r\\n\\t]", "_");
    }

    public record StoredResumeFile(
            String originalFilename,
            String storageKey,
            String fileType,
            long fileSizeBytes,
            byte[] contentBytes
    ) {
    }
}
