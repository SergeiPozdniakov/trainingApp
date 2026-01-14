package com.company.training.service;

import com.company.training.entity.Employee;
import com.company.training.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentService departmentService;

    public List<Employee> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentIdOrderByFullNameAsc(departmentId);
    }

    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Сотрудник с ID " + id + " не найден");
        }
        employeeRepository.deleteById(id);
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
    }

    public boolean employeeExists(String email) {
        return employeeRepository.existsByEmailAndIdNot(email, 0L);
    }

    public long getAllEmployeesCount() {
        return employeeRepository.count();
    }

    public Employee updateEmployee(Employee employee) {
        // Проверяем существование сотрудника
        Employee existing = employeeRepository.findById(employee.getId())
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

        // Проверяем уникальность email (если изменился)
        if (!existing.getEmail().equals(employee.getEmail()) &&
                employeeRepository.existsByEmailAndIdNot(employee.getEmail(), employee.getId())) {
            throw new RuntimeException("Email уже используется другим сотрудником");
        }

        // Обновляем данные
        existing.setFullName(employee.getFullName());
        existing.setPosition(employee.getPosition());
        existing.setEmail(employee.getEmail());

        return employeeRepository.save(existing);
    }
}