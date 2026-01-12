package com.training.controller;

import com.training.model.Department;
import com.training.model.TrainingType;
import com.training.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final TrainingTypeService trainingTypeService;

    public DepartmentController(DepartmentService departmentService,
                                EmployeeService employeeService, CertificateService certificateService, TrainingTypeService trainingTypeService, StorageService storageService) {
        this.departmentService = departmentService;
        this.employeeService = employeeService;
        this.trainingTypeService = trainingTypeService;
    }

    @GetMapping
    public String listDepartments(Model model) {
        List<Department> departments = departmentService.getAllDepartments();
        model.addAttribute("departments", departments);
        return "departments";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("department", new Department());
        return "department-form";
    }

    @PostMapping
    public String createDepartment(@Valid @ModelAttribute Department department,
                                   BindingResult result) {
        if (result.hasErrors()) {
            return "department-form";
        }

        try {
            departmentService.createDepartment(department);
            return "redirect:/departments";
        } catch (Exception e) {
            result.rejectValue("code", "error.department", e.getMessage());
            return "department-form";
        }
    }

    @GetMapping("/{id}")
    public String viewDepartment(@PathVariable Long id, Model model) {
        Department department = departmentService.getDepartmentById(id);
        List<TrainingType> trainingTypes = trainingTypeService.getAllTrainingTypes();

        model.addAttribute("department", department);
        model.addAttribute("employeeTable", employeeService.getEmployeeTableByDepartment(id));
        model.addAttribute("trainingTypes", trainingTypes); // Добавьте эту строку

        return "department";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Department department = departmentService.getDepartmentById(id);
        model.addAttribute("department", department);
        return "department-form";
    }

    @PostMapping("/{id}")
    public String updateDepartment(@PathVariable Long id,
                                   @Valid @ModelAttribute Department department,
                                   BindingResult result) {
        if (result.hasErrors()) {
            return "department-form";
        }

        try {
            departmentService.updateDepartment(id, department);
            return "redirect:/departments";
        } catch (Exception e) {
            result.rejectValue("code", "error.department", e.getMessage());
            return "department-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
        } catch (Exception e) {
            // Можно добавить сообщение об ошибке
        }
        return "redirect:/departments";
    }
}