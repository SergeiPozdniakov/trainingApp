package com.company.training.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParsingDebugService {

    private static final Logger logger = LoggerFactory.getLogger(ParsingDebugService.class);

    /**
     * Детальный анализ страницы для отладки
     */
    public void analyzePage(String pageText, int pageNumber) {
        logger.info("\n\n=== ДЕТАЛЬНЫЙ АНАЛИЗ СТРАНИЦЫ {} ===", pageNumber);

        // 1. Ищем все возможные ФИО
        List<String> foundNames = findPossibleNames(pageText);
        logger.info("Найдено возможных ФИО: {}", foundNames.size());
        for (int i = 0; i < Math.min(foundNames.size(), 5); i++) {
            logger.info("  ФИО {}: {}", i + 1, foundNames.get(i));
        }

        // 2. Ищем все даты
        List<String> foundDates = findDates(pageText);
        logger.info("Найдено дат: {}", foundDates.size());
        for (String date : foundDates) {
            logger.info("  Дата: {}", date);
        }

        // 3. Ищем номера протоколов
        List<String> foundNumbers = findProtocolNumbers(pageText);
        logger.info("Найдено номеров протоколов: {}", foundNumbers.size());
        for (String number : foundNumbers) {
            logger.info("  Номер: {}", number);
        }

        // 4. Ищем ключевые слова программ
        logProgramKeywords(pageText);

        // 5. Логирование извлеченных точных ФИО
        logExtractedExactNames(pageText);

        logger.info("=== КОНЕЦ АНАЛИЗА СТРАНИЦЫ {} ===\n", pageNumber);
    }

    /**
     * Логирование извлеченных точных ФИО из текста
     */
    public void logExtractedExactNames(String pageText) {
        logger.info("=== ИЗВЛЕЧЕНИЕ ТОЧНЫХ ФИО ===");

        // Паттерн для полного ФИО
        Pattern fullNamePattern = Pattern.compile("\\b[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\b");
        // Паттерн для Фамилия + Имя
        Pattern lastNameFirstNamePattern = Pattern.compile("\\b[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\b");

        List<String> fullNames = new ArrayList<>();
        Matcher fullNameMatcher = fullNamePattern.matcher(pageText);
        while (fullNameMatcher.find()) {
            fullNames.add(fullNameMatcher.group());
        }

        List<String> lastNameFirstNames = new ArrayList<>();
        Matcher lastNameFirstNameMatcher = lastNameFirstNamePattern.matcher(pageText);
        while (lastNameFirstNameMatcher.find()) {
            lastNameFirstNames.add(lastNameFirstNameMatcher.group());
        }

        logger.info("Найдено полных ФИО: {}", fullNames.size());
        for (String name : fullNames) {
            logger.info("  - {}", name);
        }

        logger.info("Найдено Фамилия+Имя: {}", lastNameFirstNames.size());
        for (String name : lastNameFirstNames) {
            logger.info("  - {}", name);
        }

        logger.info("=== КОНЕЦ ИЗВЛЕЧЕНИЯ ФИО ===");
    }

    /**
     * Поиск возможных ФИО в тексте
     */
    private List<String> findPossibleNames(String text) {
        List<String> names = new ArrayList<>();

        // Паттерн для ФИО (3 слова с заглавной буквы)
        Pattern namePattern = Pattern.compile("[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+");
        Matcher matcher = namePattern.matcher(text);

        while (matcher.find()) {
            names.add(matcher.group());
        }

        return names;
    }

    /**
     * Поиск дат в тексте
     */
    private List<String> findDates(String text) {
        List<String> dates = new ArrayList<>();
        Pattern datePattern = Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.\\d{4}");
        Matcher matcher = datePattern.matcher(text);

        while (matcher.find()) {
            dates.add(matcher.group());
        }

        return dates;
    }

    /**
     * Поиск номеров протоколов
     */
    private List<String> findProtocolNumbers(String text) {
        List<String> numbers = new ArrayList<>();
        Pattern numberPattern = Pattern.compile("\\b\\d{6,12}\\b");
        Matcher matcher = numberPattern.matcher(text);

        while (matcher.find()) {
            numbers.add(matcher.group());
        }

        return numbers;
    }

    /**
     * Логирование найденных ключевых слов программ
     */
    private void logProgramKeywords(String pageText) {
        String lowerText = pageText.toLowerCase();

        String[] programKeywords = {
                "электроустановк", "сосуд", "давлен", "огнев", "газоопасн",
                "первая помощь", "оказание первой", "средств индивидуальной защиты", "сиз",
                "вредных и опасных", "производственных факторов",
                "общие вопросы охраны труда", "системы управления охраной труда",
                "безопасные методы", "работ повышенной опасности"
        };

        logger.info("Ключевые слова программ на странице:");
        for (String keyword : programKeywords) {
            if (lowerText.contains(keyword)) {
                logger.info("  ✓ Найдено: '{}'", keyword);
            }
        }
    }

    /**
     * Проверка содержимого страницы
     */
    public void checkPageContent(String pageText, int pageNumber) {
        logger.info("=== ПРОВЕРКА СОДЕРЖИМОГО СТРАНИЦЫ {} ===", pageNumber);

        // Проверяем наличие обязательных элементов
        boolean hasName = hasName(pageText);
        boolean hasDate = hasDate(pageText);
        boolean hasProgram = hasProgramKeywords(pageText);

        logger.info("Есть ФИО: {}", hasName ? "ДА" : "НЕТ");
        logger.info("Есть дата: {}", hasDate ? "ДА" : "НЕТ");
        logger.info("Есть ключевые слова программы: {}", hasProgram ? "ДА" : "НЕТ");

        if (!hasName) {
            logger.warn("⚠️ На странице {} не найдено ФИО!", pageNumber);
        }
        if (!hasDate) {
            logger.warn("⚠️ На странице {} не найдено дат!", pageNumber);
        }
        if (!hasProgram) {
            logger.warn("⚠️ На странице {} не найдено ключевых слов программы!", pageNumber);
        }

        logger.info("=== КОНЕЦ ПРОВЕРКИ ===\n");
    }

    private boolean hasName(String text) {
        return text.matches(".*[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+.*");
    }

    private boolean hasDate(String text) {
        return text.matches(".*\\d{1,2}\\.\\d{1,2}\\.\\d{4}.*");
    }

    private boolean hasProgramKeywords(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("охрана труда") ||
                lowerText.contains("безопасные методы") ||
                lowerText.contains("программ");
    }
}