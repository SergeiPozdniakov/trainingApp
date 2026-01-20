package com.company.training.service;

import com.company.training.dto.ParsedProtocolRecord;
import com.company.training.dto.ProtocolValidationResult;
import com.company.training.entity.*;
import com.company.training.repository.EmployeeRepository;
import com.company.training.repository.PdfDocumentRepository;
import com.company.training.repository.TrainingDirectionRepository;
import jakarta.transaction.Transactional;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PdfProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(PdfProcessingService.class);
    private final Path pdfStorageLocation;
    private final Path protocolStorageLocation;

    @Autowired
    private PdfDocumentRepository pdfDocumentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TrainingDirectionRepository trainingDirectionRepository;

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ParsingDebugService parsingDebugService;

    private final Tesseract tesseract;

    // Паттерны для поиска
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{4})");
    private static final Pattern REG_NUM_PATTERN = Pattern.compile("\\b(\\d{4,15})\\b");
    private static final Pattern RUSSIAN_NAME_PATTERN = Pattern.compile("[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+");
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("=== Страница (\\d+) ===");

    public PdfProcessingService() {
        this.pdfStorageLocation = Paths.get("uploads/pdf-protocols").toAbsolutePath().normalize();
        this.protocolStorageLocation = Paths.get("uploads/protocols").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.pdfStorageLocation);
            Files.createDirectories(this.protocolStorageLocation);
            logger.info("Директории созданы: PDF={}, Protocols={}",
                    this.pdfStorageLocation, this.protocolStorageLocation);
        } catch (Exception ex) {
            logger.error("Не удалось создать директории", ex);
            throw new RuntimeException("Не удалось создать директории", ex);
        }

        // Инициализация Tesseract
        tesseract = new Tesseract();
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("rus");
        tesseract.setPageSegMode(6);
        tesseract.setTessVariable("user_defined_dpi", "400");
    }

    /**
     * Загрузка и сохранение PDF файла
     */
    public PdfDocument uploadPdf(MultipartFile file, PdfDocument.PdfType type) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path targetLocation = this.pdfStorageLocation.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        PdfDocument pdfDocument = new PdfDocument();
        pdfDocument.setOriginalFilename(originalFilename);
        pdfDocument.setStoredFilename(storedFilename);
        pdfDocument.setType(type);
        pdfDocument.setFilePath(targetLocation.toString());
        pdfDocument.setStatus(PdfDocument.ProcessingStatus.PENDING);

        try (PDDocument document = Loader.loadPDF(new File(targetLocation.toString()))) {
            pdfDocument.setPageCount(document.getNumberOfPages());
        } catch (Exception e) {
            logger.error("Ошибка при получении количества страниц PDF", e);
        }

        return pdfDocumentRepository.save(pdfDocument);
    }

    /**
     * OCR распознавание PDF
     */
    public PdfDocument processOcr(Long pdfDocumentId) throws Exception {
        PdfDocument pdfDocument = pdfDocumentRepository.findById(pdfDocumentId)
                .orElseThrow(() -> new RuntimeException("PDF документ не найден"));

        pdfDocument.setStatus(PdfDocument.ProcessingStatus.PROCESSING);
        pdfDocumentRepository.save(pdfDocument);

        File pdfFile = new File(pdfDocument.getFilePath());
        StringBuilder ocrText = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                logger.info("Обработка страницы {} из {}", i + 1, document.getNumberOfPages());
                BufferedImage image = renderer.renderImageWithDPI(i, 400);
                String pageText = tesseract.doOCR(image);
                ocrText.append("=== Страница ").append(i + 1).append(" ===\n");
                ocrText.append(pageText).append("\n");
            }

            pdfDocument.setOcrText(ocrText.toString());
            pdfDocument.setStatus(PdfDocument.ProcessingStatus.NEEDS_REVIEW);
            pdfDocument.setProcessedAt(java.time.LocalDateTime.now());
            return pdfDocumentRepository.save(pdfDocument);
        } catch (Exception e) {
            pdfDocument.setStatus(PdfDocument.ProcessingStatus.ERROR);
            pdfDocument.setProcessingResult("Ошибка OCR: " + e.getMessage());
            pdfDocumentRepository.save(pdfDocument);
            throw e;
        }
    }

    /**
     * Извлекает отдельную страницу из PDF и сохраняет как отдельный файл
     */
    private String extractSinglePageFromPdf(File sourcePdf, int pageNumber,
                                            Long employeeId, Long directionId,
                                            LocalDate examDate) throws IOException {

        try (PDDocument sourceDoc = Loader.loadPDF(sourcePdf)) {
            if (pageNumber < 1 || pageNumber > sourceDoc.getNumberOfPages()) {
                throw new IllegalArgumentException("Неверный номер страницы: " + pageNumber);
            }

            // Создаем новый документ с одной страницей
            try (PDDocument singlePageDoc = new PDDocument()) {
                // Получаем нужную страницу (индексация с 0)
                PDPage page = sourceDoc.getPage(pageNumber - 1);
                singlePageDoc.addPage(page);

                // Генерируем имя файла
                String fileName = String.format("protocol_%d_%d_%s_page%d.pdf",
                        employeeId,
                        directionId,
                        examDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                        pageNumber);

                Path outputPath = this.protocolStorageLocation.resolve(fileName);
                singlePageDoc.save(outputPath.toFile());

                logger.info("Страница {} извлечена в файл: {}", pageNumber, fileName);
                return fileName;
            }
        }
    }

    /**
     * УЛУЧШЕННЫЙ метод сохранения записей с извлечением отдельных страниц
     */
    @Transactional
    public ProtocolValidationResult saveConfirmedRecords(List<ParsedProtocolRecord> records, Long pdfDocumentId) {
        PdfDocument pdfDocument = pdfDocumentRepository.findById(pdfDocumentId)
                .orElseThrow(() -> new RuntimeException("PDF документ не найден"));

        int savedRecords = 0;
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        File sourceFile = new File(pdfDocument.getFilePath());

        for (ParsedProtocolRecord record : records) {
            if (record == null || !record.isValid()) {
                continue;
            }

            try {
                // 1. Находим сотрудника
                if (record.getMatchedEmployeeId() == null) {
                    errors.add("Сотрудник не указан для записи: " + record.getFullName());
                    continue;
                }

                Employee employee;
                try {
                    employee = employeeRepository.findById(record.getMatchedEmployeeId())
                            .orElseThrow(() -> new RuntimeException("Сотрудник не найден в БД"));
                } catch (Exception e) {
                    errors.add("Сотрудник не найден (ID: " + record.getMatchedEmployeeId() + "): " + e.getMessage());
                    continue;
                }

                // 2. Находим направление обучения
                TrainingDirection direction;
                Long directionId = record.getSelectedDirectionId();

                if (directionId == null) {
                    errors.add("Направление не указано для " + record.getFullName());
                    continue;
                }

                try {
                    direction = trainingDirectionRepository.findById(directionId)
                            .orElseThrow(() -> new RuntimeException("Направление не найдено"));
                } catch (Exception e) {
                    errors.add("Направление не найдено (ID: " + directionId + ") для " + record.getFullName() + ": " + e.getMessage());
                    continue;
                }

                // 3. Проверяем номер страницы
                if (record.getPageNumber() == null) {
                    errors.add("Номер страницы не указан для " + record.getFullName());
                    continue;
                }

                // 4. Проверяем, существует ли уже такая запись об обучении
                TrainingRecord existingRecord = trainingService.getTrainingRecord(
                        employee.getId(), direction.getId());

                if (existingRecord != null) {
                    // 4a. Удаляем старый файл протокола, если он существует
                    if (existingRecord.getFilePath() != null) {
                        try {
                            Path oldFilePath = this.protocolStorageLocation.resolve(existingRecord.getFilePath());
                            Files.deleteIfExists(oldFilePath);
                            logger.info("Удален старый файл протокола: {}", existingRecord.getFilePath());
                        } catch (IOException e) {
                            logger.warn("Не удалось удалить старый файл: {}", e.getMessage());
                        }
                    }

                    // 4b. Извлекаем новую страницу как отдельный файл
                    String protocolFileName = extractSinglePageFromPdf(
                            sourceFile,
                            record.getPageNumber(),
                            employee.getId(),
                            direction.getId(),
                            record.getExamDate());

                    // 4c. Обновляем существующую запись
                    existingRecord.setExamDate(record.getExamDate());
                    existingRecord.setProtocolNumber(record.getRegistrationNumber());
                    existingRecord.setApplicable(true);
                    existingRecord.setFileName(record.getFullName() + " - " + direction.getName());
                    existingRecord.setFilePath(protocolFileName);

                    trainingService.saveTrainingRecordWithoutFile(existingRecord);
                    successes.add("Обновлена запись для " + employee.getFullName() +
                            " по направлению " + direction.getName() +
                            " (страница " + record.getPageNumber() + ")");
                } else {
                    // 5. Создаем новую запись с извлеченной страницей
                    TrainingRecord trainingRecord = new TrainingRecord();
                    trainingRecord.setEmployee(employee);
                    trainingRecord.setTrainingDirection(direction);
                    trainingRecord.setExamDate(record.getExamDate());
                    trainingRecord.setProtocolNumber(record.getRegistrationNumber());
                    trainingRecord.setApplicable(true);

                    // Извлекаем страницу как отдельный файл
                    String protocolFileName = extractSinglePageFromPdf(
                            sourceFile,
                            record.getPageNumber(),
                            employee.getId(),
                            direction.getId(),
                            record.getExamDate());

                    trainingRecord.setFileName(record.getFullName() + " - " + direction.getName());
                    trainingRecord.setFilePath(protocolFileName);

                    trainingService.saveTrainingRecordWithoutFile(trainingRecord);
                    successes.add("Создана новая запись для " + employee.getFullName() +
                            " по направлению " + direction.getName() +
                            " (страница " + record.getPageNumber() + ")");
                }

                savedRecords++;

            } catch (Exception e) {
                String errorMsg = String.format("Ошибка при сохранении записи для %s (страница %d): %s",
                        record.getFullName(), record.getPageNumber(), e.getMessage());
                logger.error(errorMsg, e);
                errors.add(errorMsg);
            }
        }

        // Обновляем статус PDF документа
        pdfDocument.setStatus(PdfDocument.ProcessingStatus.PROCESSED);

        StringBuilder processingResult = new StringBuilder();
        processingResult.append(String.format(
                "Обработано %d записей из %d", savedRecords, records.size()));

        if (!successes.isEmpty()) {
            processingResult.append(". Успешно: ").append(String.join("; ",
                    successes.subList(0, Math.min(successes.size(), 3))));
        }

        if (!errors.isEmpty()) {
            processingResult.append(". Ошибки: ").append(String.join("; ",
                    errors.subList(0, Math.min(errors.size(), 3))));
            if (errors.size() > 3) {
                processingResult.append(" и еще ").append(errors.size() - 3).append(" ошибок");
            }
        }

        pdfDocument.setProcessingResult(processingResult.toString());
        pdfDocumentRepository.save(pdfDocument);

        ProtocolValidationResult result = new ProtocolValidationResult();
        result.setPdfDocumentId(pdfDocumentId);
        result.setTotalRecords(records.size());
        result.setValidRecords(savedRecords);
        result.setAllValid(savedRecords == records.size());
        result.setErrors(errors);
        result.setSuccesses(successes);
        return result;
    }

    /**
     * УЛУЧШЕННЫЙ алгоритм парсинга протоколов по охране труда
     * Обрабатывает КАЖДУЮ страницу как отдельный протокол
     */
    public List<ParsedProtocolRecord> parseOccupationalSafetyProtocol(String ocrText, Long pdfDocumentId) {
        logger.info("=== Начинаем УЛУЧШЕННЫЙ алгоритм парсинга протокола ===");

        List<ParsedProtocolRecord> records = new ArrayList<>();
        List<TrainingDirection> allDirections = trainingDirectionRepository.findAllByOrderByNameAsc();
        List<Employee> allEmployees = employeeRepository.findAll();

        logger.info("Загружено направлений из БД: {}", allDirections.size());
        logger.info("Загружено сотрудников из БД: {}", allEmployees.size());

        // Карта для быстрого поиска сотрудников
        Map<String, Employee> employeeMap = new HashMap<>();
        for (Employee emp : allEmployees) {
            if (emp.getFullName() != null) {
                String normalizedFullName = normalizeForSearch(emp.getFullName());
                employeeMap.put(normalizedFullName, emp);
                String[] nameParts = emp.getFullName().split("\\s+");
                if (nameParts.length >= 2) {
                    String lastNameFirstName = normalizeForSearch(nameParts[0] + " " + nameParts[1]);
                    employeeMap.put(lastNameFirstName, emp);
                }
            }
        }

        // 🔴 ИСПРАВЛЕНИЕ: Используем простой и надежный подход со split
        String[] pageSections = ocrText.split("=== Страница \\d+ ===");
        logger.info("Разделили текст на {} секций", pageSections.length);

        // Отладочная информация
        logger.debug("Первая секция (до первой страницы):\n{}",
                pageSections[0].substring(0, Math.min(100, pageSections[0].length())));

        // 🔴 ВАЖНО: pageSections[0] - это текст ДО первой страницы (может быть пустой или содержать метаданные)
        // Начинаем с 1, так как первая реальная страница в pageSections[1]
        for (int sectionIndex = 1; sectionIndex < pageSections.length; sectionIndex++) {
            String pageText = pageSections[sectionIndex].trim();
            int pageNumber = sectionIndex; // Номер страницы равен индексу в массиве

            logger.info("--- Обработка страницы {} (индекс {}) ---", pageNumber, sectionIndex);
            logger.debug("Текст страницы (первые 200 символов):\n{}",
                    pageText.substring(0, Math.min(200, pageText.length())));

            if (pageText.isEmpty()) {
                logger.info("Страница {} пустая, пропускаем", pageNumber);
                continue;
            }

            try {
                // 1. Ищем сотрудника на странице
                Employee employee = findEmployeeOnPage(pageText, employeeMap, allEmployees);
                if (employee == null) {
                    logger.warn("На странице {} не найден сотрудник", pageNumber);
                    employee = findEmployeeBySurname(pageText, allEmployees);
                    if (employee == null) {
                        logger.error("Не удалось найти сотрудника на странице {}", pageNumber);
                        continue;
                    }
                }
                logger.info("На странице {} найден сотрудник: {}", pageNumber, employee.getFullName());

                // 2. Ищем дату экзамена
                LocalDate examDate = extractExamDateFromPage(pageText);
                if (examDate == null) {
                    logger.warn("На странице {} не найдена дата экзамена", pageNumber);
                    List<LocalDate> allDates = extractAllDates(pageText);
                    if (!allDates.isEmpty()) {
                        examDate = allDates.get(0);
                        logger.info("Используем первую найденную дату: {}", examDate);
                    } else {
                        logger.error("На странице {} нет ни одной даты", pageNumber);
                        continue;
                    }
                }
                logger.info("Дата экзамена на странице {}: {}", pageNumber, examDate);

                // 3. Ищем номер протокола
                String protocolNumber = extractProtocolNumberFromPage(pageText);
                logger.info("Номер протокола на странице {}: {}", pageNumber, protocolNumber);

                // 4. Определяем программу обучения
                TrainingDirection direction = determineTrainingDirection(pageText, allDirections);
                if (direction != null) {
                    logger.info("Программа на странице {}: {}", pageNumber, direction.getName());
                } else {
                    logger.warn("Не удалось определить программу на странице {}", pageNumber);
                }

                // 5. Создаем запись
                ParsedProtocolRecord record = createParsedRecord(
                        employee, examDate, protocolNumber, direction,
                        pdfDocumentId, pageNumber  // Используем правильный номер страницы
                );

                // Проверяем на дубликаты
                if (!isDuplicateRecord(records, record)) {
                    records.add(record);
                    logger.info("✓ ДОБАВЛЕНА ЗАПИСЬ: {} - {} - {} (страница {})",
                            employee.getFullName(), examDate,
                            direction != null ? direction.getName() : "нет программы",
                            pageNumber);
                } else {
                    logger.info("Запись уже существует, пропускаем");
                }

            } catch (Exception e) {
                logger.error("Ошибка при обработке страницы {}: {}", pageNumber, e.getMessage(), e);
            }
        }

        logger.info("=== Парсинг завершен. Найдено {} записей из {} протоколов ===",
                records.size(), pageSections.length - 1); // -1 потому что первая секция не страница

        return records;
    }

    /**
     * Поиск сотрудника на странице (основной метод)
     */
    private Employee findEmployeeOnPage(String pageText, Map<String, Employee> employeeMap, List<Employee> allEmployees) {
        // 1. Точное совпадение по ФИО из карты
        String normalizedPageText = normalizeForSearch(pageText);

        for (Map.Entry<String, Employee> entry : employeeMap.entrySet()) {
            if (normalizedPageText.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 2. Ищем по полному ФИО в тексте
        for (Employee employee : allEmployees) {
            if (employee.getFullName() == null) continue;

            // Проверяем несколько вариантов
            String normalizedEmployeeName = normalizeForSearch(employee.getFullName());

            // Точное вхождение
            if (normalizedPageText.contains(normalizedEmployeeName)) {
                return employee;
            }

            // Проверяем по фамилии и имени (без отчества)
            String[] nameParts = employee.getFullName().split("\\s+");
            if (nameParts.length >= 2) {
                String lastNameFirstName = normalizeForSearch(nameParts[0] + " " + nameParts[1]);
                if (normalizedPageText.contains(lastNameFirstName)) {
                    return employee;
                }
            }
        }

        return null;
    }

    /**
     * Поиск сотрудника по фамилии (резервный метод)
     */
    private Employee findEmployeeBySurname(String pageText, List<Employee> allEmployees) {
        // Извлекаем все слова, похожие на фамилии (с заглавной буквы, длиной > 3)
        Pattern surnamePattern = Pattern.compile("[А-ЯЁ][а-яё]{3,}");
        Matcher matcher = surnamePattern.matcher(pageText);

        Set<String> possibleSurnames = new HashSet<>();
        while (matcher.find()) {
            possibleSurnames.add(matcher.group());
        }

        // Ищем среди сотрудников
        for (Employee employee : allEmployees) {
            if (employee.getFullName() == null) continue;

            String[] nameParts = employee.getFullName().split("\\s+");
            if (nameParts.length == 0) continue;

            String employeeSurname = nameParts[0];
            if (possibleSurnames.contains(employeeSurname)) {
                return employee;
            }

            // Проверяем с учетом ошибок OCR
            for (String possibleSurname : possibleSurnames) {
                if (calculateSimilarity(employeeSurname.toLowerCase(), possibleSurname.toLowerCase()) > 0.7) {
                    logger.warn("Найдено приблизительное совпадение по фамилии: {} -> {}", possibleSurname, employeeSurname);
                    return employee;
                }
            }
        }

        return null;
    }

    /**
     * Извлечение даты экзамена из страницы
     */
    private LocalDate extractExamDateFromPage(String pageText) {
        List<LocalDate> allDates = extractAllDates(pageText);

        // Ищем даты, которые похожи на даты экзамена (не слишком старые)
        for (LocalDate date : allDates) {
            // Проверяем, что дата реалистичная для экзамена
            if (date.getYear() >= 2020 && date.getYear() <= LocalDate.now().getYear() + 1) {
                return date;
            }
        }

        // Если не нашли подходящую, берем первую
        return allDates.isEmpty() ? null : allDates.get(0);
    }

    /**
     * Извлечение ВСЕХ дат из текста
     */
    private List<LocalDate> extractAllDates(String text) {
        List<LocalDate> dates = new ArrayList<>();
        Matcher matcher = DATE_PATTERN.matcher(text);

        while (matcher.find()) {
            try {
                LocalDate date = LocalDate.parse(matcher.group(1),
                        DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                dates.add(date);
            } catch (DateTimeParseException e) {
                // Пропускаем некорректные даты
                logger.debug("Ошибка парсинга даты: {}", matcher.group(1));
            }
        }

        // Сортируем по возрастанию
        dates.sort(Comparator.naturalOrder());
        return dates;
    }

    /**
     * Извлечение номера протокола из страницы
     */
    private String extractProtocolNumberFromPage(String pageText) {
        // Ищем последовательности из 8-12 цифр (номера протоколов)
        Matcher matcher = REG_NUM_PATTERN.matcher(pageText);

        while (matcher.find()) {
            String number = matcher.group();
            // Фильтруем слишком короткие и слишком длинные номера
            if (number.length() >= 6 && number.length() <= 12) {
                return number;
            }
        }

        return null;
    }

    /**
     * Определение направления обучения на странице
     */
    private TrainingDirection determineTrainingDirection(String pageText, List<TrainingDirection> allDirections) {
        String lowerText = pageText.toLowerCase();

        // Карта для подсчета совпадений
        Map<TrainingDirection, Integer> matchScores = new HashMap<>();

        for (TrainingDirection direction : allDirections) {
            int score = 0;

            // Проверяем name (короткое название)
            if (direction.getName() != null) {
                String lowerName = direction.getName().toLowerCase();
                if (lowerText.contains(lowerName)) {
                    score += 10;
                }
            }

            // Проверяем description (полное описание)
            if (direction.getDescription() != null) {
                String lowerDesc = direction.getDescription().toLowerCase();

                // Ищем ключевые слова из описания
                if (lowerText.contains(lowerDesc)) {
                    score += 100; // Полное совпадение!
                } else {
                    // Ищем частичные совпадения
                    String[] keywords = lowerDesc.split("[,\\.;\\s]+");
                    for (String keyword : keywords) {
                        keyword = keyword.trim();
                        if (keyword.length() > 3 && lowerText.contains(keyword)) {
                            score += 5;
                        }
                    }
                }
            }

            // Проверяем ключевые слова для разных типов программ
            if (lowerText.contains("электроустановк")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("электро")) {
                    score += 20;
                }
                if (direction.getDescription() != null && direction.getDescription().toLowerCase().contains("электро")) {
                    score += 20;
                }
            }

            if (lowerText.contains("сосуд") && lowerText.contains("давлен")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("сосуд")) {
                    score += 20;
                }
            }

            if (lowerText.contains("огнев")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("огнев")) {
                    score += 20;
                }
            }

            if (lowerText.contains("газоопасн")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("газоопасн")) {
                    score += 20;
                }
            }

            if (lowerText.contains("первая помощь")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("перв")) {
                    score += 30;
                }
            }

            if (lowerText.contains("средств индивидуальной защиты") || lowerText.contains("сиз")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("сиз")) {
                    score += 30;
                }
            }

            if (lowerText.contains("общие вопросы охраны труда") || lowerText.contains("системы управления охраной труда")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("общие")) {
                    score += 30;
                }
            }

            if (lowerText.contains("вредных и опасных производственных факторов")) {
                if (direction.getName() != null && direction.getName().toLowerCase().contains("вредн")) {
                    score += 30;
                }
            }

            if (score > 0) {
                matchScores.put(direction, score);
            }
        }

        // Находим направление с максимальным score
        TrainingDirection bestMatch = null;
        int maxScore = 0;

        for (Map.Entry<TrainingDirection, Integer> entry : matchScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestMatch = entry.getKey();
            }
        }

        return bestMatch;
    }

    /**
     * Создание объекта ParsedProtocolRecord
     */
    private ParsedProtocolRecord createParsedRecord(Employee employee, LocalDate examDate,
                                                    String protocolNumber, TrainingDirection direction,
                                                    Long pdfDocumentId, int pageNumber) {
        ParsedProtocolRecord record = new ParsedProtocolRecord();

        record.setFullName(employee.getFullName());
        record.setPosition(employee.getPosition());
        record.setDepartment(employee.getDepartment() != null ?
                employee.getDepartment().getName() : "Не указан");
        record.setExamDate(examDate);
        record.setRegistrationNumber(protocolNumber);
        record.setPdfDocumentId(pdfDocumentId);
        record.setFileName("Протокол_" + employee.getId() + "_" + System.currentTimeMillis() + ".pdf");
        record.setPageNumber(pageNumber);

        // Сопоставление с сотрудником
        record.setMatchedFullName(employee.getFullName());
        record.setMatchedEmployeeId(employee.getId());
        record.setConfidence("100%");

        // Направление обучения
        if (direction != null) {
            record.setTrainingDirection(direction.getDescription() != null ?
                    direction.getDescription() : direction.getName());
            record.setMatchedDirectionId(direction.getId());
            record.setSelectedDirectionId(direction.getId());
            record.setDirectionConfidence("100%");
        } else {
            record.setTrainingDirection("Не определено - требуется ручной выбор");
            record.setDirectionConfidence("0%");
        }

        return record;
    }

    /**
     * Нормализация текста для поиска
     */
    private String normalizeForSearch(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replace("ё", "е")
                .replaceAll("[^а-яё\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Проверка на дубликаты
     */
    private boolean isDuplicateRecord(List<ParsedProtocolRecord> records, ParsedProtocolRecord newRecord) {
        for (ParsedProtocolRecord existing : records) {
            boolean sameEmployee = existing.getFullName() != null &&
                    newRecord.getFullName() != null &&
                    existing.getFullName().equalsIgnoreCase(newRecord.getFullName());

            boolean sameDate = existing.getExamDate() != null &&
                    newRecord.getExamDate() != null &&
                    existing.getExamDate().equals(newRecord.getExamDate());

            boolean sameDirection = existing.getSelectedDirectionId() != null &&
                    newRecord.getSelectedDirectionId() != null &&
                    existing.getSelectedDirectionId().equals(newRecord.getSelectedDirectionId());

            if (sameEmployee && sameDate && sameDirection) {
                return true;
            }
        }
        return false;
    }

    /**
     * Алгоритм Левенштейна для нечеткого сравнения
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;

        s1 = s1.toLowerCase().replaceAll("[^а-яё]", "");
        s2 = s2.toLowerCase().replaceAll("[^а-яё]", "");

        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        int distance = dp[s1.length()][s2.length()];
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Получить все PDF документы
     */
    public List<PdfDocument> getAllPdfDocuments() {
        return pdfDocumentRepository.findAllByOrderByUploadedAtDesc();
    }

    /**
     * Получить PDF документ по ID
     */
    public PdfDocument getPdfDocumentById(Long id) {
        return pdfDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PDF документ не найден"));
    }

    /**
     * Получить все направления обучения из базы данных
     */
    public List<TrainingDirection> getAllTrainingDirections() {
        return trainingDirectionRepository.findAllByOrderByNameAsc();
    }
}