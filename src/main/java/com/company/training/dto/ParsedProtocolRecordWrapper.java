package com.company.training.dto;

import java.util.List;

public class ParsedProtocolRecordWrapper {
    private List<ParsedProtocolRecord> records;

    // Геттеры и сеттеры
    public List<ParsedProtocolRecord> getRecords() { return records; }
    public void setRecords(List<ParsedProtocolRecord> records) { this.records = records; }
}