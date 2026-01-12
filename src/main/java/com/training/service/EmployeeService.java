package com.training.service;

import com.training.dto.EmployeeTableDTO;
import com.training.dto.CertificateCellDTO;
import com.training.model.Employee;
import com.training.model.Department;
import com.training.model.Certificate;
import com.training.repository.EmployeeRepository;
import com.training.repository.CertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CertificateRepository certificateRepository;
    private final TrainingTypeService trainingTypeService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           CertificateRepository certificateRepository,
                           TrainingTypeService trainingTypeService) {
        this.employeeRepository = employeeRepository;
        this.certificateRepository = certificateRepository;
        this.trainingTypeService = trainingTypeService;
    }

    @Transactional
    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

        employee.setLastName(employeeDetails.getLastName());
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setMiddleName(employeeDetails.getMiddleName());
        employee.setPosition(employeeDetails.getPosition());
        employee.setEmail(employeeDetails.getEmail());
        employee.setDepartment(employeeDetails.getDepartment());

        return employeeRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

        // Удаляем все сертификаты сотрудника перед удалением самого сотрудника
        certificateRepository.deleteAll(employee.getCertificates());
        employeeRepository.delete(employee);
    }

    public List<Employee> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentIdOrderByLastNameAscFirstNameAsc(departmentId);
    }

    public List<EmployeeTableDTO> getEmployeeTableByDepartment(Long departmentId) {
        List<Employee> employees = getEmployeesByDepartment(departmentId);
        List<com.training.model.TrainingType> trainingTypes = trainingTypeService.getAllTrainingTypes();

        return employees.stream()
                .map(employee -> convertToEmployeeTableDTO(employee, trainingTypes))
                .collect(Collectors.toList());
    }

    private EmployeeTableDTO convertToEmployeeTableDTO(Employee employee,
                                                       List<com.training.model.TrainingType> trainingTypes) {
        EmployeeTableDTO dto = new EmployeeTableDTO();
        dto.setId(employee.getId());
        dto.setFullName(employee.getFullName());
        dto.setPosition(employee.getPosition());
        dto.setEmail(employee.getEmail());
        dto.setDepartmentId(employee.getDepartment().getId());
        dto.setDepartmentName(employee.getDepartment().getName());

        Map<Long, CertificateCellDTO> certificatesMap = new HashMap<>();

        for (com.training.model.TrainingType trainingType : trainingTypes) {
            List<Certificate> certificates = certificateRepository.findLatestCertificate(
                    employee.getId(), trainingType.getId());

            if (!certificates.isEmpty()) {
                Certificate latestCertificate = certificates.get(0);
                CertificateCellDTO cellDto = new CertificateCellDTO();
                cellDto.setCertificateId(latestCertificate.getId());
                cellDto.setExamDate(latestCertificate.getExamDate());
                cellDto.setProtocolNumber(latestCertificate.getProtocolNumber());
                cellDto.setFilePath(latestCertificate.getFilePath());
                cellDto.setApplicable(latestCertificate.isApplicable());
                cellDto.setNextExamDueDate(latestCertificate.getNextExamDueDate());
                cellDto.setStatusColor(calculateStatusColor(latestCertificate));

                certificatesMap.put(trainingType.getId(), cellDto);
            }
        }

        dto.setCertificatesByTrainingType(certificatesMap);
        return dto;
    }

    private String calculateStatusColor(Certificate certificate) {
        if (!certificate.isApplicable()) {
            return "gray";
        }

        LocalDate dueDate = certificate.getNextExamDueDate();
        LocalDate today = LocalDate.now();
        LocalDate warningDate = dueDate.minusMonths(3);

        if (warningDate.isBefore(today) && dueDate.isAfter(today)) {
            return "red"; // Менее 3 месяцев до истечения
        } else if (dueDate.isAfter(today)) {
            return "green"; // Действителен
        } else {
            return "orange"; // Просрочен
        }
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
    }
}