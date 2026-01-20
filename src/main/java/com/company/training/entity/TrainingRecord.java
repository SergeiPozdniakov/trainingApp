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

    // Храним только имя файла, путь вычисляем
    @Column(name = "file_name")
    private String fileName;

    // Путь к файлу относительно директории протоколов
    @Column(name = "file_path")
    private String filePath;

    private Boolean applicable;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Transient
    private LocalDate nextExamDate;

    // Новое поле: номер страницы в исходном PDF
    @Column(name = "source_page_number")
    private Integer sourcePageNumber;

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

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Boolean getApplicable() { return applicable; }
    public void setApplicable(Boolean applicable) { this.applicable = applicable; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public Integer getSourcePageNumber() { return sourcePageNumber; }
    public void setSourcePageNumber(Integer sourcePageNumber) { this.sourcePageNumber = sourcePageNumber; }

    // Метод для получения полного пути к файлу
    public String getFullFilePath() {
        if (filePath == null) return null;
        return "uploads/protocols/" + filePath;
    }




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

    // ВЫЧИСЛЯЕМ nextExamDate динамически
    public LocalDate getNextExamDate() {
        // Если поле уже установлено (например, в тестах), возвращаем его
        if (nextExamDate != null) {
            return nextExamDate;
        }

        // Иначе вычисляем на основе examDate и validityMonths
        if (examDate != null && trainingDirection != null && trainingDirection.getValidityMonths() != null) {
            return examDate.plusMonths(trainingDirection.getValidityMonths());
        }

        return null;
    }

    // Сеттер только для тестов
    public void setNextExamDate(LocalDate nextExamDate) {
        this.nextExamDate = nextExamDate;
    }
}