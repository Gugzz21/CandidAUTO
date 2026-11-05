package com.jobassistant.resume.parser;

import com.jobassistant.shared.FileProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class ResumeTextExtractor {

    private static final int MIN_EXTRACTABLE_CHARS = 15;

    public String extractText(byte[] contentBytes, String fileType) {
        if (contentBytes == null || contentBytes.length == 0) {
            throw new FileProcessingException("O arquivo está vazio e não possui conteúdo para extração.");
        }

        String rawText;
        if ("PDF".equalsIgnoreCase(fileType)) {
            rawText = extractFromPdf(contentBytes);
        } else if ("DOCX".equalsIgnoreCase(fileType)) {
            rawText = extractFromDocx(contentBytes);
        } else {
            throw new FileProcessingException("Tipo de arquivo não suportado para extração: " + fileType);
        }

        String normalized = normalizeWhitespace(rawText);
        if (normalized.length() < MIN_EXTRACTABLE_CHARS) {
            throw new FileProcessingException(
                    "Não foi possível extrair texto suficiente do documento. "
                            + "Verifique se o PDF não é apenas uma imagem digitalizada (OCR não habilitado nesta etapa)."
            );
        }

        return normalized;
    }

    private String extractFromPdf(byte[] contentBytes) {
        try (PDDocument document = Loader.loadPDF(contentBytes)) {
            if (document.isEncrypted()) {
                throw new FileProcessingException("O arquivo PDF está protegido por senha e não pode ser processado.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (FileProcessingException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new FileProcessingException("Não foi possível ler o arquivo PDF. O documento pode estar corrompido.", e);
        }
    }

    private String extractFromDocx(byte[] contentBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(contentBytes);
             XWPFDocument doc = new XWPFDocument(in)) {

            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text.trim()).append("\n");
                }
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringBuilder rowText = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isBlank()) {
                            if (!rowText.isEmpty()) {
                                rowText.append(" | ");
                            }
                            rowText.append(cellText.trim());
                        }
                    }
                    if (!rowText.isEmpty()) {
                        sb.append(rowText).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (IOException | RuntimeException e) {
            throw new FileProcessingException("Não foi possível ler o arquivo DOCX. O documento pode estar corrompido.", e);
        }
    }

    private String normalizeWhitespace(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
