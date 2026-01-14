package com.company.training.repository;

import com.company.training.entity.Employee;
import com.company.training.entity.TrainingDirection;
import com.company.training.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {
    List<TrainingRecord> findByEmployee(Employee employee);
    Optional<TrainingRecord> findByEmployeeAndTrainingDirection(Employee employee, TrainingDirection trainingDirection);
    List<TrainingRecord> findByEmployeeDepartmentId(Long departmentId);
    List<TrainingRecord> findAll();
    long count();
}