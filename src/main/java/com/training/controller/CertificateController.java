package com.training.controller;

import com.training.model.Certificate;
import com.training.model.Employee;
import com.training.model.TrainingType;
import com.training.service.CertificateService;
import com.training.service.EmployeeService;
import com.training.service.TrainingTypeService;
import com.training.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final EmployeeService employeeService;
    private final TrainingTypeService trainingTypeService;
    private final StorageService storageService;

    public CertificateController(CertificateService certificateService,
                                 EmployeeService employeeService,
                                 TrainingTypeService trainingTypeService,
                                 StorageService storageService) {
        this.certificateService = certificateService;
        this.employeeService = employeeService;
        this.trainingTypeService = trainingTypeService;
        this.storageService = storageService;
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam Long employeeId,
                                 @RequestParam Long trainingTypeId,
                                 Model model) {
        Certificate certificate = new Certificate();

        Employee employee = employeeService.getEmployeeById(employeeId);
        TrainingType trainingType = trainingTypeService.getTrainingTypeById(trainingTypeId);

        certificate.setEmployee(employee);
        certificate.setTrainingType(trainingType);

        model.addAttribute("certificate", certificate);
        return "certificate-form";
    }

    @PostMapping
    public String createCertificate(@Valid @ModelAttribute Certificate certificate,
                                    BindingResult result,
                                    @RequestParam("file") MultipartFile file) {
        if (result.hasErrors()) {
            return "certificate-form";
        }

        try {
            certificateService.createCertificate(certificate, file);
            return "redirect:/departments/" + certificate.getEmployee().getDepartment().getId();
        } catch (Exception e) {
            result.rejectValue("examDate", "error.certificate", e.getMessage());
            return "certificate-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Certificate certificate = certificateService.getCertificateById(id);
        model.addAttribute("certificate", certificate);
        return "certificate-form";
    }

    @PostMapping("/{id}")
    public String updateCertificate(@PathVariable Long id,
                                    @Valid @ModelAttribute Certificate certificate,
                                    BindingResult result,
                                    @RequestParam(value = "file", required = false) MultipartFile file) {
        if (result.hasErrors()) {
            return "certificate-form";
        }

        try {
            certificateService.updateCertificate(id, certificate, file);
            return "redirect:/departments/" + certificate.getEmployee().getDepartment().getId();
        } catch (Exception e) {
            result.rejectValue("examDate", "error.certificate", e.getMessage());
            return "certificate-form";
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Certificate certificate = certificateService.getCertificateById(id);

        if (certificate.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = storageService.loadAsResource(certificate.getFilePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/delete")
    public String deleteCertificate(@PathVariable Long id) {
        Certificate certificate = certificateService.getCertificateById(id);
        Long departmentId = certificate.getEmployee().getDepartment().getId();

        certificateService.deleteCertificate(id);
        return "redirect:/departments/" + departmentId;
    }
}