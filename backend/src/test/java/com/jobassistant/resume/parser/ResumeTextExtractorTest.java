package com.jobassistant.resume.parser;

import com.jobassistant.shared.FileProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ResumeTextExtractorTest {

    private ResumeTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ResumeTextExtractor();
    }

    @Test
    void shouldExtractTextFromValidPdf() throws IOException {
        byte[] pdfBytes = createPdfWithText(
                "Gustavo Silva",
                "Desenvolvedor Full Stack Pleno",
                "Experiencia com Java 21, Spring Boot, React, TypeScript e PostgreSQL."
        );

        String extracted = extractor.extractText(pdfBytes, "PDF");

        assertThat(extracted)
                .contains("Gustavo Silva")
                .contains("Desenvolvedor Full Stack Pleno")
                .contains("Spring Boot");
    }

    @Test
    void shouldExtractTextFromValidDocx() throws IOException {
        byte[] docxBytes = createDocxWithText(
                "Mariana Costa",
                "Engenheira de Software Back-end",
                "Java, Spring Boot, Docker, PostgreSQL, Inglês Avançado"
        );

        String extracted = extractor.extractText(docxBytes, "DOCX");

        assertThat(extracted)
                .contains("Mariana Costa")
                .contains("Engenheira de Software Back-end")
                .contains("PostgreSQL");
    }

    @Test
    void shouldRejectPdfWithoutExtractableText() throws IOException {
        byte[] blankPdfBytes;
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            blankPdfBytes = out.toByteArray();
        }

        assertThatThrownBy(() -> extractor.extractText(blankPdfBytes, "PDF"))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("Não foi possível extrair texto suficiente");
    }

    @Test
    void shouldRejectCorruptedDocument() {
        byte[] corrupted = "%PDF-1.4 invalid corrupted content".getBytes();

        assertThatThrownBy(() -> extractor.extractText(corrupted, "PDF"))
                .isInstanceOf(FileProcessingException.class);
    }

    public static byte[] createPdfWithText(String... lines) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                for (String line : lines) {
                    content.showText(line);
                    content.newLineAtOffset(0, -18);
                }
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    public static byte[] createDocxWithText(String... lines) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String line : lines) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText(line);
            }
            doc.write(out);
            return out.toByteArray();
        }
    }
}
