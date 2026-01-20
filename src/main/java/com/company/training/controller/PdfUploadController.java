package com.company.training.controller;

import com.company.training.dto.ParsedProtocolRecord;
import com.company.training.dto.ProtocolValidationResult;
import com.company.training.entity.PdfDocument;
import com.company.training.service.PdfProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/pdf")
public class PdfUploadController {

    private static final Logger logger = LoggerFactory.getLogger(PdfUploadController.class);

    @Autowired
    private PdfProcessingService pdfProcessingService;

    /**
     * Страница загрузки PDF
     */
    @GetMapping("/upload")
    public String showUploadPage(Model model) {
        model.addAttribute("title", "Загрузка протоколов");
        return "admin/pdf-upload";
    }

    /**
     * Страница списка загруженных PDF
     */
    @GetMapping("/list")
    public String showPdfList(Model model) {
        List<PdfDocument> documents = pdfProcessingService.getAllPdfDocuments();
        model.addAttribute("documents", documents);
        model.addAttribute("title", "Загруженные протоколы");
        return "admin/pdf-list";
    }

    /**
     * Загрузка PDF файла
     */
    @PostMapping("/upload")
    public String uploadPdf(@RequestParam("pdfFile") MultipartFile file,
                            @RequestParam("pdfType") String pdfType,
                            RedirectAttributes redirectAttributes) {
        try {
            PdfDocument.PdfType type = PdfDocument.PdfType.valueOf(pdfType);
            PdfDocument document = pdfProcessingService.uploadPdf(file, type);

            redirectAttributes.addFlashAttribute("success",
                    "Файл успешно загружен. ID: " + document.getId());
            return "redirect:/admin/pdf/process/" + document.getId();

        } catch (Exception e) {
            logger.error("Ошибка загрузки PDF", e);
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка загрузки файла: " + e.getMessage());
            return "redirect:/admin/pdf/upload";
        }
    }

    /**
     * Страница обработки OCR
     */
    @GetMapping("/process/{id}")
    public String showProcessPage(@PathVariable Long id, Model model) {
        PdfDocument document = pdfProcessingService.getPdfDocumentById(id);

        // Если статус PENDING, запускаем OCR обработку
        if (document.getStatus() == PdfDocument.ProcessingStatus.PENDING) {
            try {
                document = pdfProcessingService.processOcr(id);
            } catch (Exception e) {
                model.addAttribute("error", "Ошибка при запуске OCR: " + e.getMessage());
            }
        }

        // Если OCR завершен, перенаправляем на страницу валидации
        if (document.getStatus() == PdfDocument.ProcessingStatus.NEEDS_REVIEW) {
            return "redirect:/admin/pdf/parse/" + id;
        }

        model.addAttribute("document", document);
        model.addAttribute("title", "Обработка протокола");
        return "admin/pdf-process";
    }

    /**
     * Запуск OCR обработки
     */
    @PostMapping("/process/{id}")
    public String processOcr(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            PdfDocument document = pdfProcessingService.processOcr(id);
            redirectAttributes.addFlashAttribute("success",
                    "OCR обработка завершена. Распознано " +
                            (document.getOcrText() != null ? document.getOcrText().length() : 0) + " символов.");
            return "redirect:/admin/pdf/parse/" + id;
        } catch (Exception e) {
            logger.error("Ошибка OCR обработки", e);
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка OCR: " + e.getMessage());
            return "redirect:/admin/pdf/process/" + id;
        }
    }

    /**
     * Сохранение подтвержденных записей
     */
    @PostMapping("/save/{id}")
    public String saveRecords(@PathVariable Long id,
                              @ModelAttribute("records") List<ParsedProtocolRecord> records,
                              RedirectAttributes redirectAttributes) {
        try {
            // Проверяем, что для всех записей выбрано направление
            List<String> missingDirections = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                ParsedProtocolRecord record = records.get(i);
                if (record != null && record.getSelectedDirectionId() == null) {
                    missingDirections.add("Запись #" + (i + 1) + ": " + record.getFullName());
                }
            }

            if (!missingDirections.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Для следующих записей не выбрано направление: " +
                                String.join(", ", missingDirections.subList(0, Math.min(3, missingDirections.size()))));
                return "redirect:/admin/pdf/parse/" + id;
            }

            ProtocolValidationResult result = pdfProcessingService.saveConfirmedRecords(records, id);

            redirectAttributes.addFlashAttribute("success",
                    String.format("Сохранено %d из %d записей.",
                            result.getValidRecords(), result.getTotalRecords()));

            // Если есть успешные операции, показываем
            if (result.getSuccesses() != null && !result.getSuccesses().isEmpty()) {
                int successCount = Math.min(result.getSuccesses().size(), 3);
                redirectAttributes.addFlashAttribute("info",
                        "Успешно: " + String.join("; ", result.getSuccesses().subList(0, successCount)));
            }

            // Если есть ошибки, показываем
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                int errorCount = Math.min(result.getErrors().size(), 3);
                redirectAttributes.addFlashAttribute("warning",
                        "Были ошибки: " + String.join("; ", result.getErrors().subList(0, errorCount)));
            }

            return "redirect:/admin/pdf/list";

        } catch (Exception e) {
            logger.error("Ошибка сохранения записей", e);
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка сохранения: " + e.getMessage());
            return "redirect:/admin/pdf/parse/" + id;
        }
    }

    /**
     * Просмотр OCR текста
     */
    @GetMapping("/view-text/{id}")
    public String viewOcrText(@PathVariable Long id, Model model) {
        PdfDocument document = pdfProcessingService.getPdfDocumentById(id);

        // Разбиваем текст на страницы
        List<String> pages = new ArrayList<>();
        if (document.getOcrText() != null) {
            String[] pageParts = document.getOcrText().split("=== Страница \\d+ ===");
            for (int i = 0; i < pageParts.length; i++) {
                String page = pageParts[i].trim();
                if (!page.isEmpty()) {
                    pages.add("=== Страница " + (i + 1) + " ===\n" + page);
                }
            }
        }

        model.addAttribute("document", document);
        model.addAttribute("pages", pages);
        model.addAttribute("title", "OCR текст");
        return "admin/pdf-text-view";
    }

    /**
     * Страница парсинга и валидации данных
     */
    @GetMapping("/parse/{id}")
    public String showParsePage(@PathVariable Long id, Model model) {
        PdfDocument document = pdfProcessingService.getPdfDocumentById(id);
        if (document.getOcrText() == null) {
            return "redirect:/admin/pdf/process/" + id;
        }

        // Парсим записи
        List<ParsedProtocolRecord> records;
        if (document.getType() == PdfDocument.PdfType.OCCUPATIONAL_SAFETY) {
            records = pdfProcessingService.parseOccupationalSafetyProtocol(
                    document.getOcrText(), document.getId());
        } else {
            // Для промышленной безопасности - другая логика
            records = List.of();
        }

        model.addAttribute("document", document);
        model.addAttribute("records", records);
        model.addAttribute("directions", pdfProcessingService.getAllTrainingDirections());
        model.addAttribute("title", "Валидация данных протокола");

        return "admin/pdf-validate";
    }
}