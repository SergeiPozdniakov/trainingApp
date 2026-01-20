package com.company.training.repository;

import com.company.training.entity.Employee;
import com.company.training.entity.TrainingDirection;
import com.company.training.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {
    List<TrainingRecord> findByEmployee(Employee employee);
    Optional<TrainingRecord> findByEmployeeAndTrainingDirection(Employee employee, TrainingDirection trainingDirection);
    List<TrainingRecord> findByEmployeeDepartmentId(Long departmentId);

    // Используем именованный параметр вместо entity
    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.trainingDirection = :direction")
    List<TrainingRecord> findByTrainingDirection(@Param("direction") TrainingDirection direction);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.employee.department.id = :departmentId AND tr.trainingDirection.id = :directionId")
    List<TrainingRecord> findByDepartmentAndDirection(@Param("departmentId") Long departmentId, @Param("directionId") Long directionId);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.applicable = true AND tr.examDate IS NOT NULL")
    List<TrainingRecord> findApplicableRecords();

    // Убираем запросы с nextExamDate, так как это вычисляемое поле
    // Вместо них будем использовать простые запросы, а фильтрацию делать на уровне сервиса
    List<TrainingRecord> findAll();
    long count();

    @Query("SELECT COUNT(tr) FROM TrainingRecord tr WHERE tr.applicable = true")
    long countApplicable();

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.employee.id = :employeeId ORDER BY tr.trainingDirection.name")
    List<TrainingRecord> findByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.trainingDirection.id = :directionId ORDER BY tr.employee.fullName")
    List<TrainingRecord> findByDirectionId(@Param("directionId") Long directionId);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.examDate BETWEEN :startDate AND :endDate")
    List<TrainingRecord> findByExamDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT tr.employee FROM TrainingRecord tr WHERE tr.trainingDirection.id = :directionId")
    List<Employee> findEmployeesByDirection(@Param("directionId") Long directionId);

    @Query("SELECT DISTINCT tr.trainingDirection FROM TrainingRecord tr WHERE tr.employee.id = :employeeId")
    List<TrainingDirection> findDirectionsByEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.filePath IS NOT NULL")
    List<TrainingRecord> findRecordsWithFiles();

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.filePath IS NULL")
    List<TrainingRecord> findRecordsWithoutFiles();

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.applicable IS NULL OR tr.applicable = false")
    List<TrainingRecord> findInapplicableRecords();

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.applicable = true AND (tr.examDate IS NULL OR tr.protocolNumber IS NULL)")
    List<TrainingRecord> findIncompleteRecords();

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.applicable = true AND tr.filePath IS NOT NULL AND tr.examDate IS NOT NULL ORDER BY tr.examDate DESC")
    List<TrainingRecord> findCompleteRecords();

    @Query("SELECT COUNT(tr) FROM TrainingRecord tr WHERE YEAR(tr.examDate) = :year AND MONTH(tr.examDate) = :month")
    long countByExamYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.employee.department.id = :departmentId AND tr.applicable = true AND tr.examDate IS NOT NULL")
    List<TrainingRecord> findApplicableByDepartment(@Param("departmentId") Long departmentId);
}