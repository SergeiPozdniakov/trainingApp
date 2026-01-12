package com.training.dto;

import java.time.LocalDate;

public class CertificateCellDTO {
    private Long certificateId;
    private LocalDate examDate;
    private String protocolNumber;
    private String filePath;
    private boolean applicable;
    private String statusColor;
    private LocalDate nextExamDueDate;

    // Геттеры и сеттеры
    public Long getCertificateId() { return certificateId; }
    public void setCertificateId(Long certificateId) { this.certificateId = certificateId; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getProtocolNumber() { return protocolNumber; }
    public void setProtocolNumber(String protocolNumber) { this.protocolNumber = protocolNumber; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isApplicable() { return applicable; }
    public void setApplicable(boolean applicable) { this.applicable = applicable; }

    public String getStatusColor() { return statusColor; }
    public void setStatusColor(String statusColor) { this.statusColor = statusColor; }

    public LocalDate getNextExamDueDate() { return nextExamDueDate; }
    public void setNextExamDueDate(LocalDate nextExamDueDate) { this.nextExamDueDate = nextExamDueDate; }
}