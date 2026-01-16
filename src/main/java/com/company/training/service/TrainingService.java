package com.company.training.service;

import com.company.training.entity.*;
import com.company.training.repository.TrainingDirectionRepository;
import com.company.training.repository.TrainingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TrainingService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);
    private final Path fileStorageLocation;

    @Autowired
    private TrainingDirectionRepository trainingDirectionRepository;

    @Autowired
    private TrainingRecordRepository trainingRecordRepository;

    @Autowired
    private EmployeeService employeeService;

    public TrainingService() {
        this.fileStorageLocation = Paths.get("uploads/protocols").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("Директория для хранения файлов создана: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            logger.error("Не удалось создать директорию для хранения файлов", ex);
            throw new RuntimeException("Не удалось создать директорию для хранения файлов", ex);
        }
    }

    public List<TrainingDirection> getAllTrainingDirections() {
        return trainingDirectionRepository.findAllByOrderByNameAsc();
    }

    public TrainingDirection getTrainingDirectionById(Long id) {
        return trainingDirectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));
    }

    public TrainingDirection createTrainingDirection(TrainingDirection direction) {
        return trainingDirectionRepository.save(direction);
    }

    public TrainingDirection updateTrainingDirection(Long id, TrainingDirection direction) {
        TrainingDirection existing = trainingDirectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));

        existing.setName(direction.getName());
        existing.setValidityMonths(direction.getValidityMonths());
        existing.setDescription(direction.getDescription());

        return trainingDirectionRepository.save(existing);
    }

    public void deleteTrainingDirection(Long id) {
        trainingDirectionRepository.deleteById(id);
    }

    public TrainingRecord getTrainingRecord(Long employeeId, Long directionId) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        TrainingDirection direction = getTrainingDirectionById(directionId);

        return trainingRecordRepository.findByEmployeeAndTrainingDirection(employee, direction)
                .orElse(null);
    }

    public TrainingRecord saveTrainingRecord(TrainingRecord record, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String fileName = storeFile(file);
            record.setFileName(file.getOriginalFilename());
            record.setFilePath(fileName);
            logger.info("Файл сохранен: {}", fileName);
        }

        return trainingRecordRepository.save(record);
    }

    public TrainingRecord saveTrainingRecordWithoutFile(TrainingRecord record) {
        logger.info("Сохранение записи без файла: ID={}", record.getId());
        return trainingRecordRepository.save(record);
    }

    public List<TrainingRecord> getTrainingRecordsByDepartment(Long departmentId) {
        return trainingRecordRepository.findByEmployeeDepartmentId(departmentId);
    }

    private String storeFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(fileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    public byte[] loadFile(String fileName) throws IOException {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        return Files.readAllBytes(filePath);
    }

    public void deleteFile(String fileName) throws IOException {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        Files.deleteIfExists(filePath);
    }

    public List<TrainingRecord> getExpiringRecords() {
        List<TrainingRecord> allRecords = trainingRecordRepository.findAll();
        return allRecords.stream()
                .filter(TrainingRecord::isExpiringSoon)
                .toList();
    }

    public long getTotalTrainingRecordsCount() {
        return trainingRecordRepository.count();
    }

    public List<TrainingRecord> getAllTrainingRecords() {
        return trainingRecordRepository.findAll();
    }
}