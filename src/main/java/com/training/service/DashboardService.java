package com.training.service;

import com.training.dto.DashboardStatsDTO;
import com.training.repository.DepartmentRepository;
import com.training.repository.EmployeeRepository;
import com.training.repository.TrainingTypeRepository;
import com.training.repository.CertificateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DashboardService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final CertificateRepository certificateRepository;

    public DashboardService(DepartmentRepository departmentRepository,
                            EmployeeRepository employeeRepository,
                            TrainingTypeRepository trainingTypeRepository,
                            CertificateRepository certificateRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.trainingTypeRepository = trainingTypeRepository;
        this.certificateRepository = certificateRepository;
    }

    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        stats.setTotalDepartments(departmentRepository.count());
        stats.setTotalEmployees(employeeRepository.count());
        stats.setTotalTrainingTypes(trainingTypeRepository.count());

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);

        long upcomingExams = certificateRepository.findUpcomingCertificates(today, thirtyDaysLater).size();
        stats.setUpcomingExams(upcomingExams);

        long expiredCertificates = certificateRepository.findExpiredCertificates(today).size();
        stats.setExpiredCertificates(expiredCertificates);

        return stats;
    }
}