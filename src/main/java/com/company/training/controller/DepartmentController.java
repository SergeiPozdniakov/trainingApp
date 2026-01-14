package com.company.training.controller;

import com.company.training.entity.Department;
import com.company.training.entity.Employee;
import com.company.training.entity.TrainingDirection;
import com.company.training.entity.TrainingRecord;
import com.company.training.service.DepartmentService;
import com.company.training.service.EmployeeService;
import com.company.training.service.TrainingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentController.class);

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TrainingService trainingService;

    @GetMapping("/{id}")
    public String viewDepartment(@PathVariable Long id, Model model) {
        try {
            Department department = departmentService.getDepartmentById(id);
            List<Employee> employees = employeeService.getEmployeesByDepartment(id);
            List<TrainingDirection> directions = trainingService.getAllTrainingDirections();

            // Загружаем записи об обучении для каждого сотрудника
            Map<Long, Map<Long, TrainingRecord>> trainingRecordsMap = new HashMap<>();
            for (Employee employee : employees) {
                Map<Long, TrainingRecord> employeeRecords = new HashMap<>();
                for (TrainingDirection direction : directions) {
                    TrainingRecord record = trainingService.getTrainingRecord(employee.getId(), direction.getId());
                    if (record != null) {
                        employeeRecords.put(direction.getId(), record);
                    }
                }
                trainingRecordsMap.put(employee.getId(), employeeRecords);
            }

            model.addAttribute("department", department);
            model.addAttribute("employees", employees);
            model.addAttribute("directions", directions);
            model.addAttribute("employee", new Employee());
            model.addAttribute("trainingRecordsMap", trainingRecordsMap); // ← добавляем карту записей

            return "department/view";
        } catch (Exception e) {
            logger.error("Ошибка при загрузке отдела: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке отдела: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/{departmentId}/employees")
    public String addEmployee(@PathVariable Long departmentId,
                              @Valid @ModelAttribute("employee") Employee employee,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации данных сотрудника");
            return "redirect:/departments/" + departmentId;
        }

        try {
            employee.setDepartment(departmentService.getDepartmentById(departmentId));
            employeeService.createEmployee(employee);
            redirectAttributes.addFlashAttribute("success", "Сотрудник успешно добавлен");
        } catch (Exception e) {
            logger.error("Ошибка при добавлении сотрудника: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при добавлении сотрудника: " + e.getMessage());
        }

        return "redirect:/departments/" + departmentId;
    }

    @PostMapping("/{departmentId}/employees/{employeeId}/delete")
    public String deleteEmployee(@PathVariable Long departmentId,
                                 @PathVariable Long employeeId,
                                 RedirectAttributes redirectAttributes) {

        try {
            employeeService.deleteEmployee(employeeId);
            redirectAttributes.addFlashAttribute("success", "Сотрудник успешно удален");
        } catch (Exception e) {
            logger.error("Ошибка при удалении сотрудника: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении сотрудника: " + e.getMessage());
        }

        return "redirect:/departments/" + departmentId;
    }

    @PostMapping("/{departmentId}/training-records")
    public String updateTrainingRecord(@PathVariable Long departmentId,
                                       @RequestParam Long employeeId,
                                       @RequestParam Long directionId,
                                       @RequestParam(required = false) LocalDate examDate,
                                       @RequestParam(required = false) String protocolNumber,
                                       @RequestParam(required = false) Boolean applicable,
                                       @RequestParam(required = false) MultipartFile protocolFile,
                                       RedirectAttributes redirectAttributes) {

        try {
            logger.info("Сохранение записи: employeeId={}, directionId={}, examDate={}, protocolNumber={}, applicable={}, file={}",
                    employeeId, directionId, examDate, protocolNumber, applicable,
                    protocolFile != null ? protocolFile.getOriginalFilename() : "null");

            // Получаем существующую запись или создаем новую
            TrainingRecord record = trainingService.getTrainingRecord(employeeId, directionId);

            if (record == null) {
                logger.info("Запись не найдена, создаем новую");
                record = new TrainingRecord();
                record.setEmployee(employeeService.getEmployeeById(employeeId));
                record.setTrainingDirection(trainingService.getTrainingDirectionById(directionId));
            }

            // Устанавливаем значения
            record.setExamDate(examDate);
            record.setProtocolNumber(protocolNumber);
            record.setApplicable(applicable);

            // Сохраняем файл, если он есть
            if (protocolFile != null && !protocolFile.isEmpty()) {
                logger.info("Загружаем файл: {}", protocolFile.getOriginalFilename());
                trainingService.saveTrainingRecord(record, protocolFile);
            } else {
                // Если файл не загружается, но запись уже есть - сохраняем без изменения файла
                logger.info("Файл не загружен, сохраняем без изменения файла");
                trainingService.saveTrainingRecordWithoutFile(record);
            }

            redirectAttributes.addFlashAttribute("success", "Данные сохранены успешно");

        } catch (Exception e) {
            logger.error("Ошибка при сохранении записи: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при сохранении: " + e.getMessage());
        }

        return "redirect:/departments/" + departmentId;
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
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
                throw new RuntimeException("Файл не найден: " + fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{departmentId}/employees/{employeeId}/edit")
    public String showEditEmployeeForm(@PathVariable Long departmentId,
                                       @PathVariable Long employeeId,
                                       Model model) {
        try {
            Department department = departmentService.getDepartmentById(departmentId);
            Employee employee = employeeService.getEmployeeById(employeeId);

            model.addAttribute("department", department);
            model.addAttribute("employee", employee);

            return "department/edit-employee";
        } catch (Exception e) {
            logger.error("Ошибка при загрузке формы редактирования: {}", e.getMessage(), e);
            return "redirect:/departments/" + departmentId + "?error=Ошибка при загрузке данных сотрудника";
        }
    }

    @PostMapping("/{departmentId}/employees/{employeeId}/update")
    public String updateEmployee(@PathVariable Long departmentId,
                                 @PathVariable Long employeeId,
                                 @Valid @ModelAttribute("employee") Employee employee,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации данных сотрудника");
            return "redirect:/departments/" + departmentId + "/employees/" + employeeId + "/edit";
        }

        try {
            // Получаем существующего сотрудника
            Employee existingEmployee = employeeService.getEmployeeById(employeeId);

            // Обновляем только необходимые поля
            existingEmployee.setFullName(employee.getFullName());
            existingEmployee.setPosition(employee.getPosition());
            existingEmployee.setEmail(employee.getEmail());

            employeeService.updateEmployee(existingEmployee);

            redirectAttributes.addFlashAttribute("success", "Данные сотрудника обновлены");
            return "redirect:/departments/" + departmentId;

        } catch (Exception e) {
            logger.error("Ошибка при обновлении сотрудника: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении сотрудника: " + e.getMessage());
            return "redirect:/departments/" + departmentId + "/employees/" + employeeId + "/edit";
        }
    }


}