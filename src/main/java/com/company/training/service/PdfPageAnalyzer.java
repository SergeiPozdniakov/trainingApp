package com.company.training.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfPageAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(PdfPageAnalyzer.class);

    /**
     * Анализирует страницу PDF и возвращает структурированную информацию
     */
    public PageAnalysisResult analyzePageStructure(String pageText, int pageNumber) {
        PageAnalysisResult result = new PageAnalysisResult();
        result.setPageNumber(pageNumber);

        // 1. Извлекаем все возможные ФИО с контекстом
        List<NameWithContext> namesWithContext = extractNamesWithContext(pageText);
        result.setNamesWithContext(namesWithContext);

        // 2. Извлекаем все даты с контекстом
        List<DateWithContext> datesWithContext = extractDatesWithContext(pageText);
        result.setDatesWithContext(datesWithContext);

        // 3. Извлекаем номера протоколов с контекстом
        List<ProtocolNumberWithContext> protocolNumbersWithContext = extractProtocolNumbersWithContext(pageText);
        result.setProtocolNumbersWithContext(protocolNumbersWithContext);

        // 4. Определяем тип документа
        DocumentType documentType = determineDocumentType(pageText);
        result.setDocumentType(documentType);

        // 5. Ищем таблицу с данными
        TableStructure tableStructure = findTableStructure(pageText);
        result.setTableStructure(tableStructure);

        return result;
    }

    /**
     * Извлекает ФИО с контекстом (окружающим текстом)
     */
    private List<NameWithContext> extractNamesWithContext(String text) {
        List<NameWithContext> results = new ArrayList<>();

        // Паттерн для поиска ФИО
        Pattern namePattern = Pattern.compile("\\b([А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+)?)\\b");
        Matcher matcher = namePattern.matcher(text);

        while (matcher.find()) {
            String fullName = matcher.group(1);
            int start = Math.max(0, matcher.start() - 50);
            int end = Math.min(text.length(), matcher.end() + 50);
            String context = text.substring(start, end);

            NameWithContext nameWithContext = new NameWithContext();
            nameWithContext.setFullName(fullName);
            nameWithContext.setContext(context);
            nameWithContext.setPositionInText(matcher.start());

            results.add(nameWithContext);
        }

        return results;
    }

    /**
     * Извлекает даты с контекстом
     */
    private List<DateWithContext> extractDatesWithContext(String text) {
        List<DateWithContext> results = new ArrayList<>();

        Pattern datePattern = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{4})");
        Matcher matcher = datePattern.matcher(text);

        while (matcher.find()) {
            String date = matcher.group(1);
            int start = Math.max(0, matcher.start() - 30);
            int end = Math.min(text.length(), matcher.end() + 30);
            String context = text.substring(start, end);

            DateWithContext dateWithContext = new DateWithContext();
            dateWithContext.setDate(date);
            dateWithContext.setContext(context);

            results.add(dateWithContext);
        }

        return results;
    }

    /**
     * Извлекает номера протоколов с контекстом
     */
    private List<ProtocolNumberWithContext> extractProtocolNumbersWithContext(String text) {
        List<ProtocolNumberWithContext> results = new ArrayList<>();

        Pattern numberPattern = Pattern.compile("\\b(\\d{6,12})\\b");
        Matcher matcher = numberPattern.matcher(text);

        while (matcher.find()) {
            String number = matcher.group(1);
            int start = Math.max(0, matcher.start() - 30);
            int end = Math.min(text.length(), matcher.end() + 30);
            String context = text.substring(start, end);

            ProtocolNumberWithContext protocolNumberWithContext = new ProtocolNumberWithContext();
            protocolNumberWithContext.setNumber(number);
            protocolNumberWithContext.setContext(context);

            results.add(protocolNumberWithContext);
        }

        return results;
    }

    /**
     * Определяет тип документа
     */
    private DocumentType determineDocumentType(String text) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("протокол проверки знаний") &&
                lowerText.contains("охрана труда")) {
            return DocumentType.OCCUPATIONAL_SAFETY_PROTOCOL;
        } else if (lowerText.contains("протокол") &&
                lowerText.contains("проверка знаний")) {
            return DocumentType.GENERAL_PROTOCOL;
        } else if (lowerText.contains("аттестация") ||
                lowerText.contains("свидетельство")) {
            return DocumentType.CERTIFICATE;
        }

        return DocumentType.UNKNOWN;
    }

    /**
     * Ищет табличную структуру в тексте
     */
    private TableStructure findTableStructure(String text) {
        TableStructure tableStructure = new TableStructure();

        // Разбиваем на строки
        String[] lines = text.split("\n");
        List<TableRow> rows = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Проверяем, похожа ли строка на заголовок таблицы
            if (line.toLowerCase().contains("№ п/п") ||
                    line.toLowerCase().contains("фамилия") ||
                    line.toLowerCase().contains("фио")) {
                tableStructure.setHeaderLine(i);
                tableStructure.setHeaderText(line);
            }

            // Проверяем, похожа ли строка на строку с данными
            // (содержит несколько слов, разделенных пробелами/табуляцией)
            String[] parts = line.split("\\s{2,}");
            if (parts.length >= 3) {
                TableRow row = new TableRow();
                row.setLineNumber(i);
                row.setParts(Arrays.asList(parts));
                rows.add(row);
            }
        }

        tableStructure.setRows(rows);
        return tableStructure;
    }

    // Вложенные классы для структурированных результатов

    public static class PageAnalysisResult {
        private int pageNumber;
        private List<NameWithContext> namesWithContext;
        private List<DateWithContext> datesWithContext;
        private List<ProtocolNumberWithContext> protocolNumbersWithContext;
        private DocumentType documentType;
        private TableStructure tableStructure;

        // геттеры и сеттеры
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

        public List<NameWithContext> getNamesWithContext() { return namesWithContext; }
        public void setNamesWithContext(List<NameWithContext> namesWithContext) { this.namesWithContext = namesWithContext; }

        public List<DateWithContext> getDatesWithContext() { return datesWithContext; }
        public void setDatesWithContext(List<DateWithContext> datesWithContext) { this.datesWithContext = datesWithContext; }

        public List<ProtocolNumberWithContext> getProtocolNumbersWithContext() { return protocolNumbersWithContext; }
        public void setProtocolNumbersWithContext(List<ProtocolNumberWithContext> protocolNumbersWithContext) { this.protocolNumbersWithContext = protocolNumbersWithContext; }

        public DocumentType getDocumentType() { return documentType; }
        public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

        public TableStructure getTableStructure() { return tableStructure; }
        public void setTableStructure(TableStructure tableStructure) { this.tableStructure = tableStructure; }
    }

    public static class NameWithContext {
        private String fullName;
        private String context;
        private int positionInText;

        // геттеры и сеттеры
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }

        public int getPositionInText() { return positionInText; }
        public void setPositionInText(int positionInText) { this.positionInText = positionInText; }
    }

    public static class DateWithContext {
        private String date;
        private String context;

        // геттеры и сеттеры
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }

    public static class ProtocolNumberWithContext {
        private String number;
        private String context;

        // геттеры и сеттеры
        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }

        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }

    public enum DocumentType {
        OCCUPATIONAL_SAFETY_PROTOCOL,
        GENERAL_PROTOCOL,
        CERTIFICATE,
        UNKNOWN
    }

    public static class TableStructure {
        private Integer headerLine;
        private String headerText;
        private List<TableRow> rows = new ArrayList<>();

        // геттеры и сеттеры
        public Integer getHeaderLine() { return headerLine; }
        public void setHeaderLine(Integer headerLine) { this.headerLine = headerLine; }

        public String getHeaderText() { return headerText; }
        public void setHeaderText(String headerText) { this.headerText = headerText; }

        public List<TableRow> getRows() { return rows; }
        public void setRows(List<TableRow> rows) { this.rows = rows; }
    }

    public static class TableRow {
        private int lineNumber;
        private List<String> parts = new ArrayList<>();

        // геттеры и сеттеры
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

        public List<String> getParts() { return parts; }
        public void setParts(List<String> parts) { this.parts = parts; }
    }
}