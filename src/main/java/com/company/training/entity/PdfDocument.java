package com.company.training.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_documents")
public class PdfDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PdfType type;

    @Column(nullable = false)
    private String filePath;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String processingResult;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public enum PdfType {
        OCCUPATIONAL_SAFETY,    // Охрана труда, пожарная безопасность, экология, ГОЧС, первая помощь (Профи-Юг)
        INDUSTRIAL_SAFETY        // Промышленная безопасность
    }

    public enum ProcessingStatus {
        PENDING, PROCESSING, NEEDS_REVIEW, PROCESSED, ERROR
    }

    // Конструкторы
    public PdfDocument() {}

    public PdfDocument(String originalFilename, String storedFilename, PdfType type, String filePath) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.type = type;
        this.filePath = filePath;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public PdfType getType() { return type; }
    public void setType(PdfType type) { this.type = type; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }

    public ProcessingStatus getStatus() { return status; }
    public void setStatus(ProcessingStatus status) { this.status = status; }

    public String getProcessingResult() { return processingResult; }
    public void setProcessingResult(String processingResult) { this.processingResult = processingResult; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}