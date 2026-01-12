package com.training.dto;

import java.util.Map;

public class EmployeeTableDTO {
    private Long id;
    private String fullName;
    private String position;
    private String email;
    private Long departmentId;
    private String departmentName;
    private Map<Long, CertificateCellDTO> certificatesByTrainingType;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Map<Long, CertificateCellDTO> getCertificatesByTrainingType() { return certificatesByTrainingType; }
    public void setCertificatesByTrainingType(Map<Long, CertificateCellDTO> certificatesByTrainingType) {
        this.certificatesByTrainingType = certificatesByTrainingType;
    }
}