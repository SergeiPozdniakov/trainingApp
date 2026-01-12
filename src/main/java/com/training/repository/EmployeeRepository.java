package com.training.repository;

import com.training.model.Employee;
import com.training.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartmentOrderByLastNameAscFirstNameAsc(Department department);
    List<Employee> findByDepartmentIdOrderByLastNameAscFirstNameAsc(Long departmentId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);
}