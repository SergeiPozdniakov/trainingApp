package com.training.controller;

import com.training.dto.DashboardStatsDTO;
import com.training.service.DashboardService;
import com.training.service.CertificateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final CertificateService certificateService;

    public DashboardController(DashboardService dashboardService,
                               CertificateService certificateService) {
        this.dashboardService = dashboardService;
        this.certificateService = certificateService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);

        // Ближайшие события (30 дней)
        model.addAttribute("upcomingCertificates",
                certificateService.getUpcomingCertificates(30));

        model.addAttribute("expiredCertificates",
                certificateService.getExpiredCertificates());

        model.addAttribute("currentDate", LocalDate.now());

        return "dashboard";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}