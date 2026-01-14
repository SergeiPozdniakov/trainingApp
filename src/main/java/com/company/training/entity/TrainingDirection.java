package com.company.training.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "training_directions")
public class TrainingDirection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название направления обязательно")
    @Column(nullable = false, unique = true)
    private String name;

    @Min(value = 1, message = "Периодичность должна быть не менее 1 месяца")
    @Column(name = "validity_months", nullable = false)
    private Integer validityMonths;

    private String description;

    // Конструкторы
    public TrainingDirection() {}

    public TrainingDirection(String name, Integer validityMonths, String description) {
        this.name = name;
        this.validityMonths = validityMonths;
        this.description = description;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getValidityMonths() { return validityMonths; }
    public void setValidityMonths(Integer validityMonths) { this.validityMonths = validityMonths; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}