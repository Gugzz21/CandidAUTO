package com.jobassistant.resume.storage;

import com.jobassistant.resume.parser.ResumeTextExtractorTest;
import com.jobassistant.shared.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeStorageServiceTest {

    @TempDir
    Path tempDir;

    private ResumeStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new ResumeStorageService(tempDir.toString(), 1024 * 1024); // 1 MB
    }

    @Test
    void shouldValidateAndStorePdfWithInternalStorageKey() throws IOException {
        byte[] pdfBytes = ResumeTextExtractorTest.createPdfWithText("Curriculo Teste PDF", "Desenvolvedor Java Spring Boot");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "meu_curriculo_pessoal.pdf",
                "application/pdf",
                pdfBytes
        );

        ResumeStorageService.StoredResumeFile stored = storageService.validateAndStore(file);

        assertThat(stored.originalFilename()).isEqualTo("meu_curriculo_pessoal.pdf");
        assertThat(stored.fileType()).isEqualTo("PDF");
        assertThat(stored.storageKey()).endsWith(".pdf");
        assertThat(stored.storageKey()).doesNotContain("meu_curriculo_pessoal");
        assertThat(Files.exists(tempDir.resolve(stored.storageKey()))).isTrue();
    }

    @Test
    void shouldRejectUnsupportedFileExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "MZ fake binary".getBytes()
        );

        assertThatThrownBy(() -> storageService.validateAndStore(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Formato de arquivo não suportado");
    }

    @Test
    void shouldRejectFakePdfWithInvalidMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "falso.pdf",
                "application/pdf",
                "Este arquivo nao tem cabecalho PDF real".getBytes()
        );

        assertThatThrownBy(() -> storageService.validateAndStore(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("documento PDF válido");
    }
}
