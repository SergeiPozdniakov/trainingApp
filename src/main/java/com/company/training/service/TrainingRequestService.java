package com.company.training.service;

import com.company.training.entity.*;
import com.company.training.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.mail.MessagingException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainingRequestService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingRequestService.class);

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    private Clock clock = Clock.systemDefaultZone();

    // Вложенный класс для склонения русских ФИО
    private static class RussianNameDecliner {

        // Основной метод для склонения полного ФИО
        public static String declineToDative(String fullName) {
            if (fullName == null || fullName.trim().isEmpty()) {
                return "";
            }

            String[] parts = fullName.split("\\s+");
            if (parts.length < 2) {
                return fullName; // Неполное ФИО
            }

            // Определяем пол по отчеству или имени
            boolean isMale = determineGender(parts);

            // Разбираем на части (возможны варианты: ФИО или Ф И)
            String lastName = parts[0];
            String firstName = parts[1];
            String middleName = parts.length > 2 ? parts[2] : null;

            return String.format("%s %s %s",
                    declineLastName(lastName, isMale),
                    declineFirstName(firstName, isMale),
                    middleName != null ? declineMiddleName(middleName, isMale) : ""
            ).trim();
        }

        // Определение пола
        private static boolean determineGender(String[] nameParts) {
            // Если есть отчество, определяем по нему
            if (nameParts.length > 2) {
                String middleName = nameParts[2].toLowerCase();
                return middleName.endsWith("вич") || middleName.endsWith("ич");
            }

            // Если отчества нет, определяем по имени
            if (nameParts.length > 1) {
                String firstName = nameParts[1].toLowerCase();
                // Мужские имена обычно оканчиваются на согласную, й, ь
                if (firstName.endsWith("й") || firstName.endsWith("ь") ||
                        isConsonant(firstName.charAt(firstName.length() - 1))) {
                    // Исключения - женские имена, оканчивающиеся на согласную
                    Set<String> femaleExceptions = Set.of(
                            "любовь", "нинель", "гаэль", "рашель", "ассоль", "юдифь"
                    );
                    return !femaleExceptions.contains(firstName);
                }
                // Женские имена обычно оканчиваются на а, я
                if (firstName.endsWith("а") || firstName.endsWith("я")) {
                    // Исключения - мужские имена, оканчивающиеся на а, я
                    Set<String> maleExceptions = Set.of(
                            "илья", "кузьма", "никита", "фома", "савва", "льва"
                    );
                    return maleExceptions.contains(firstName);
                }
            }

            return true; // по умолчанию считаем мужским
        }

        // Склонение фамилии
        private static String declineLastName(String lastName, boolean isMale) {
            if (lastName == null || lastName.isEmpty()) return "";

            lastName = lastName.trim();
            String lowerLastName = lastName.toLowerCase();

            // Несклоняемые фамилии
            if (isUnchangeableLastName(lastName)) {
                return lastName;
            }

            if (!isMale) {
                return declineFemaleLastName(lastName, lowerLastName);
            }

            return declineMaleLastName(lastName, lowerLastName);
        }

        // Склонение мужских фамилий
        private static String declineMaleLastName(String lastName, String lowerLastName) {
            // Фамилии на -ов, -ев, -ин, -ын
            if (lowerLastName.endsWith("ов") || lowerLastName.endsWith("ев") ||
                    lowerLastName.endsWith("ин") || lowerLastName.endsWith("ын")) {
                // Исключения (иностранные фамилии)
                if (isException(lastName, Set.of("дарвин", "чаплин", "франклин"))) {
                    return lastName + "у";
                }
                return lastName + "у";
            }

            // Фамилии на -ский, -цкий, -ской, -цкой
            if (lowerLastName.endsWith("ский") || lowerLastName.endsWith("цкий") ||
                    lowerLastName.endsWith("ской") || lowerLastName.endsWith("цкой")) {
                return lastName.substring(0, lastName.length() - 2) + "ому";
            }

            // Фамилии на -ой
            if (lowerLastName.endsWith("ой")) {
                return lastName.substring(0, lastName.length() - 2) + "ому";
            }

            // Фамилии на -ий (прилагательные)
            if (lowerLastName.endsWith("ий")) {
                return lastName.substring(0, lastName.length() - 2) + "ию";
            }

            // Фамилии на -ай
            if (lowerLastName.endsWith("ай")) {
                return lastName.substring(0, lastName.length() - 2) + "аю";
            }

            // Фамилии на -й
            if (lowerLastName.endsWith("й")) {
                return lastName.substring(0, lastName.length() - 1) + "ю";
            }

            // Фамилии на -ь
            if (lowerLastName.endsWith("ь")) {
                return lastName.substring(0, lastName.length() - 1) + "ю";
            }

            // Фамилии на согласную (кроме -ж, -ш, -ч, -щ, которые могут быть в женских фамилиях)
            if (isConsonant(lowerLastName.charAt(lowerLastName.length() - 1))) {
                return lastName + "у";
            }

            // Для остальных случаев (иностранные фамилии на гласную)
            return lastName;
        }

        // Склонение женских фамилий
        private static String declineFemaleLastName(String lastName, String lowerLastName) {
            // Фамилии на -ова, -ева, -ина, -ына
            if (lowerLastName.endsWith("ова") || lowerLastName.endsWith("ева") ||
                    lowerLastName.endsWith("ина") || lowerLastName.endsWith("ына")) {
                return lastName.substring(0, lastName.length() - 1) + "ой";
            }

            // Фамилии на -ская, -цкая
            if (lowerLastName.endsWith("ская") || lowerLastName.endsWith("цкая")) {
                return lastName.substring(0, lastName.length() - 3) + "кой";
            }

            // Фамилии на -ая
            if (lowerLastName.endsWith("ая")) {
                return lastName.substring(0, lastName.length() - 2) + "ой";
            }

            // Фамилии на -яя
            if (lowerLastName.endsWith("яя")) {
                return lastName.substring(0, lastName.length() - 2) + "ей";
            }

            // Фамилии на -а
            if (lowerLastName.endsWith("а")) {
                // Исключения (несклоняемые)
                if (isException(lastName, Set.of("круз", "гришко", "дюма", "золя"))) {
                    return lastName;
                }
                // Проверяем, не иностранная ли фамилия
                if (isForeignLastName(lastName)) {
                    return lastName;
                }
                return lastName.substring(0, lastName.length() - 1) + "е";
            }

            // Фамилии на -я
            if (lowerLastName.endsWith("я")) {
                return lastName.substring(0, lastName.length() - 1) + "е";
            }

            // Женские фамилии, оканчивающиеся на согласную, обычно не склоняются
            return lastName;
        }

        // Склонение имени
        private static String declineFirstName(String firstName, boolean isMale) {
            if (firstName == null || firstName.isEmpty()) return "";

            firstName = firstName.trim();
            String lowerFirstName = firstName.toLowerCase();

            if (isMale) {
                return declineMaleFirstName(firstName, lowerFirstName);
            } else {
                return declineFemaleFirstName(firstName, lowerFirstName);
            }
        }

        private static String declineMaleFirstName(String firstName, String lowerFirstName) {
            // Имена на -й
            if (lowerFirstName.endsWith("й")) {
                return firstName.substring(0, firstName.length() - 1) + "ю";
            }

            // Имена на -ь
            if (lowerFirstName.endsWith("ь")) {
                return firstName.substring(0, firstName.length() - 1) + "ю";
            }

            // Мужские имена на -а
            if (lowerFirstName.endsWith("а")) {
                if (firstName.equalsIgnoreCase("илья")) {
                    return "Илье";
                }
                if (firstName.equalsIgnoreCase("кузьма")) {
                    return "Кузьме";
                }
                if (firstName.equalsIgnoreCase("никита")) {
                    return "Никите";
                }
                if (firstName.equalsIgnoreCase("лука")) {
                    return "Луке";
                }
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            // Мужские имена на -я
            if (lowerFirstName.endsWith("я")) {
                if (firstName.equalsIgnoreCase("илья")) {
                    return "Илье"; // уже обработано выше, но для надежности
                }
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            // Имена на согласную
            if (isConsonant(lowerFirstName.charAt(lowerFirstName.length() - 1))) {
                // Проверяем мягкий знак перед согласной
                if (lowerFirstName.endsWith("ль") || lowerFirstName.endsWith("нь")) {
                    return firstName.substring(0, firstName.length() - 1) + "ю";
                }
                return firstName + "у";
            }

            // Для иностранных имен
            return firstName;
        }

        private static String declineFemaleFirstName(String firstName, String lowerFirstName) {
            // Имена на -а
            if (lowerFirstName.endsWith("а")) {
                // Имена на -ия
                if (lowerFirstName.endsWith("ия")) {
                    return firstName.substring(0, firstName.length() - 1) + "и";
                }
                // Имена на -ья
                if (lowerFirstName.endsWith("ья")) {
                    return firstName.substring(0, firstName.length() - 2) + "ье";
                }
                // Имена на -ла, -ра (греческие)
                if (lowerFirstName.endsWith("ла") || lowerFirstName.endsWith("ра")) {
                    return firstName.substring(0, firstName.length() - 1) + "е";
                }
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            // Имена на -я
            if (lowerFirstName.endsWith("я")) {
                // Имена на -ия уже обработаны выше
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            // Женские имена на -ь
            if (lowerFirstName.endsWith("ь")) {
                return firstName.substring(0, firstName.length() - 1) + "и";
            }

            // Женские имена на согласную (иностранные)
            return firstName;
        }

        // Склонение отчества
        private static String declineMiddleName(String middleName, boolean isMale) {
            if (middleName == null || middleName.isEmpty()) return "";

            middleName = middleName.trim();
            String lowerMiddleName = middleName.toLowerCase();

            if (isMale) {
                if (lowerMiddleName.endsWith("вич")) {
                    return middleName.substring(0, middleName.length() - 3) + "вичу";
                }
                if (lowerMiddleName.endsWith("ич")) {
                    return middleName.substring(0, middleName.length() - 2) + "ичу";
                }
            } else {
                if (lowerMiddleName.endsWith("вна")) {
                    return middleName.substring(0, middleName.length() - 3) + "вне";
                }
                if (lowerMiddleName.endsWith("чна")) {
                    return middleName.substring(0, middleName.length() - 3) + "чне";
                }
                if (lowerMiddleName.endsWith("на")) {
                    return middleName.substring(0, middleName.length() - 2) + "не";
                }
            }

            return middleName;
        }

        // Вспомогательные методы

        private static boolean isUnchangeableLastName(String lastName) {
            String lower = lastName.toLowerCase();

            // Фамилии на -ых, -их (не склоняются)
            if (lower.endsWith("ых") || lower.endsWith("их")) {
                return true;
            }

            // Иноязычные фамилии на -о, -е, -и, -у, -ю, -э, -ы
            if (lower.endsWith("о") || lower.endsWith("е") ||
                    lower.endsWith("и") || lower.endsWith("у") ||
                    lower.endsWith("ю") || lower.endsWith("э") ||
                    lower.endsWith("ы")) {
                return true;
            }

            // Фамилии на -аго, -яго, -ово, -ко
            if (lower.endsWith("аго") || lower.endsWith("яго") ||
                    lower.endsWith("ово") || lower.endsWith("ко")) {
                return true;
            }

            // Короткие иностранные фамилии (1-2 слога)
            if (lastName.length() <= 4 && isForeignLastName(lastName)) {
                return true;
            }

            return false;
        }

        private static boolean isForeignLastName(String lastName) {
            String lower = lastName.toLowerCase();
            // Простой способ - проверка на наличие не-русских букв
            String russianLetters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
            for (char c : lower.toCharArray()) {
                if (russianLetters.indexOf(c) == -1 && Character.isLetter(c)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isException(String name, Set<String> exceptions) {
            return exceptions.contains(name.toLowerCase());
        }

        private static boolean isConsonant(char c) {
            String consonants = "бвгджзйклмнпрстфхцчшщ";
            return consonants.indexOf(c) != -1;
        }
    }

    /**
     * Получает все записи, у которых в этом месяце начинается 3-месячный период до экзамена
     */
    public List<TrainingRecord> getRecordsWithThreeMonthPeriodStartingThisMonth() {
        LocalDate today = getCurrentDate();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<TrainingRecord> allRecords = getAllTrainingRecords();
        List<TrainingRecord> result = new ArrayList<>();

        for (TrainingRecord record : allRecords) {
            if (record.getExamDate() != null &&
                    record.getApplicable() != null &&
                    record.getApplicable()) {

                LocalDate nextExamDate = record.getNextExamDate();
                if (nextExamDate != null) {
                    // Вычисляем дату за 3 месяца до следующего экзамена
                    LocalDate threeMonthsBefore = nextExamDate.minusMonths(3);

                    // Проверяем, попадает ли эта дата в текущий месяц
                    if (!threeMonthsBefore.isBefore(firstDayOfMonth) &&
                            !threeMonthsBefore.isAfter(lastDayOfMonth)) {
                        result.add(record);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Преобразует ФИО в дательный падеж (использует улучшенный алгоритм)
     */
    private String convertToDativeCase(String fullName) {
        return RussianNameDecliner.declineToDative(fullName);
    }

    /**
     * Генерирует документ Word с заявкой на основе шаблона template.docx
     */
    public byte[] generateTrainingRequestDocument(List<TrainingRecord> records) throws Exception {
        logger.info("=== Начинаем заполнение документа ===");
        logger.info("✅ Используемый шрифт: Tahoma 8pt");

        // 1. Читаем файл шаблона
        String templatePath = "template.docx";

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            if (is == null) {
                logger.error("Файл шаблона не найден: {}", templatePath);
                logger.error("Положите ваш template.docx в папку src/main/resources/!");
                throw new RuntimeException("Файл шаблона не найден: " + templatePath);
            }

            XWPFDocument doc = new XWPFDocument(is);

            // 2. Ищем таблицу
            List<XWPFTable> tables = doc.getTables();
            logger.info("Найдено таблиц в документе: {}", tables.size());

            if (tables.isEmpty()) {
                logger.error("Таблиц не найдено!");
                throw new RuntimeException("В шаблоне не найдены таблицы");
            }

            XWPFTable table = tables.get(0);
            logger.info("Строк в таблице: {}", table.getNumberOfRows());

            // 3. Находим индекс строки с заголовком "п/п"
            int headerRowIndex = findHeaderRow(table);

            if (headerRowIndex == -1) {
                logger.error("Не найдена строка с заголовком 'п/п'!");
                throw new RuntimeException("Не найдена строка с заголовком 'п/п'");
            }

            logger.info("Заголовок найден в строке: {}", headerRowIndex);

            // 4. Удаляем все строки между заголовком и концом таблицы
            while (table.getNumberOfRows() > headerRowIndex + 1) {
                table.removeRow(headerRowIndex + 1);
            }

            // 5. Добавляем новые строки с данными
            for (int i = 0; i < records.size(); i++) {
                TrainingRecord record = records.get(i);

                XWPFTableRow newRow = table.createRow();

                // Заполняем ячейки с шрифтом Tahoma 8pt
                setCellTextWithTahoma(newRow, 0, String.valueOf(i + 1));                 // № п/п
                setCellTextWithTahoma(newRow, 1, record.getEmployee().getFullName());   // ФИО сотрудника
                // ФИО в дательном падеже генерируется на лету с использованием улучшенного алгоритма
                setCellTextWithTahoma(newRow, 2, convertToDativeCase(record.getEmployee().getFullName()));
                setCellTextWithTahoma(newRow, 3, record.getEmployee().getPosition());   // Должность (убираем отдел)
                setCellTextWithTahoma(newRow, 4, record.getTrainingDirection().getName()); // Направление
                setCellTextWithTahoma(newRow, 5, record.getExamDate().format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                setCellTextWithTahoma(newRow, 6, record.getNextExamDate().format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy")));

                logger.info("✓ Добавлена запись для: {}", record.getEmployee().getFullName());
                logger.debug("  Дательный падеж: {}", convertToDativeCase(record.getEmployee().getFullName()));
            }

            // 6. Сохраняем результат
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            doc.close();

            logger.info("=== Документ успешно сгенерирован ===");
            logger.info("📊 Добавлено записей: {}", records.size());
            logger.info("🎨 Шрифт для всех строк: Tahoma 8pt");

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Ошибка при генерации документа: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Поиск строки с заголовком "п/п"
     */
    private int findHeaderRow(XWPFTable table) {
        for (int i = 0; i < table.getNumberOfRows(); i++) {
            XWPFTableRow row = table.getRow(i);
            if (row != null && row.getTableCells().size() > 0) {
                String cellText = row.getCell(0).getText().trim().toLowerCase();
                if (cellText.contains("п/п") || cellText.contains("№ п/п") || cellText.contains("№п/п")) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Заполнение ячейки текстом с шрифтом Tahoma 8pt
     */
    private void setCellTextWithTahoma(XWPFTableRow row, int cellIndex, String text) {
        try {
            XWPFTableCell cell;

            // Если ячейка существует - используем ее, иначе создаем новую
            if (cellIndex < row.getTableCells().size()) {
                cell = row.getCell(cellIndex);
            } else {
                // Создаем недостающие ячейки
                for (int i = row.getTableCells().size(); i <= cellIndex; i++) {
                    row.createCell();
                }
                cell = row.getCell(cellIndex);
            }

            // Очищаем ячейку
            while (cell.getParagraphs().size() > 0) {
                cell.removeParagraph(0);
            }

            // Создаем новый параграф
            XWPFParagraph paragraph = cell.addParagraph();

            // Создаем run (текстовый блок) с настройками шрифта
            XWPFRun run = paragraph.createRun();
            run.setText(text != null ? text : "");

            // Устанавливаем шрифт Tahoma и размер 8pt
            run.setFontFamily("Tahoma");
            run.setFontSize(8);

            // Дополнительные настройки для единообразия
            paragraph.setSpacingAfter(0);
            paragraph.setSpacingBefore(0);

        } catch (Exception e) {
            logger.error("Ошибка при заполнении ячейки {}: {}", cellIndex, e.getMessage());
        }
    }

    /**
     * Отправка email с заявкой
     */
    public void sendTrainingRequestEmail(List<String> adminEmails, byte[] documentBytes,
                                         List<TrainingRecord> records) throws MessagingException {
        String monthName = getCurrentMonthName();
        String fileName = "Заявка_на_обучение_" + monthName.replace(" ", "_") + ".docx";

        for (String email : adminEmails) {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Заявка на обучение персонала на " + monthName);

            String emailText = String.format("""
                Уважаемый коллега!
                
                Во вложении находится заявка на обучение персонала на %s.
                
                В заявке включены сотрудники, у которых в этом месяце начинается 
                трехмесячный период до повторного экзамена/аттестации.
                
                Всего сотрудников в заявке: %d
                
                Список сотрудников:
                %s
                
                Пожалуйста, организуйте их обучение в соответствии с графиком.
                
                С уважением,
                Система управления обучением
                """,
                    monthName,
                    records.size(),
                    getEmployeeList(records));

            helper.setText(emailText);

            helper.addAttachment(fileName, () -> new ByteArrayInputStream(documentBytes));

            mailSender.send(message);
            logger.info("Отправлена заявка на обучение на адрес: {}", email);
        }
    }

    private String getEmployeeList(List<TrainingRecord> records) {
        StringBuilder sb = new StringBuilder();
        int counter = 1;

        for (TrainingRecord record : records) {
            Employee emp = record.getEmployee();
            sb.append(String.format("%d. %s (%s) - %s. Дата след. экзамена: %s%n",
                    counter++,
                    emp.getFullName(),
                    emp.getPosition(),
                    record.getTrainingDirection().getName(),
                    record.getNextExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        }

        return sb.toString();
    }

    /**
     * Главный метод для проверки и отправки заявок
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkAndSendTrainingRequests() {
        try {
            logger.info("Проверка необходимости отправки заявки на обучение...");
            logger.info("Текущая дата системы: {}", getCurrentDate());

            if (isFirstWorkingDayOfMonth()) {
                logger.info("Сегодня первый рабочий день месяца. Формируем заявку...");

                List<TrainingRecord> records = getRecordsWithThreeMonthPeriodStartingThisMonth();

                if (!records.isEmpty()) {
                    logger.info("Найдено {} записей для заявки на обучение", records.size());

                    List<User> admins = userRepository.findByAdminTrueAndEnabledTrue();
                    List<String> adminEmails = admins.stream()
                            .map(User::getEmail)
                            .collect(Collectors.toList());

                    if (!adminEmails.isEmpty()) {
                        byte[] documentBytes = generateTrainingRequestDocument(records);
                        sendTrainingRequestEmail(adminEmails, documentBytes, records);

                        logger.info("Заявка на обучение успешно отправлена {} администраторам", adminEmails.size());

                        for (TrainingRecord record : records) {
                            logger.info("Включен в заявку: {} - {}. Дата след. экзамена: {}",
                                    record.getEmployee().getFullName(),
                                    record.getTrainingDirection().getName(),
                                    record.getNextExamDate());
                        }
                    } else {
                        logger.warn("Не найдено активных администраторов для отправки заявки");
                    }
                } else {
                    logger.info("Нет записей для формирования заявки на этот месяц");
                }
            } else {
                logger.info("Сегодня не первый рабочий день месяца. Пропускаем отправку.");
            }
        } catch (Exception e) {
            logger.error("Ошибка при отправке заявки на обучение: {}", e.getMessage(), e);
        }
    }

    private List<TrainingRecord> getAllTrainingRecords() {
        return trainingService.getAllTrainingRecords();
    }

    private String getCurrentMonthName() {
        return getCurrentDate().format(DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("ru")));
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private LocalDate getCurrentDate() {
        return LocalDate.now(clock);
    }

    /**
     * Получение первого рабочего дня месяца
     */
    public LocalDate getFirstWorkingDayOfMonth(LocalDate date) {
        LocalDate firstDay = date.withDayOfMonth(1);

        while (firstDay.getDayOfWeek() == DayOfWeek.SATURDAY ||
                firstDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            firstDay = firstDay.plusDays(1);
        }

        return firstDay;
    }

    /**
     * Проверяет, является ли сегодня первый рабочий день месяца
     */
    public boolean isFirstWorkingDayOfMonth() {
        LocalDate today = getCurrentDate();
        LocalDate firstWorkingDay = getFirstWorkingDayOfMonth(today);

        return today.isEqual(firstWorkingDay);
    }

    public void manualTriggerRequestSending() {
        checkAndSendTrainingRequests();
    }

    public LocalDate getCurrentDatePublic() {
        return getCurrentDate();
    }

    public boolean isFirstWorkingDayOfMonthPublic() {
        return isFirstWorkingDayOfMonth();
    }
}