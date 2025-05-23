package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.service.SemesterApprovalService;
import com.example.auth1.repository.StudentRepository;
import com.example.auth1.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/approvals")
public class SemesterApprovalController {

    @Autowired
    private SemesterApprovalService approvalService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    // Student endpoints
    @GetMapping("/request")
    public String requestApprovalForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String studentNumber = auth.getName();
        
        try {
            Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            String currentAcademicYear = getCurrentAcademicYear();
            List<SemesterApprovalRequest> studentRequests = approvalService.getStudentRequests(student.getId());
            
            model.addAttribute("student", student);
            model.addAttribute("currentAcademicYear", currentAcademicYear);
            model.addAttribute("requests", studentRequests);
            model.addAttribute("canRequest", approvalService.canRequestNextSemester(student.getId(), currentAcademicYear));
            
            return "student/request_approval";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/request")
    public Object submitApprovalRequest(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String studentNumber = auth.getName();
            Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            // If not provided, infer academicYear and semester
            if (academicYear == null) {
                academicYear = getCurrentAcademicYear();
            }
            if (semester == null) {
                semester = student.getCurrentSemester();
            }

            approvalService.createApprovalRequest(student.getId(), academicYear, semester);

            if (isAjax) {
                return ResponseEntity.ok().build();
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Approval request submitted successfully");
                return "redirect:/approvals/request";
            }
        } catch (Exception e) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/approvals/request";
            }
        }
    }

    // Admin endpoints
    @GetMapping("/pending")
    public String viewPendingRequests(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

            List<SemesterApprovalRequest> pendingRequests = approvalService.getPendingRequests();
            model.addAttribute("requests", pendingRequests);
            model.addAttribute("admin", admin);
            return "admin/pending_approvals";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{requestId}/process")
    public String processApprovalRequest(
            @PathVariable Long requestId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comments,
            RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

            approvalService.processApprovalRequest(requestId, approved, comments, admin.getId());
            
            String message = approved ? 
                "Request approved successfully" : 
                "Request rejected successfully";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/approvals/pending";
    }

    // Helper method to get current academic year
    private String getCurrentAcademicYear() {
        // Implementation depends on your academic year format
        // This is a simple example
        int currentYear = java.time.LocalDate.now().getYear();
        return currentYear + "-" + (currentYear + 1);
    }
} 