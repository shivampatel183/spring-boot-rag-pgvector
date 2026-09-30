package com.example.BotAI.Services;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.BotAI.Config.VectorConfig;
import com.example.BotAI.DTOs.ConfirmationResponse;
import com.example.BotAI.DTOs.StudentDto;
import com.example.BotAI.Entity.StudentEntity;
import com.example.BotAI.Repository.StudentRepository;

@Service
@Transactional
public class StudentServices {

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    VectorConfig vectorConfig;

    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();

    public List<StudentEntity> getAllStudents() {
        return studentRepository.findAll();
    }

    public StudentEntity getStudentById(long id) {
        return studentRepository.findById(id).orElse(null);
    }

    public StudentEntity createStudent(StudentDto student) {
        StudentEntity entity = new StudentEntity();
        entity.setName(student.getName());
        entity.setSchool(student.getSchool());
        entity.setEmail(student.getEmail());
        entity.setPhone(student.getPhone());
        entity.setDescription(student.getDescription());

        StudentEntity saved = studentRepository.save(entity);
        vectorConfig.addStudentVector(saved);
        return saved;
    }

    public StudentEntity updateStudent(long id, StudentDto student) {
        StudentEntity entity = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found: " + id));
        entity.setName(student.getName());
        entity.setSchool(student.getSchool());
        entity.setEmail(student.getEmail());
        entity.setPhone(student.getPhone());
        entity.setDescription(student.getDescription());

        StudentEntity saved = studentRepository.save(entity);
        vectorConfig.updateStudentVector(saved);
        return saved;
    }

    public String deleteStudent(long id) {
        StudentEntity entity = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found: " + id));
        vectorConfig.deleteStudentVector(entity);
        studentRepository.delete(entity);
        return "Student deleted successfully";
    }

    public Object askAI(String question) {
        return askAI(question, null, false);
    }

    public Object askAI(String question, String actionId, boolean confirmed) {
        if (question == null || question.isBlank()) {
            return "Please provide a valid question.";
        }

        if (confirmed && actionId != null) {
            PendingAction pendingAction = pendingActions.remove(actionId);
            if (pendingAction == null) {
                return "This confirmation is invalid or expired.";
            }
            return executePendingAction(pendingAction);
        }

        PendingAction pendingAction = parsePendingAction(question);
        if (pendingAction == null) {
            return vectorConfig.askAI(question);
        }

        String newActionId = UUID.randomUUID().toString();
        pendingActions.put(newActionId, pendingAction);

        String confirmUrl = "/api/askai/confirm?actionId=" + newActionId + "&confirm=true";
        String cancelUrl = "/api/askai/confirm?actionId=" + newActionId + "&confirm=false";

        return new ConfirmationResponse(
                "I can perform this action only after your confirmation. " + pendingAction.description(),
                newActionId,
                confirmUrl,
                cancelUrl
        );
    }

    public String confirmAction(String actionId, boolean confirm) {
        if (actionId == null || actionId.isBlank()) {
            return "Action ID is required.";
        }
        if (!confirm) {
            pendingActions.remove(actionId);
            return "Action cancelled.";
        }
        PendingAction pendingAction = pendingActions.remove(actionId);
        if (pendingAction == null) {
            return "No pending action found for this action ID.";
        }
        return executePendingAction(pendingAction);
    }

    private String executePendingAction(PendingAction pendingAction) {
        return switch (pendingAction.type()) {
            case CREATE -> {
                StudentDto dto = new StudentDto();
                dto.setName(pendingAction.name());
                dto.setSchool(pendingAction.school());
                dto.setEmail(pendingAction.email());
                dto.setPhone(pendingAction.phone());
                dto.setDescription(pendingAction.description());
                StudentEntity saved = createStudent(dto);
                yield "Student created successfully: " + saved.getName();
            }
            case UPDATE -> {
                StudentDto dto = new StudentDto();
                dto.setName(pendingAction.name());
                dto.setSchool(pendingAction.school());
                dto.setEmail(pendingAction.email());
                dto.setPhone(pendingAction.phone());
                dto.setDescription(pendingAction.description());
                StudentEntity saved = updateStudent(pendingAction.id(), dto);
                yield "Student updated successfully: " + saved.getName();
            }
            case DELETE -> {
                String result = deleteStudent(pendingAction.id());
                yield result;
            }
        };
    }

    private PendingAction parsePendingAction(String question) {

        String normalized = question.trim();
        String lower = normalized.toLowerCase();

        if (lower.contains("create student")) {
            String rest = normalized.replaceFirst("(?i).*?create student", "").trim();
            String name = extractValue(rest, "name");
            String school = extractValue(rest, "school");
            String email = extractValue(rest, "email");
            String phone = extractValue(rest, "phone");
            String description = extractValue(rest, "description");

            if (name == null || name.isBlank()) {
                return null;
            }
            return new PendingAction(ActionType.CREATE, 0L, name, school, email, phone, description);
        }

        if (lower.contains("update student")) {
            String rest = normalized.replaceFirst("(?i).*?update student", "").trim();
            Long id = extractId(rest);
            String name = extractValue(rest, "name");
            String school = extractValue(rest, "school");
            String email = extractValue(rest, "email");
            String phone = extractValue(rest, "phone");
            String description = extractValue(rest, "description");

            if (id == null) {
                return null;
            }
            return new PendingAction(ActionType.UPDATE, id, name, school, email, phone, description);
        }

        if (lower.contains("delete student")) {
            String rest = normalized.replaceFirst("(?i).*?delete student", "").trim();
            Long id = extractId(rest);
            if (id == null) {
                return null;
            }
            return new PendingAction(ActionType.DELETE, id, null, null, null, null, null);
        }

        return null;
    }

    private Long extractId(String text) {
        Matcher matcher = Pattern.compile("(?i)\\b(?:id|student id)\\s*[:=]?\\s*(\\d+)").matcher(text);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        Matcher matchername = Pattern.compile("(?i)\\b(?:name|student name)\\s*[:=]?\\s*(\\d+)").matcher(text);
        if (matchername.find()) {
            StudentEntity student = studentRepository.findByName(matchername.group(1));
            if (student != null) {
                return student.getId();
            }
        }

        return null;
    }

    private String extractValue(String text, String fieldName) {
        Matcher matcher = Pattern.compile("(?i)\\b" + fieldName + "\\s*[:=]?\\s*([^,;]+)").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private record PendingAction(ActionType type, Long id, String name, String school, String email, String phone,
                                String description) {
        public String description() {
            return switch (type) {
                case CREATE -> "Create student with name='" + name + "', school='" + school + "', email='" + email + "', phone='" + phone + "', description='" + description + "'";
                case UPDATE -> "Update student id=" + id + " with name='" + name + "', school='" + school + "', email='" + email + "', phone='" + phone + "', description='" + description + "'";
                case DELETE -> "Delete student with id=" + id;
            };
        }
    }

    private enum ActionType {
        CREATE,
        UPDATE,
        DELETE
    }
}
