package com.training.controller;

import com.training.model.TrainingType;
import com.training.service.TrainingTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/training-types")
public class TrainingTypeController {

    private final TrainingTypeService trainingTypeService;

    public TrainingTypeController(TrainingTypeService trainingTypeService) {
        this.trainingTypeService = trainingTypeService;
    }

    @GetMapping
    public String listTrainingTypes(Model model) {
        List<TrainingType> trainingTypes = trainingTypeService.getAllTrainingTypes();
        model.addAttribute("trainingTypes", trainingTypes);
        return "training-types";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("trainingType", new TrainingType());
        return "training-type-form";
    }

    @PostMapping
    public String createTrainingType(@Valid @ModelAttribute TrainingType trainingType,
                                     BindingResult result) {
        if (result.hasErrors()) {
            return "training-type-form";
        }

        try {
            trainingTypeService.createTrainingType(trainingType);
            return "redirect:/training-types";
        } catch (Exception e) {
            result.rejectValue("name", "error.trainingType", e.getMessage());
            return "training-type-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        TrainingType trainingType = trainingTypeService.getTrainingTypeById(id);
        model.addAttribute("trainingType", trainingType);
        return "training-type-form";
    }

    @PostMapping("/{id}")
    public String updateTrainingType(@PathVariable Long id,
                                     @Valid @ModelAttribute TrainingType trainingType,
                                     BindingResult result) {
        if (result.hasErrors()) {
            return "training-type-form";
        }

        try {
            trainingTypeService.updateTrainingType(id, trainingType);
            return "redirect:/training-types";
        } catch (Exception e) {
            result.rejectValue("name", "error.trainingType", e.getMessage());
            return "training-type-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteTrainingType(@PathVariable Long id) {
        try {
            trainingTypeService.deleteTrainingType(id);
        } catch (Exception e) {
            // Можно добавить сообщение об ошибке
        }
        return "redirect:/training-types";
    }
}