package com.training.controller;

import com.training.model.Employee;
import com.training.model.Department;
import com.training.service.EmployeeService;
import com.training.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeeController(EmployeeService employeeService,
                              DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam Long departmentId, Model model) {
        Employee employee = new Employee();
        Department department = departmentService.getDepartmentById(departmentId);
        employee.setDepartment(department);

        model.addAttribute("employee", employee);
        return "employee-form";
    }

    @PostMapping
    public String createEmployee(@Valid @ModelAttribute Employee employee,
                                 BindingResult result) {
        if (result.hasErrors()) {
            return "employee-form";
        }

        employeeService.createEmployee(employee);
        return "redirect:/departments/" + employee.getDepartment().getId();
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        model.addAttribute("employee", employee);
        return "employee-form";
    }

    @PostMapping("/{id}")
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute Employee employee,
                                 BindingResult result) {
        if (result.hasErrors()) {
            return "employee-form";
        }

        Employee updated = employeeService.updateEmployee(id, employee);
        return "redirect:/departments/" + updated.getDepartment().getId();
    }

    @PostMapping("/{id}/delete")
    public String deleteEmployee(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        Long departmentId = employee.getDepartment().getId();

        employeeService.deleteEmployee(id);
        return "redirect:/departments/" + departmentId;
    }
}