package com.company.training.dto;

import java.time.LocalDate;

public class ParsedProtocolRecord {
    private Long id;
    private String fullName;
    private String position;
    private String department;
    private LocalDate examDate;
    private String registrationNumber;
    private String fileName;
    private Long pdfDocumentId;

    // Поля для сопоставления
    private String matchedFullName;
    private Long matchedEmployeeId;
    private String confidence;

    // Поля для направления обучения
    private String trainingDirection;
    private Long matchedDirectionId;
    private String directionConfidence;
    private Long selectedDirectionId;

    // Новое поле: номер страницы в PDF
    private Integer pageNumber;

    private boolean valid = true;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getPdfDocumentId() { return pdfDocumentId; }
    public void setPdfDocumentId(Long pdfDocumentId) { this.pdfDocumentId = pdfDocumentId; }

    public String getMatchedFullName() { return matchedFullName; }
    public void setMatchedFullName(String matchedFullName) { this.matchedFullName = matchedFullName; }

    public Long getMatchedEmployeeId() { return matchedEmployeeId; }
    public void setMatchedEmployeeId(Long matchedEmployeeId) { this.matchedEmployeeId = matchedEmployeeId; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getTrainingDirection() { return trainingDirection; }
    public void setTrainingDirection(String trainingDirection) { this.trainingDirection = trainingDirection; }

    public Long getMatchedDirectionId() { return matchedDirectionId; }
    public void setMatchedDirectionId(Long matchedDirectionId) { this.matchedDirectionId = matchedDirectionId; }

    public String getDirectionConfidence() { return directionConfidence; }
    public void setDirectionConfidence(String directionConfidence) { this.directionConfidence = directionConfidence; }

    public Long getSelectedDirectionId() { return selectedDirectionId; }
    public void setSelectedDirectionId(Long selectedDirectionId) { this.selectedDirectionId = selectedDirectionId; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    // Вспомогательный метод для отображения
    public String getDisplayPageInfo() {
        if (pageNumber != null) {
            return "Страница " + pageNumber;
        }
        return "Не указана";
    }
}