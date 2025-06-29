package com.example.auth1.controller;

import com.example.auth1.model.GraduationRequest;
import com.example.auth1.model.Student;
import com.example.auth1.model.Program;
import com.example.auth1.model.Section;
import com.example.auth1.repository.StudentRepository;
import com.example.auth1.repository.ProgramRepository;
import com.example.auth1.repository.SectionRepository;
import com.example.auth1.service.GraduationService;
import com.example.auth1.service.CustomStudentDetails;
import com.example.auth1.service.CustomAdminDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/graduation")
public class GraduationController {

    private final GraduationService graduationService;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;

    public GraduationController(GraduationService graduationService,
                               StudentRepository studentRepository,
                               ProgramRepository programRepository,
                               SectionRepository sectionRepository) {
        this.graduationService = graduationService;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.sectionRepository = sectionRepository;
    }

    // Student endpoints
    @PostMapping("/student/request")
    @ResponseBody
    public ResponseEntity<Map<String, String>> submitGraduationRequest(Authentication authentication) {
        try {
            CustomStudentDetails studentDetails = (CustomStudentDetails) authentication.getPrincipal();
            Student student = studentRepository.findById(studentDetails.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

            graduationService.createGraduationRequest(student.getId());
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "Graduation request submitted successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Admin endpoints
    @GetMapping("/admin/pending")
    public String showPendingGraduationRequests(
            @RequestParam(value = "programId", required = false) Long programId,
            @RequestParam(value = "yearLevel", required = false) Integer yearLevel,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            Model model) {
        
        List<GraduationRequest> pendingRequests = graduationService.getPendingRequests();
        
        // Filtering logic
        if (programId != null) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getStudent().getProgram() != null && r.getStudent().getProgram().getId().equals(programId))
                .collect(Collectors.toList());
        }
        if (yearLevel != null) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getStudent().getEnrollmentYear() != null && r.getStudent().getEnrollmentYear().equals(yearLevel))
                .collect(Collectors.toList());
        }
        if (sectionId != null) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getStudent().getSection() != null && r.getStudent().getSection().getId().equals(sectionId))
                .collect(Collectors.toList());
        }
        if (academicYear != null && !academicYear.isEmpty()) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getStudent().getAcademicYear() != null && r.getStudent().getAcademicYear().equals(academicYear))
                .collect(Collectors.toList());
        }
        
        // For filter dropdowns
        model.addAttribute("programs", programRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("academicYears", pendingRequests.stream()
            .map(r -> r.getStudent().getAcademicYear())
            .distinct()
            .collect(Collectors.toList()));
        model.addAttribute("yearLevels", List.of(1,2,3,4));
        model.addAttribute("selectedProgramId", programId);
        model.addAttribute("selectedYearLevel", yearLevel);
        model.addAttribute("selectedSectionId", sectionId);
        model.addAttribute("selectedAcademicYear", academicYear);
        model.addAttribute("pendingRequests", pendingRequests);
        
        return "admin/pending_graduation_requests";
    }

    @PostMapping("/admin/process")
    @ResponseBody
    public ResponseEntity<Map<String, String>> processGraduationRequest(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        try {
            Long requestId = Long.parseLong(payload.get("requestId").toString());
            boolean approved = Boolean.parseBoolean(payload.get("approved").toString());
            String comments = (String) payload.get("comments");

            graduationService.processGraduationRequest(requestId, approved, comments);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "Graduation request processed successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 