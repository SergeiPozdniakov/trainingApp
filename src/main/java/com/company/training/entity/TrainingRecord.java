package com.company.training.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "training_records")
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_direction_id", nullable = false)
    private TrainingDirection trainingDirection;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "protocol_number")
    private String protocolNumber;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    private Boolean applicable;

    @Column(name = "created_at")
    private LocalDate createdAt;

    // Конструкторы
    public TrainingRecord() {}

    public TrainingRecord(Employee employee, TrainingDirection trainingDirection) {
        this.employee = employee;
        this.trainingDirection = trainingDirection;
        this.createdAt = LocalDate.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }

    // Вспомогательные методы
    public LocalDate getNextExamDate() {
        if (examDate != null && trainingDirection != null && trainingDirection.getValidityMonths() != null) {
            return examDate.plusMonths(trainingDirection.getValidityMonths());
        }
        return null;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public TrainingDirection getTrainingDirection() { return trainingDirection; }
    public void setTrainingDirection(TrainingDirection trainingDirection) {
        this.trainingDirection = trainingDirection;
    }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getProtocolNumber() { return protocolNumber; }
    public void setProtocolNumber(String protocolNumber) { this.protocolNumber = protocolNumber; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Boolean getApplicable() { return applicable; }
    public void setApplicable(Boolean applicable) { this.applicable = applicable; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public boolean isExpiringSoon() {
        LocalDate nextExamDate = getNextExamDate();
        if (nextExamDate != null) {
            LocalDate threeMonthsBefore = nextExamDate.minusMonths(3);
            LocalDate today = LocalDate.now();
            return today.isAfter(threeMonthsBefore) && today.isBefore(nextExamDate);
        }
        return false;
    }

    public boolean isExpired() {
        LocalDate nextExamDate = getNextExamDate();
        return nextExamDate != null && LocalDate.now().isAfter(nextExamDate);
    }

    // Геттер для отображения
    public String getStatusColor() {
        if (applicable != null && !applicable) {
            return "secondary";
        } else if (isExpired()) {
            return "danger";
        } else if (isExpiringSoon()) {
            return "warning";
        } else if (applicable != null && applicable) {
            return "success";
        }
        return "light";
    }
}