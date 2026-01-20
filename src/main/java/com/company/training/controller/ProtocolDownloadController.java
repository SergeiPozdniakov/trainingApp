package com.company.training.controller;

import com.company.training.entity.TrainingRecord;
import com.company.training.service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ProtocolDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolDownloadController.class);

    @Autowired
    private TrainingService trainingService;

    /**
     * Скачивание файла протокола по имени файла
     */
    @GetMapping("/download/protocol/{fileName:.+}")
    public ResponseEntity<Resource> downloadProtocol(@PathVariable String fileName) {
        try {
            // Загружаем файл как ресурс
            Path filePath = Paths.get("uploads/protocols").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";

                // Определяем Content-Type по расширению файла
                if (fileName.toLowerCase().endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.toLowerCase().endsWith(".doc")) {
                    contentType = "application/msword";
                } else if (fileName.toLowerCase().endsWith(".docx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Файл протокола не найден: " + fileName);
            }
        } catch (Exception e) {
            logger.error("Ошибка при скачивании файла протокола: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при скачивании файла протокола: " + e.getMessage(), e);
        }
    }

    /**
     * Просмотр PDF протокола в браузере
     */
    @GetMapping("/view/protocol/{fileName:.+}")
    public ResponseEntity<Resource> viewProtocol(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("uploads/protocols").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Для PDF отображаем в браузере
                if (fileName.toLowerCase().endsWith(".pdf")) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                            .body(resource);
                } else {
                    // Для других файлов - скачивание
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                            .body(resource);
                }
            } else {
                throw new RuntimeException("Файл протокола не найден: " + fileName);
            }
        } catch (Exception e) {
            logger.error("Ошибка при просмотре файла протокола: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при просмотре файла протокола: " + e.getMessage(), e);
        }
    }
}