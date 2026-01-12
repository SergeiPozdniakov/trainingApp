package com.training.dto;

public class DashboardStatsDTO {
    private long totalEmployees;
    private long totalDepartments;
    private long upcomingExams;
    private long expiredCertificates;
    private long totalTrainingTypes;

    // Геттеры и сеттеры
    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }

    public long getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(long totalDepartments) { this.totalDepartments = totalDepartments; }

    public long getUpcomingExams() { return upcomingExams; }
    public void setUpcomingExams(long upcomingExams) { this.upcomingExams = upcomingExams; }

    public long getExpiredCertificates() { return expiredCertificates; }
    public void setExpiredCertificates(long expiredCertificates) { this.expiredCertificates = expiredCertificates; }

    public long getTotalTrainingTypes() { return totalTrainingTypes; }
    public void setTotalTrainingTypes(long totalTrainingTypes) { this.totalTrainingTypes = totalTrainingTypes; }
}