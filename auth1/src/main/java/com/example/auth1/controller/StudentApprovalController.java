package com.example.auth1.controller;

import com.example.auth1.model.SemesterApprovalRequest;
import com.example.auth1.model.Student;
import com.example.auth1.repository.StudentRepository;
import com.example.auth1.service.SemesterApprovalService;
import com.example.auth1.service.CustomStudentDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/student/approval")
public class StudentApprovalController {

    private final SemesterApprovalService semesterApprovalService;
    private final StudentRepository studentRepository;

    public StudentApprovalController(SemesterApprovalService semesterApprovalService,
                                     StudentRepository studentRepository) {
        this.semesterApprovalService = semesterApprovalService;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/request")
    public String requestApprovalForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String studentNumber = auth.getName();
        
        try {
            Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            String currentAcademicYear = getCurrentAcademicYear();
            List<SemesterApprovalRequest> studentRequests = semesterApprovalService.getStudentRequests(student.getId());
            
            model.addAttribute("student", student);
            model.addAttribute("currentAcademicYear", currentAcademicYear);
            model.addAttribute("requests", studentRequests);
            model.addAttribute("canRequest", semesterApprovalService.canRequestNextSemester(student.getId(), currentAcademicYear));
            boolean hasPendingRequest = studentRequests.stream()
                .anyMatch(r -> r.getStatus() == com.example.auth1.model.SemesterApprovalRequest.ApprovalStatus.PENDING);
            model.addAttribute("hasPendingRequest", hasPendingRequest);
            
            return "student/request_approval";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/request")
    @ResponseBody
    public ResponseEntity<Map<String, String>> submitApprovalRequest(@RequestBody Map<String, String> payload, Authentication authentication) {
        try {
            CustomStudentDetails studentDetails = (CustomStudentDetails) authentication.getPrincipal();
            // Fetch the latest student state from the database
            Student student = studentRepository.findById(studentDetails.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
            String comments = payload.get("comments");
            String academicYear = student.getAcademicYear();
            int currentSemester = student.getCurrentSemester();
            int currentYear = student.getEnrollmentYear();

            // Calculate the next semester and year
            int nextSemester = (currentSemester == 1) ? 2 : 1;
            int nextYear = (currentSemester == 1) ? currentYear : currentYear + 1;

            // Only allow up to 4th year, 2nd semester
            if (nextYear > 4 || (nextYear == 4 && nextSemester > 2)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Student has already completed all semesters.");
                return ResponseEntity.badRequest().body(error);
            }

            semesterApprovalService.createApprovalRequest(student.getId(), academicYear, nextSemester);
            Map<String, String> response = new HashMap<>();
            response.put("success", "Approval request submitted successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to submit request. Please try again or contact support.");
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String getCurrentAcademicYear() {
        int currentYear = java.time.LocalDate.now().getYear();
        return currentYear + "-" + (currentYear + 1);
    }
} 