import com.company.training.entity.*;
import com.company.training.repository.UserRepository;
import com.company.training.service.EmployeeService;
import com.company.training.service.TrainingRequestService;
import com.company.training.service.TrainingService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingRequestServiceTest {

    @Mock
    private TrainingService trainingService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private TrainingRequestService trainingRequestService;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        // Устанавливаем clock на 1 января 2024 (понедельник)
        fixedClock = Clock.fixed(
                LocalDate.of(2024, 1, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );

        trainingRequestService.setClock(fixedClock);
    }

    @Test
    void testIsFirstWorkingDayOfMonth_WhenFirstMonday() {
        assertTrue(trainingRequestService.isFirstWorkingDayOfMonthPublic());
    }

    @Test
    void testIsFirstWorkingDayOfMonth_WhenFirstSaturday() {
        // Устанавливаем clock на 1 июня 2024 (суббота)
        fixedClock = Clock.fixed(
                LocalDate.of(2024, 6, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        trainingRequestService.setClock(fixedClock);

        // 1 июня 2024 - суббота, не первый рабочий день
        assertFalse(trainingRequestService.isFirstWorkingDayOfMonthPublic());
    }

    @Test
    void testGetRecordsWithThreeMonthPeriodStartingThisMonth() {
        // Создаем тестовые записи
        TrainingRecord record1 = createTestTrainingRecord(
                "Иванов Иван Иванович",
                "Охрана труда",
                12,
                LocalDate.of(2023, 10, 1)
        );

        TrainingRecord record2 = createTestTrainingRecord(
                "Петрова Анна Сергеевна",
                "Пожарная безопасность",
                12,
                LocalDate.of(2023, 4, 1)
        );

        List<TrainingRecord> allRecords = Arrays.asList(record1, record2);

        when(trainingService.getAllTrainingRecords()).thenReturn(allRecords);

        List<TrainingRecord> result = trainingRequestService
                .getRecordsWithThreeMonthPeriodStartingThisMonth();

        // record2: экзамен 01.04.2023 + 12 месяцев = 01.04.2024
        // 3 месяца до 01.04.2024 = 01.01.2024 (январь 2024)
        assertEquals(1, result.size(), "Должна быть только одна запись для января 2024");
        assertEquals("Петрова Анна Сергеевна", result.get(0).getEmployee().getFullName());
    }

    @Test
    void testCheckAndSendTrainingRequests_WhenFirstWorkingDayAndRecordsExist() throws Exception {
        // Настраиваем моки
        User admin = new User();
        admin.setEmail("admin@company.com");
        admin.setAdmin(true);
        admin.setEnabled(true);
        when(userRepository.findByAdminTrueAndEnabledTrue())
                .thenReturn(Collections.singletonList(admin));

        TrainingRecord record = createTestTrainingRecord(
                "Иванов Иван Иванович",
                "Охрана труда",
                12,
                LocalDate.of(2023, 4, 1)
        );

        when(trainingService.getAllTrainingRecords())
                .thenReturn(Collections.singletonList(record));

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        trainingRequestService.checkAndSendTrainingRequests();

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testCheckAndSendTrainingRequests_WhenNotFirstWorkingDay() {
        // Устанавливаем clock на 2 января 2024 (вторник) - не первый рабочий день
        fixedClock = Clock.fixed(
                LocalDate.of(2024, 1, 2)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        trainingRequestService.setClock(fixedClock);

        trainingRequestService.checkAndSendTrainingRequests();

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testCheckAndSendTrainingRequests_WhenNoAdmins() {
        when(userRepository.findByAdminTrueAndEnabledTrue())
                .thenReturn(Collections.emptyList());

        // Нам нужна заглушка для getAllTrainingRecords, чтобы метод продолжал выполнение
        TrainingRecord record = createTestTrainingRecord(
                "Иванов Иван Иванович",
                "Охрана труда",
                12,
                LocalDate.of(2023, 4, 1)
        );
        when(trainingService.getAllTrainingRecords())
                .thenReturn(Collections.singletonList(record));

        trainingRequestService.checkAndSendTrainingRequests();

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testGenerateTrainingRequestDocument() throws Exception {
        List<TrainingRecord> records = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            TrainingRecord record = createTestTrainingRecord(
                    "Сотрудник " + (i + 1),
                    "Направление " + (i + 1),
                    12,
                    LocalDate.of(2023, 4, 1)
            );
            records.add(record);
        }

        byte[] document = trainingRequestService.generateTrainingRequestDocument(records);

        assertNotNull(document);
        assertTrue(document.length > 0);
    }

    @Test
    void testSendTrainingRequestEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        List<String> adminEmails = Arrays.asList("admin1@company.com", "admin2@company.com");
        byte[] documentBytes = "test document".getBytes();

        List<TrainingRecord> records = Collections.singletonList(
                createTestTrainingRecord(
                        "Иванов Иван Иванович",
                        "Охрана труда",
                        12,
                        LocalDate.of(2023, 4, 1)
                )
        );

        trainingRequestService.sendTrainingRequestEmail(adminEmails, documentBytes, records);

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void testGetCurrentDatePublic() {
        LocalDate currentDate = trainingRequestService.getCurrentDatePublic();
        assertNotNull(currentDate);
        assertEquals(LocalDate.of(2024, 1, 1), currentDate);
    }

    @Test
    void testManualTriggerRequestSending() {
        assertDoesNotThrow(() -> trainingRequestService.manualTriggerRequestSending());
    }

    @Test
    void testDifferentValidityPeriods() {
        TrainingRecord record1 = createTestTrainingRecord(
                "Сотрудник 1", "Направление 1", 6, LocalDate.of(2023, 10, 1)
        );

        TrainingRecord record2 = createTestTrainingRecord(
                "Сотрудник 2", "Направление 2", 24, LocalDate.of(2022, 10, 1)
        );

        TrainingRecord record3 = createTestTrainingRecord(
                "Сотрудник 3", "Направление 3", 36, LocalDate.of(2021, 10, 1)
        );

        List<TrainingRecord> allRecords = Arrays.asList(record1, record2, record3);

        when(trainingService.getAllTrainingRecords()).thenReturn(allRecords);

        List<TrainingRecord> result = trainingRequestService
                .getRecordsWithThreeMonthPeriodStartingThisMonth();

        System.out.println("Найдено записей: " + result.size());
        for (TrainingRecord r : result) {
            System.out.println(r.getEmployee().getFullName() + " - " +
                    r.getExamDate() + " + " + r.getTrainingDirection().getValidityMonths() + " мес. = " +
                    r.getNextExamDate());
        }
    }

    private TrainingRecord createTestTrainingRecord(
            String employeeName,
            String directionName,
            int validityMonths,
            LocalDate examDate) {

        Employee employee = new Employee();
        employee.setFullName(employeeName);
        employee.setPosition("Тестовая должность");
        employee.setEmail("test@company.com");

        Department department = new Department();
        department.setName("Тестовый отдел");
        employee.setDepartment(department);

        TrainingDirection direction = new TrainingDirection();
        direction.setName(directionName);
        direction.setValidityMonths(validityMonths);
        direction.setCost(BigDecimal.valueOf(1000));

        TrainingRecord record = new TrainingRecord();
        record.setEmployee(employee);
        record.setTrainingDirection(direction);
        record.setExamDate(examDate);
        record.setApplicable(true);

        // Устанавливаем nextExamDate на основе examDate и validityMonths
        LocalDate nextExamDate = examDate.plusMonths(validityMonths);
        // Используем рефлексию для установки nextExamDate
        try {
            java.lang.reflect.Field nextExamDateField = TrainingRecord.class.getDeclaredField("nextExamDate");
            nextExamDateField.setAccessible(true);
            nextExamDateField.set(record, nextExamDate);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось установить nextExamDate", e);
        }

        return record;
    }
}