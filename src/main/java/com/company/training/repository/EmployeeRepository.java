package com.company.training.repository;

import com.company.training.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.OptionalDouble;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartmentIdOrderByFullNameAsc(Long departmentId);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    long count();

}