package com.company.training.controller;

import com.company.training.service.DepartmentService;
import com.company.training.service.TrainingService;
import com.company.training.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard");
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("expiringRecords", trainingService.getExpiringRecords());

        // Добавляем статистику сотрудников
        long totalEmployees = employeeService.getAllEmployeesCount();
        model.addAttribute("totalEmployees", totalEmployees);

        // Добавляем статистику записей обучения
        long totalRecords = trainingService.getTotalTrainingRecordsCount();
        model.addAttribute("totalRecords", totalRecords);

        return "dashboard";
    }
}