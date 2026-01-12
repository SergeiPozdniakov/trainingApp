package com.training.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_type_id", nullable = false)
    private TrainingType trainingType;

    @NotNull(message = "Дата экзамена обязательна")
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @NotBlank(message = "Номер протокола обязателен")
    @Column(name = "protocol_number", nullable = false)
    private String protocolNumber;

    @Column(name = "file_path")
    private String filePath;

    @Column(nullable = false)
    private boolean applicable = true;

    @Column(name = "next_exam_due_date")
    private LocalDate nextExamDueDate;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        calculateNextExamDueDate();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateNextExamDueDate();
    }

    private void calculateNextExamDueDate() {
        if (examDate != null && trainingType != null && trainingType.getValidityPeriodMonths() != null) {
            this.nextExamDueDate = examDate.plusMonths(trainingType.getValidityPeriodMonths());
        }
    }

    // Конструкторы
    public Certificate() {}

    public Certificate(Employee employee, TrainingType trainingType, LocalDate examDate, String protocolNumber) {
        this.employee = employee;
        this.trainingType = trainingType;
        this.examDate = examDate;
        this.protocolNumber = protocolNumber;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public TrainingType getTrainingType() { return trainingType; }
    public void setTrainingType(TrainingType trainingType) { this.trainingType = trainingType; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getProtocolNumber() { return protocolNumber; }
    public void setProtocolNumber(String protocolNumber) { this.protocolNumber = protocolNumber; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isApplicable() { return applicable; }
    public void setApplicable(boolean applicable) { this.applicable = applicable; }

    public LocalDate getNextExamDueDate() { return nextExamDueDate; }
    public void setNextExamDueDate(LocalDate nextExamDueDate) { this.nextExamDueDate = nextExamDueDate; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}