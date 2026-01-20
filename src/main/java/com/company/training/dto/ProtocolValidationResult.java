package com.company.training.dto;

import java.util.List;

public class ProtocolValidationResult {

    private Long pdfDocumentId;
    private List<ParsedProtocolRecord> records;
    private int totalRecords;
    private int validRecords;
    private List<String> errors;
    private List<String> successes;
    private boolean allValid;

    // Геттеры и сеттеры
    public Long getPdfDocumentId() { return pdfDocumentId; }
    public void setPdfDocumentId(Long pdfDocumentId) { this.pdfDocumentId = pdfDocumentId; }

    public List<ParsedProtocolRecord> getRecords() { return records; }
    public void setRecords(List<ParsedProtocolRecord> records) { this.records = records; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getValidRecords() { return validRecords; }
    public void setValidRecords(int validRecords) { this.validRecords = validRecords; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getSuccesses() { return successes; }
    public void setSuccesses(List<String> successes) { this.successes = successes; }

    public boolean isAllValid() { return allValid; }
    public void setAllValid(boolean allValid) { this.allValid = allValid; }
}