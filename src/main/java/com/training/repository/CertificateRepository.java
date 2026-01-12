package com.training.repository;

import com.training.model.Certificate;
import com.training.model.Employee;
import com.training.model.TrainingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByEmployeeAndTrainingType(Employee employee, TrainingType trainingType);
    List<Certificate> findByEmployee(Employee employee);
    List<Certificate> findByTrainingType(TrainingType trainingType);

    @Query("SELECT c FROM Certificate c WHERE c.nextExamDueDate BETWEEN :startDate AND :endDate")
    List<Certificate> findUpcomingCertificates(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Certificate c WHERE c.nextExamDueDate < :today AND c.applicable = true")
    List<Certificate> findExpiredCertificates(@Param("today") LocalDate today);

    @Query("SELECT c FROM Certificate c WHERE c.employee.id = :employeeId AND c.trainingType.id = :trainingTypeId ORDER BY c.examDate DESC")
    List<Certificate> findLatestCertificate(@Param("employeeId") Long employeeId,
                                            @Param("trainingTypeId") Long trainingTypeId);
}