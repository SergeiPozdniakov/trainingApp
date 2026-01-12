package com.training.service;

import com.training.model.Department;
import com.training.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public Department createDepartment(Department department) {
        if (departmentRepository.existsByCode(department.getCode())) {
            throw new RuntimeException("Подразделение с таким кодом уже существует");
        }
        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Подразделение не найдено"));

        if (!department.getCode().equals(departmentDetails.getCode()) &&
                departmentRepository.existsByCode(departmentDetails.getCode())) {
            throw new RuntimeException("Подразделение с таким кодом уже существует");
        }

        department.setName(departmentDetails.getName());
        department.setCode(departmentDetails.getCode());

        return departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Подразделение не найдено"));

        // Проверяем, есть ли сотрудники в подразделении
        if (!department.getEmployees().isEmpty()) {
            throw new RuntimeException("Нельзя удалить подразделение с сотрудниками");
        }

        departmentRepository.delete(department);
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Подразделение не найдено"));
    }

    public Department getDepartmentByCode(String code) {
        return departmentRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Подразделение не найдено"));
    }
}