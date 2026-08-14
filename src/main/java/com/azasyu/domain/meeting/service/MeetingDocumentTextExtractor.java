package com.azasyu.domain.meeting.service;

import com.azasyu.domain.meeting.entity.MeetingRecordSourceType;
import com.azasyu.global.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MeetingDocumentTextExtractor {

    public ExtractedDocument extract(MultipartFile file) {
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extension(fileName);
        try {
            return switch (extension) {
                case "txt" -> new ExtractedDocument(MeetingRecordSourceType.TXT, fileName,
                    new String(file.getBytes(), StandardCharsets.UTF_8));
                case "docx" -> new ExtractedDocument(MeetingRecordSourceType.DOCX, fileName, extractDocx(file));
                case "pdf" -> new ExtractedDocument(MeetingRecordSourceType.PDF, fileName, extractPdf(file));
                default -> throw unsupportedFile();
            };
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_READ_FAILED", "문서 내용을 읽을 수 없습니다.");
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (var document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw unsupportedFile();
        }
        return originalFileName.replace('\\', '/').substring(originalFileName.replace('\\', '/').lastIndexOf('/') + 1);
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private ApiException unsupportedFile() {
        return new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_DOCUMENT_TYPE", "TXT, DOCX, PDF 파일만 업로드할 수 있습니다.");
    }

    public record ExtractedDocument(MeetingRecordSourceType sourceType, String fileName, String content) {
    }
}
