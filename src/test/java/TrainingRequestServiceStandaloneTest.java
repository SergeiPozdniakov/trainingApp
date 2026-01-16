import com.company.training.entity.*;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TrainingRequestServiceStandaloneTest {

    // Вложенный класс для склонения русских ФИО (такой же как в сервисе)
    private static class RussianNameDecliner {

        public static String declineToDative(String fullName) {
            if (fullName == null || fullName.trim().isEmpty()) {
                return "";
            }

            String[] parts = fullName.split("\\s+");
            if (parts.length < 2) {
                return fullName;
            }

            boolean isMale = determineGender(parts);

            String lastName = parts[0];
            String firstName = parts[1];
            String middleName = parts.length > 2 ? parts[2] : null;

            return String.format("%s %s %s",
                    declineLastName(lastName, isMale),
                    declineFirstName(firstName, isMale),
                    middleName != null ? declineMiddleName(middleName, isMale) : ""
            ).trim();
        }

        private static boolean determineGender(String[] nameParts) {
            if (nameParts.length > 2) {
                String middleName = nameParts[2].toLowerCase();
                return middleName.endsWith("вич") || middleName.endsWith("ич");
            }

            if (nameParts.length > 1) {
                String firstName = nameParts[1].toLowerCase();
                if (firstName.endsWith("й") || firstName.endsWith("ь") ||
                        isConsonant(firstName.charAt(firstName.length() - 1))) {
                    java.util.Set<String> femaleExceptions = java.util.Set.of(
                            "любовь", "нинель", "гаэль", "рашель"
                    );
                    return !femaleExceptions.contains(firstName);
                }
                if (firstName.endsWith("а") || firstName.endsWith("я")) {
                    java.util.Set<String> maleExceptions = java.util.Set.of(
                            "илья", "кузьма", "никита", "фома"
                    );
                    return maleExceptions.contains(firstName);
                }
            }
            return true;
        }

        private static String declineLastName(String lastName, boolean isMale) {
            if (lastName == null || lastName.isEmpty()) return "";

            lastName = lastName.trim();
            String lowerLastName = lastName.toLowerCase();

            if (isUnchangeableLastName(lastName)) {
                return lastName;
            }

            if (!isMale) {
                return declineFemaleLastName(lastName, lowerLastName);
            }

            return declineMaleLastName(lastName, lowerLastName);
        }

        private static String declineMaleLastName(String lastName, String lowerLastName) {
            if (lowerLastName.endsWith("ов") || lowerLastName.endsWith("ев") ||
                    lowerLastName.endsWith("ин") || lowerLastName.endsWith("ын")) {
                if (isException(lastName, java.util.Set.of("дарвин", "чаплин", "франклин"))) {
                    return lastName + "у";
                }
                return lastName + "у";
            }

            if (lowerLastName.endsWith("ский") || lowerLastName.endsWith("цкий") ||
                    lowerLastName.endsWith("ской") || lowerLastName.endsWith("цкой")) {
                return lastName.substring(0, lastName.length() - 2) + "ому";
            }

            if (lowerLastName.endsWith("ой")) {
                return lastName.substring(0, lastName.length() - 2) + "ому";
            }

            if (lowerLastName.endsWith("ий")) {
                return lastName.substring(0, lastName.length() - 2) + "ему";
            }

            if (lowerLastName.endsWith("ай")) {
                return lastName.substring(0, lastName.length() - 2) + "аю";
            }

            if (lowerLastName.endsWith("й")) {
                return lastName.substring(0, lastName.length() - 1) + "ю";
            }

            if (lowerLastName.endsWith("ь")) {
                return lastName.substring(0, lastName.length() - 1) + "ю";
            }

            if (isConsonant(lowerLastName.charAt(lowerLastName.length() - 1))) {
                return lastName + "у";
            }

            return lastName;
        }

        private static String declineFemaleLastName(String lastName, String lowerLastName) {
            // ОСНОВНОЕ ИСПРАВЛЕНИЕ: Петрова -> Петровой (не Петрове!)
            if (lowerLastName.endsWith("ова") || lowerLastName.endsWith("ева")) {
                return lastName.substring(0, lastName.length() - 1) + "ой";
            }

            if (lowerLastName.endsWith("ина") || lowerLastName.endsWith("ына")) {
                return lastName.substring(0, lastName.length() - 1) + "ой";
            }

            if (lowerLastName.endsWith("ская") || lowerLastName.endsWith("цкая")) {
                return lastName.substring(0, lastName.length() - 3) + "кой";
            }

            if (lowerLastName.endsWith("ая")) {
                return lastName.substring(0, lastName.length() - 2) + "ой";
            }

            if (lowerLastName.endsWith("яя")) {
                return lastName.substring(0, lastName.length() - 2) + "ей";
            }

            if (lowerLastName.endsWith("а")) {
                if (isException(lastName, java.util.Set.of("круз", "гришко", "дюма", "золя"))) {
                    return lastName;
                }
                if (isForeignLastName(lastName)) {
                    return lastName;
                }
                char secondLastChar = lowerLastName.charAt(lowerLastName.length() - 2);
                if (isConsonant(secondLastChar)) {
                    // Для фамилий типа "Смирна" -> "Смирне"
                    return lastName.substring(0, lastName.length() - 1) + "е";
                }
            }

            if (lowerLastName.endsWith("я")) {
                return lastName.substring(0, lastName.length() - 1) + "е";
            }

            return lastName;
        }

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
            if (lowerFirstName.endsWith("й")) {
                return firstName.substring(0, firstName.length() - 1) + "ю";
            }

            if (lowerFirstName.endsWith("ь")) {
                return firstName.substring(0, firstName.length() - 1) + "ю";
            }

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
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            if (lowerFirstName.endsWith("я")) {
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            if (isConsonant(lowerFirstName.charAt(lowerFirstName.length() - 1))) {
                if (lowerFirstName.endsWith("ль") || lowerFirstName.endsWith("нь")) {
                    return firstName.substring(0, firstName.length() - 1) + "ю";
                }
                return firstName + "у";
            }

            return firstName;
        }

        private static String declineFemaleFirstName(String firstName, String lowerFirstName) {
            if (lowerFirstName.endsWith("а")) {
                if (lowerFirstName.endsWith("ия")) {
                    return firstName.substring(0, firstName.length() - 1) + "и";
                }
                if (lowerFirstName.endsWith("ья")) {
                    return firstName.substring(0, firstName.length() - 2) + "ье";
                }
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            if (lowerFirstName.endsWith("я")) {
                return firstName.substring(0, firstName.length() - 1) + "е";
            }

            if (lowerFirstName.endsWith("ь")) {
                return firstName.substring(0, firstName.length() - 1) + "и";
            }

            return firstName;
        }

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

        private static boolean isUnchangeableLastName(String lastName) {
            String lower = lastName.toLowerCase();

            if (lower.endsWith("ых") || lower.endsWith("их")) {
                return true;
            }

            if (lower.endsWith("о") || lower.endsWith("е") ||
                    lower.endsWith("и") || lower.endsWith("у") ||
                    lower.endsWith("ю") || lower.endsWith("э") ||
                    lower.endsWith("ы")) {
                return true;
            }

            if (lower.endsWith("аго") || lower.endsWith("яго") ||
                    lower.endsWith("ово") || lower.endsWith("ко")) {
                return true;
            }

            if (lastName.length() <= 4 && isForeignLastName(lastName)) {
                return true;
            }

            return false;
        }

        private static boolean isForeignLastName(String lastName) {
            String lower = lastName.toLowerCase();
            String russianLetters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
            for (char c : lower.toCharArray()) {
                if (russianLetters.indexOf(c) == -1 && Character.isLetter(c)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isException(String name, java.util.Set<String> exceptions) {
            return exceptions.contains(name.toLowerCase());
        }

        private static boolean isConsonant(char c) {
            String consonants = "бвгджзйклмнпрстфхцчшщ";
            return consonants.indexOf(c) != -1;
        }
    }

    @Test
    public void generateTestDocument() throws Exception {
        System.out.println("=== Начинаем генерацию тестовой заявки ===");
        System.out.println("✅ Используемый шрифт: Tahoma 8pt");

        String templatePath = "template.docx";
        if (!Files.exists(Paths.get(templatePath))) {
            System.err.println("❌ Файл шаблона не найден: " + templatePath);
            System.err.println("❗ Положите ваш template.docx в корневую папку проекта!");
            return;
        }

        List<TrainingRecord> records = createTestRecords();
        byte[] documentBytes = generateTrainingRequestDocument(records);

        String outputPath = "generated_training_request.docx";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(documentBytes);
        }

        System.out.println("\n✅ Документ успешно создан!");
        System.out.println("📁 Путь к файлу: " + new File(outputPath).getAbsolutePath());
        System.out.println("📊 Количество записей: " + records.size());
        System.out.println("🎨 Все данные заполнены шрифтом Tahoma 8pt");

        // Дополнительная проверка склонения
        System.out.println("\n=== Проверка склонения ФИО ===");
        for (TrainingRecord record : records) {
            String original = record.getEmployee().getFullName();
            String dative = convertToDativeCase(original);
            System.out.println(original + " -> " + dative);
        }
    }

    private List<TrainingRecord> createTestRecords() {
        List<TrainingRecord> records = new ArrayList<>();

        Department itDept = new Department("IT-отдел", "Отдел информационных технологий");
        itDept.setId(1L);

        Department hrDept = new Department("Отдел кадров", "Отдел по управлению персоналом");
        hrDept.setId(2L);

        // Тестовые сотрудники с разными типами фамилий
        Employee emp1 = new Employee("Иванов Иван Иванович", "Ведущий инженер", "ivanov@example.com", itDept);
        emp1.setId(1L);

        Employee emp2 = new Employee("Петрова Анна Сергеевна", "Специалист по персоналу", "petrova@example.com", hrDept);
        emp2.setId(2L);

        Employee emp3 = new Employee("Сидоров Алексей Петрович", "Главный специалист", "sidorov@example.com", itDept);
        emp3.setId(3L);

        Employee emp4 = new Employee("Смирнова Елена Владимировна", "Менеджер проектов", "smirnova@example.com", itDept);
        emp4.setId(4L);

        Employee emp5 = new Employee("Козловский Михаил Андреевич", "Аналитик", "kozlovsky@example.com", itDept);
        emp5.setId(5L);

        TrainingDirection javaTraining = new TrainingDirection("Java разработка", 12, BigDecimal.valueOf(15000), "Углубленное изучение Java");
        javaTraining.setId(1L);

        TrainingDirection safetyTraining = new TrainingDirection("Охрана труда", 6, BigDecimal.valueOf(8000), "Базовый курс по охране труда");
        safetyTraining.setId(2L);

        TrainingDirection hrTraining = new TrainingDirection("Управление персоналом", 12, BigDecimal.valueOf(12000), "Современные методы HR-менеджмента");
        hrTraining.setId(3L);

        TrainingRecord record1 = new TrainingRecord(emp1, javaTraining);
        record1.setId(1L);
        record1.setExamDate(LocalDate.of(2024, 2, 1));
        record1.setApplicable(true);

        TrainingRecord record2 = new TrainingRecord(emp2, safetyTraining);
        record2.setId(2L);
        record2.setExamDate(LocalDate.of(2024, 2, 15));
        record2.setApplicable(true);

        TrainingRecord record3 = new TrainingRecord(emp3, hrTraining);
        record3.setId(3L);
        record3.setExamDate(LocalDate.of(2024, 3, 1));
        record3.setApplicable(true);

        TrainingRecord record4 = new TrainingRecord(emp4, javaTraining);
        record4.setId(4L);
        record4.setExamDate(LocalDate.of(2024, 3, 15));
        record4.setApplicable(true);

        TrainingRecord record5 = new TrainingRecord(emp5, safetyTraining);
        record5.setId(5L);
        record5.setExamDate(LocalDate.of(2024, 4, 1));
        record5.setApplicable(true);

        records.add(record1);
        records.add(record2);
        records.add(record3);
        records.add(record4);
        records.add(record5);

        return records;
    }

    private byte[] generateTrainingRequestDocument(List<TrainingRecord> records) throws Exception {
        System.out.println("🔄 Генерация документа на основе шаблона...");

        try (InputStream is = new FileInputStream("template.docx")) {
            XWPFDocument doc = new XWPFDocument(is);

            List<XWPFTable> tables = doc.getTables();
            System.out.println("🔍 Найдено таблиц в документе: " + tables.size());

            if (tables.isEmpty()) {
                throw new RuntimeException("В шаблоне не найдены таблицы");
            }

            XWPFTable table = tables.get(0);
            System.out.println("📊 Строк в таблице до очистки: " + table.getNumberOfRows());

            int headerRowIndex = findHeaderRow(table);

            if (headerRowIndex == -1) {
                throw new RuntimeException("Не найдена строка с заголовком 'п/п'");
            }

            System.out.println("🏷️  Заголовок таблицы найден в строке: " + headerRowIndex);

            while (table.getNumberOfRows() > headerRowIndex + 1) {
                table.removeRow(headerRowIndex + 1);
            }

            System.out.println("🧹 Старые данные удалены. Строк в таблице после очистки: " + table.getNumberOfRows());

            for (int i = 0; i < records.size(); i++) {
                TrainingRecord record = records.get(i);
                XWPFTableRow newRow = table.createRow();

                setCellTextWithTahoma(newRow, 0, String.valueOf(i + 1));
                setCellTextWithTahoma(newRow, 1, record.getEmployee().getFullName() + " <" + record.getEmployee().getEmail() + ">");
                setCellTextWithTahoma(newRow, 2, convertToDativeCase(record.getEmployee().getFullName()));
                setCellTextWithTahoma(newRow, 3, record.getEmployee().getPosition());
                setCellTextWithTahoma(newRow, 4, record.getTrainingDirection().getName());
                setCellTextWithTahoma(newRow, 5, record.getExamDate() != null ?
                        record.getExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "");
                setCellTextWithTahoma(newRow, 6, record.getTrainingDirection().getCost() != null ?
                        String.format("%,.2f", record.getTrainingDirection().getCost()) : "0.00");

                System.out.println("✅ Добавлена запись #" + (i+1) + ": " + record.getEmployee().getFullName() +
                        " -> " + convertToDativeCase(record.getEmployee().getFullName()));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            doc.close();

            System.out.println("✨ Документ успешно сгенерирован!");
            System.out.println("📝 Общее количество записей в таблице: " + (table.getNumberOfRows() - headerRowIndex - 1));

            return baos.toByteArray();
        }
    }

    /**
     * Используем тот же метод, что и в сервисе
     */
    private String convertToDativeCase(String fullName) {
        return RussianNameDecliner.declineToDative(fullName);
    }

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

    private void setCellTextWithTahoma(XWPFTableRow row, int cellIndex, String text) {
        try {
            XWPFTableCell cell;

            if (cellIndex < row.getTableCells().size()) {
                cell = row.getCell(cellIndex);
            } else {
                for (int i = row.getTableCells().size(); i <= cellIndex; i++) {
                    row.createCell();
                }
                cell = row.getCell(cellIndex);
            }

            while (cell.getParagraphs().size() > 0) {
                cell.removeParagraph(0);
            }

            XWPFParagraph paragraph = cell.addParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text != null ? text : "");
            run.setFontFamily("Tahoma");
            run.setFontSize(8);

            paragraph.setSpacingAfter(0);
            paragraph.setSpacingBefore(0);

        } catch (Exception e) {
            System.err.println("⚠️ Ошибка при заполнении ячейки " + cellIndex + ": " + e.getMessage());
        }
    }
}