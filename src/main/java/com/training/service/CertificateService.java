package com.training.service;

import com.training.model.Certificate;
import com.training.model.Employee;
import com.training.model.TrainingType;
import com.training.repository.CertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final StorageService storageService;

    public CertificateService(CertificateRepository certificateRepository,
                              StorageService storageService) {
        this.certificateRepository = certificateRepository;
        this.storageService = storageService;
    }

    @Transactional
    public Certificate createCertificate(Certificate certificate, MultipartFile file) {
        // Проверяем, существует ли уже сертификат для этого сотрудника и направления
        List<Certificate> existing = certificateRepository.findLatestCertificate(
                certificate.getEmployee().getId(),
                certificate.getTrainingType().getId()
        );

        if (!existing.isEmpty()) {
            Certificate latest = existing.get(0);
            if (latest.isApplicable() && latest.getNextExamDueDate().isAfter(LocalDate.now())) {
                throw new RuntimeException("У сотрудника уже есть действующий сертификат по этому направлению");
            }
        }

        if (file != null && !file.isEmpty()) {
            String filePath = storageService.store(file);
            certificate.setFilePath(filePath);
        }

        return certificateRepository.save(certificate);
    }

    @Transactional
    public Certificate updateCertificate(Long id, Certificate certificateDetails, MultipartFile file) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сертификат не найден"));

        if (file != null && !file.isEmpty()) {
            // Удаляем старый файл
            if (certificate.getFilePath() != null) {
                storageService.delete(certificate.getFilePath());
            }
            // Сохраняем новый файл
            String filePath = storageService.store(file);
            certificate.setFilePath(filePath);
        }

        certificate.setExamDate(certificateDetails.getExamDate());
        certificate.setProtocolNumber(certificateDetails.getProtocolNumber());
        certificate.setApplicable(certificateDetails.isApplicable());

        return certificateRepository.save(certificate);
    }

    @Transactional
    public void deleteCertificate(Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сертификат не найден"));

        // Удаляем файл
        if (certificate.getFilePath() != null) {
            storageService.delete(certificate.getFilePath());
        }

        certificateRepository.delete(certificate);
    }

    public List<Certificate> getUpcomingCertificates(int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);
        return certificateRepository.findUpcomingCertificates(today, endDate);
    }

    public List<Certificate> getExpiredCertificates() {
        return certificateRepository.findExpiredCertificates(LocalDate.now());
    }

    public Certificate getCertificateById(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сертификат не найден"));
    }

    public List<Certificate> getCertificatesByEmployee(Long employeeId) {
        Employee employee = new Employee();
        employee.setId(employeeId);
        return certificateRepository.findByEmployee(employee);
    }
}