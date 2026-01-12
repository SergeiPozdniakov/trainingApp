package com.training.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_types")
public class TrainingType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название направления обязательно")
    @Size(min = 2, max = 100)
    @Column(unique = true, nullable = false)
    private String name;

    @Min(value = 1, message = "Периодичность должна быть положительным числом")
    @Column(name = "validity_period_months", nullable = false)
    private Integer validityPeriodMonths;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "trainingType", cascade = CascadeType.ALL)
    private List<Certificate> certificates = new ArrayList<>();

    // Конструкторы
    public TrainingType() {}

    public TrainingType(String name, Integer validityPeriodMonths) {
        this.name = name;
        this.validityPeriodMonths = validityPeriodMonths;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getValidityPeriodMonths() { return validityPeriodMonths; }
    public void setValidityPeriodMonths(Integer validityPeriodMonths) {
        this.validityPeriodMonths = validityPeriodMonths;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Certificate> getCertificates() { return certificates; }
    public void setCertificates(List<Certificate> certificates) { this.certificates = certificates; }
}