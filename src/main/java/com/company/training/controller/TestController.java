package com.company.training.controller;

import com.company.training.service.TrainingRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
public class TestController {

    @Autowired
    private TrainingRequestService trainingRequestService;

    @GetMapping("/api/test/email")
    public String testEmail() {
        try {
            // Принудительный запуск отправки заявки
            trainingRequestService.checkAndSendTrainingRequests();
            return "Тестовая отправка запущена. Проверьте логи и email.";
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }

    }

    @GetMapping("/test/pdf-processing")
    public String testPdfProcessing() {
        return "redirect:/admin/pdf/upload";
    }

    @GetMapping("/api/test/date")
    public String testDate() {
        LocalDate today = LocalDate.now();
        return String.format("""
            Текущая дата системы: %s
            Первый рабочий день этого месяца: %s
            Является ли сегодня первым рабочим днем: %s
            """,
                today,
                trainingRequestService.getFirstWorkingDayOfMonth(today),
                trainingRequestService.isFirstWorkingDayOfMonth()
        );
    }
}