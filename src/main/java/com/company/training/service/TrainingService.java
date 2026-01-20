package com.company.training.service;

import com.company.training.entity.Employee;
import com.company.training.entity.TrainingDirection;
import com.company.training.entity.TrainingRecord;
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
            logger.info("Директория для хранения протоколов создана: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            logger.error("Не удалось создать директорию для хранения протоколов", ex);
            throw new RuntimeException("Не удалось создать директорию для хранения протоколов", ex);
        }
    }

    // ==================== НАПРАВЛЕНИЯ ОБУЧЕНИЯ ====================

    public List<TrainingDirection> getAllTrainingDirections() {
        return trainingDirectionRepository.findAllByOrderByNameAsc();
    }

    public TrainingDirection getTrainingDirectionById(Long id) {
        return trainingDirectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));
    }

    public TrainingDirection createTrainingDirection(TrainingDirection direction) {
        // Проверяем уникальность названия
        if (trainingDirectionRepository.existsByName(direction.getName())) {
            throw new RuntimeException("Направление с таким названием уже существует");
        }
        return trainingDirectionRepository.save(direction);
    }

    public TrainingDirection updateTrainingDirection(Long id, TrainingDirection direction) {
        TrainingDirection existing = trainingDirectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));

        // Проверяем, не используется ли новое название другим направлением
        if (!existing.getName().equals(direction.getName()) &&
                trainingDirectionRepository.existsByName(direction.getName())) {
            throw new RuntimeException("Направление с таким названием уже существует");
        }

        existing.setName(direction.getName());
        existing.setValidityMonths(direction.getValidityMonths());
        existing.setDescription(direction.getDescription());
        existing.setCost(direction.getCost());

        return trainingDirectionRepository.save(existing);
    }

    public void deleteTrainingDirection(Long id) {
        // Проверяем, используется ли направление в записях об обучении
        TrainingDirection direction = trainingDirectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Направление не найдено"));

        List<TrainingRecord> records = trainingRecordRepository.findByTrainingDirection(direction);
        if (!records.isEmpty()) {
            throw new RuntimeException("Нельзя удалить направление, так как оно используется в " +
                    records.size() + " записях об обучении");
        }

        trainingDirectionRepository.deleteById(id);
    }

    // ==================== ЗАПИСИ ОБ ОБУЧЕНИИ ====================

    public TrainingRecord getTrainingRecord(Long employeeId, Long directionId) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        TrainingDirection direction = getTrainingDirectionById(directionId);

        return trainingRecordRepository.findByEmployeeAndTrainingDirection(employee, direction)
                .orElse(null);
    }

    public TrainingRecord getTrainingRecordById(Long recordId) {
        return trainingRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Запись об обучении не найдена"));
    }

    public TrainingRecord saveTrainingRecord(TrainingRecord record, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String fileName = storeFile(file);
            record.setFileName(file.getOriginalFilename());
            record.setFilePath(fileName);
            logger.info("Файл протокола сохранен: {}", fileName);
        }

        return trainingRecordRepository.save(record);
    }

    public TrainingRecord saveTrainingRecordWithoutFile(TrainingRecord record) {
        logger.info("Сохранение записи об обучении без файла: ID={}, Сотрудник={}, Направление={}",
                record.getId(),
                record.getEmployee().getFullName(),
                record.getTrainingDirection().getName());
        return trainingRecordRepository.save(record);
    }

    public TrainingRecord createTrainingRecord(TrainingRecord record, MultipartFile file) throws IOException {
        // Проверяем, не существует ли уже запись для этого сотрудника и направления
        Optional<TrainingRecord> existing = trainingRecordRepository.findByEmployeeAndTrainingDirection(
                record.getEmployee(), record.getTrainingDirection());

        if (existing.isPresent()) {
            throw new RuntimeException("Запись об обучении для сотрудника " +
                    record.getEmployee().getFullName() +
                    " по направлению " +
                    record.getTrainingDirection().getName() +
                    " уже существует");
        }

        if (file != null && !file.isEmpty()) {
            String fileName = storeFile(file);
            record.setFileName(file.getOriginalFilename());
            record.setFilePath(fileName);
            logger.info("Файл протокола сохранен: {}", fileName);
        }

        return trainingRecordRepository.save(record);
    }

    public TrainingRecord updateTrainingRecord(Long recordId, TrainingRecord record, MultipartFile file) throws IOException {
        TrainingRecord existing = getTrainingRecordById(recordId);

        // Обновляем поля
        existing.setExamDate(record.getExamDate());
        existing.setProtocolNumber(record.getProtocolNumber());
        existing.setApplicable(record.getApplicable());
        existing.setSourcePageNumber(record.getSourcePageNumber());

        // Если загружен новый файл
        if (file != null && !file.isEmpty()) {
            // Удаляем старый файл, если он существует
            if (existing.getFilePath() != null) {
                deleteFile(existing.getFilePath());
            }

            String fileName = storeFile(file);
            existing.setFileName(file.getOriginalFilename());
            existing.setFilePath(fileName);
            logger.info("Файл протокола обновлен: {}", fileName);
        }

        return trainingRecordRepository.save(existing);
    }

    public void deleteTrainingRecord(Long recordId) throws IOException {
        TrainingRecord record = getTrainingRecordById(recordId);

        // Удаляем связанный файл, если он существует
        if (record.getFilePath() != null) {
            deleteFile(record.getFilePath());
        }

        trainingRecordRepository.deleteById(recordId);
        logger.info("Запись об обучении удалена: ID={}", recordId);
    }

    public List<TrainingRecord> getTrainingRecordsByDepartment(Long departmentId) {
        return trainingRecordRepository.findByEmployeeDepartmentId(departmentId);
    }

    public List<TrainingRecord> getTrainingRecordsByEmployee(Long employeeId) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        return trainingRecordRepository.findByEmployee(employee);
    }

    public List<TrainingRecord> getTrainingRecordsByDirection(Long directionId) {
        TrainingDirection direction = getTrainingDirectionById(directionId);
        return trainingRecordRepository.findByTrainingDirection(direction);
    }

    public List<TrainingRecord> getAllTrainingRecords() {
        return trainingRecordRepository.findAll();
    }

    // ==================== РАБОТА С ФАЙЛАМИ ====================

    private String storeFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // Генерируем уникальное имя файла
        String fileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(fileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Файл сохранен: {} -> {}", originalFileName, fileName);
        return fileName;
    }

    /**
     * Загружает файл протокола вручную (ручная загрузка)
     */
    public TrainingRecord uploadProtocolFile(TrainingRecord record, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        // Генерируем уникальное имя файла
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String fileName = String.format("protocol_manual_%d_%d_%s%s",
                record.getEmployee().getId(),
                record.getTrainingDirection().getId(),
                UUID.randomUUID().toString().substring(0, 8),
                fileExtension);

        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        record.setFileName(originalFileName);
        record.setFilePath(fileName);
        logger.info("Файл протокола сохранен: {} ({} байт)", fileName, file.getSize());

        return trainingRecordRepository.save(record);
    }

    /**
     * Сохраняет файл с указанным именем (используется при автоматической обработке PDF)
     */
    public void saveProtocolFile(byte[] fileContent, String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Имя файла не может быть пустым");
        }

        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        Files.write(targetLocation, fileContent);

        logger.info("Файл протокола сохранен: {} ({} байт)", fileName, fileContent.length);
    }

    /**
     * Получает файл протокола как byte array
     */
    public byte[] loadProtocolFile(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Имя файла не может быть пустым");
        }

        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

        // Проверяем безопасность пути
        if (!filePath.startsWith(this.fileStorageLocation)) {
            throw new SecurityException("Попытка доступа к файлу вне разрешенной директории");
        }

        if (!Files.exists(filePath)) {
            throw new IOException("Файл не найден: " + fileName);
        }

        return Files.readAllBytes(filePath);
    }

    /**
     * Удаляет файл протокола
     */
    public void deleteFile(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

        // Проверяем безопасность пути
        if (!filePath.startsWith(this.fileStorageLocation)) {
            throw new SecurityException("Попытка удаления файла вне разрешенной директории");
        }

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            logger.info("Файл протокола удален: {}", fileName);
        } else {
            logger.warn("Файл для удаления не найден: {}", fileName);
        }
    }

    /**
     * Получает информацию о файле протокола
     */
    public FileInfo getProtocolFileInfo(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

        if (!Files.exists(filePath)) {
            return null;
        }

        try {
            FileInfo info = new FileInfo();
            info.setFileName(fileName);
            info.setFilePath(filePath.toString());
            info.setFileSize(Files.size(filePath));
            info.setLastModified(Files.getLastModifiedTime(filePath).toMillis());

            // Определяем тип файла по расширению
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".pdf")) {
                info.setFileType("application/pdf");
                info.setFileExtension(".pdf");
            } else if (lowerFileName.endsWith(".doc")) {
                info.setFileType("application/msword");
                info.setFileExtension(".doc");
            } else if (lowerFileName.endsWith(".docx")) {
                info.setFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                info.setFileExtension(".docx");
            } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
                info.setFileType("image/jpeg");
                info.setFileExtension(".jpg");
            } else if (lowerFileName.endsWith(".png")) {
                info.setFileType("image/png");
                info.setFileExtension(".png");
            } else {
                info.setFileType("application/octet-stream");
                int dotIndex = fileName.lastIndexOf(".");
                info.setFileExtension(dotIndex > 0 ? fileName.substring(dotIndex) : "");
            }

            return info;
        } catch (IOException e) {
            logger.error("Ошибка при получении информации о файле {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * Проверяет существование файла протокола
     */
    public boolean protocolFileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        return Files.exists(filePath) && Files.isReadable(filePath);
    }

    /**
     * Получает размер файла протокола
     */
    public long getProtocolFileSize(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            return 0;
        }

        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        if (Files.exists(filePath)) {
            return Files.size(filePath);
        }
        return 0;
    }

    /**
     * Получает MIME-тип файла протокола
     */
    public String getProtocolFileMimeType(String fileName) {
        FileInfo info = getProtocolFileInfo(fileName);
        return info != null ? info.getFileType() : "application/octet-stream";
    }

    // ==================== СТАТИСТИКА И ФИЛЬТРАЦИЯ ====================

    public List<TrainingRecord> getExpiringRecords() {
        List<TrainingRecord> allRecords = trainingRecordRepository.findAll();
        return allRecords.stream()
                .filter(record -> record.isExpiringSoon() &&
                        (record.getApplicable() == null || record.getApplicable()))
                .toList();
    }

    public List<TrainingRecord> getExpiredRecords() {
        List<TrainingRecord> allRecords = trainingRecordRepository.findAll();
        return allRecords.stream()
                .filter(record -> record.isExpired() &&
                        (record.getApplicable() == null || record.getApplicable()))
                .toList();
    }

    public List<TrainingRecord> getValidRecords() {
        List<TrainingRecord> allRecords = trainingRecordRepository.findAll();
        return allRecords.stream()
                .filter(record -> !record.isExpired() &&
                        (record.getApplicable() == null || record.getApplicable()))
                .toList();
    }

    public long getTotalTrainingRecordsCount() {
        return trainingRecordRepository.count();
    }

    public long getExpiringRecordsCount() {
        return getExpiringRecords().size();
    }

    public long getExpiredRecordsCount() {
        return getExpiredRecords().size();
    }

    public long getValidRecordsCount() {
        return getValidRecords().size();
    }

    /**
     * Получает записи, у которых истекает срок действия в указанном месяце
     */
    public List<TrainingRecord> getRecordsExpiringInMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<TrainingRecord> allRecords = trainingRecordRepository.findAll();
        return allRecords.stream()
                .filter(record -> {
                    LocalDate nextExamDate = record.getNextExamDate();
                    if (nextExamDate == null || record.getApplicable() == null || !record.getApplicable()) {
                        return false;
                    }
                    return !nextExamDate.isBefore(startDate) && !nextExamDate.isAfter(endDate);
                })
                .toList();
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ====================

    /**
     * Класс для хранения информации о файле
     */
    public static class FileInfo {
        private String fileName;
        private String filePath;
        private long fileSize;
        private long lastModified;
        private String fileType;
        private String fileExtension;

        // Геттеры и сеттеры
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }

        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

        public String getFormattedFileSize() {
            if (fileSize < 1024) {
                return fileSize + " B";
            } else if (fileSize < 1024 * 1024) {
                return String.format("%.1f KB", fileSize / 1024.0);
            } else {
                return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
            }
        }

        public String getFormattedLastModified() {
            return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    .format(new java.util.Date(lastModified));
        }

        public boolean isPdf() {
            return fileType != null && fileType.equals("application/pdf");
        }

        public boolean isImage() {
            return fileType != null && (fileType.startsWith("image/"));
        }

        public boolean isDocument() {
            return fileType != null && (fileType.contains("word") || fileType.contains("document"));
        }
    }

    /**
     * Класс для статистики по протоколам
     */
    public static class ProtocolStats {
        private long totalFiles;
        private long totalSize;
        private long pdfCount;
        private long imageCount;
        private long documentCount;
        private long otherCount;

        // Геттеры и сеттеры
        public long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }

        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

        public long getPdfCount() { return pdfCount; }
        public void setPdfCount(long pdfCount) { this.pdfCount = pdfCount; }

        public long getImageCount() { return imageCount; }
        public void setImageCount(long imageCount) { this.imageCount = imageCount; }

        public long getDocumentCount() { return documentCount; }
        public void setDocumentCount(long documentCount) { this.documentCount = documentCount; }

        public long getOtherCount() { return otherCount; }
        public void setOtherCount(long otherCount) { this.otherCount = otherCount; }

        public String getFormattedTotalSize() {
            if (totalSize < 1024) {
                return totalSize + " B";
            } else if (totalSize < 1024 * 1024) {
                return String.format("%.1f KB", totalSize / 1024.0);
            } else if (totalSize < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", totalSize / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", totalSize / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }

    /**
     * Получает статистику по файлам протоколов
     */
    public ProtocolStats getProtocolStats() throws IOException {
        ProtocolStats stats = new ProtocolStats();

        if (Files.exists(fileStorageLocation) && Files.isDirectory(fileStorageLocation)) {
            try (var files = Files.list(fileStorageLocation)) {
                List<Path> fileList = files.filter(Files::isRegularFile).toList();

                stats.setTotalFiles(fileList.size());

                for (Path file : fileList) {
                    long fileSize = Files.size(file);
                    stats.setTotalSize(stats.getTotalSize() + fileSize);

                    String fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".pdf")) {
                        stats.setPdfCount(stats.getPdfCount() + 1);
                    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                            fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                        stats.setImageCount(stats.getImageCount() + 1);
                    } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx") ||
                            fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                        stats.setDocumentCount(stats.getDocumentCount() + 1);
                    } else {
                        stats.setOtherCount(stats.getOtherCount() + 1);
                    }
                }
            }
        }

        return stats;
    }

    /**
     * Очищает неиспользуемые файлы протоколов
     */
    public int cleanupOrphanedProtocolFiles() throws IOException {
        int deletedCount = 0;

        if (Files.exists(fileStorageLocation) && Files.isDirectory(fileStorageLocation)) {
            // Получаем список файлов, на которые есть ссылки в базе данных
            List<TrainingRecord> allRecords = trainingRecordRepository.findAll();
            List<String> referencedFiles = allRecords.stream()
                    .map(TrainingRecord::getFilePath)
                    .filter(fileName -> fileName != null && !fileName.isEmpty())
                    .toList();

            // Проверяем все файлы в директории
            try (var files = Files.list(fileStorageLocation)) {
                List<Path> fileList = files.filter(Files::isRegularFile).toList();

                for (Path file : fileList) {
                    String fileName = file.getFileName().toString();

                    // Если файл не упоминается в базе данных
                    if (!referencedFiles.contains(fileName)) {
                        // Проверяем возраст файла (например, удаляем файлы старше 30 дней)
                        long fileAgeDays = Files.getLastModifiedTime(file).toMillis();
                        long currentTime = System.currentTimeMillis();
                        long ageInDays = (currentTime - fileAgeDays) / (1000 * 60 * 60 * 24);

                        if (ageInDays > 30) {
                            Files.delete(file);
                            deletedCount++;
                            logger.info("Удален неиспользуемый файл протокола: {} (возраст {} дней)",
                                    fileName, ageInDays);
                        }
                    }
                }
            }
        }

        return deletedCount;
    }
}