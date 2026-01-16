package com.company.training.controller;

import com.company.training.entity.Department;
import com.company.training.entity.TrainingDirection;
import com.company.training.service.DepartmentService;
import com.company.training.service.TrainingService;
import com.company.training.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private TrainingService trainingService;

    // Пользователи
    @GetMapping("/users")
    public String userManagement(Model model) {
        model.addAttribute("pendingUsers", userService.getPendingUsers());
        model.addAttribute("activeUsers", userService.getAllActiveUsers());
        return "admin/users";
    }

    @PostMapping("/users/approve/{id}")
    public String approveUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.approveUser(id);
        redirectAttributes.addFlashAttribute("success", "Пользователь утвержден");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "Пользователь удален");
        return "redirect:/admin/users";
    }

    // Отделы
    @GetMapping("/departments")
    public String departmentManagement(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("department", new Department());
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String createDepartment(@Valid @ModelAttribute Department department,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации");
            return "redirect:/admin/departments";
        }

        if (departmentService.departmentExists(department.getName())) {
            redirectAttributes.addFlashAttribute("error", "Отдел с таким названием уже существует");
            return "redirect:/admin/departments";
        }

        departmentService.createDepartment(department);
        redirectAttributes.addFlashAttribute("success", "Отдел создан");
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        departmentService.deleteDepartment(id);
        redirectAttributes.addFlashAttribute("success", "Отдел удален");
        return "redirect:/admin/departments";
    }

    // Направления обучения
    @GetMapping("/training-directions")
    public String trainingDirections(Model model) {
        model.addAttribute("directions", trainingService.getAllTrainingDirections());
        model.addAttribute("direction", new TrainingDirection());
        return "admin/training-directions";
    }

    @PostMapping("/training-directions")
    public String createTrainingDirection(@Valid @ModelAttribute TrainingDirection direction,
                                          BindingResult result,
                                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации");
            return "redirect:/admin/training-directions";
        }

        // Установка стоимости по умолчанию, если не указана
        if (direction.getCost() == null) {
            direction.setCost(BigDecimal.ZERO);
        }

        trainingService.createTrainingDirection(direction);
        redirectAttributes.addFlashAttribute("success", "Направление создано");
        return "redirect:/admin/training-directions";
    }

    @PostMapping("/training-directions/{id}/delete")
    public String deleteTrainingDirection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        trainingService.deleteTrainingDirection(id);
        redirectAttributes.addFlashAttribute("success", "Направление удалено");
        return "redirect:/admin/training-directions";
    }
}