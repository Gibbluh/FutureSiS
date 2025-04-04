package com.example.auth1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String home() {
        return "index"; // points to templates/index.html
    }

    // Admin routes
    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/admin_login";
    }

    @GetMapping("/admin/home")
    public String adminHome() {
        return "admin/admin_home";
    }

    // Enhanced Student routes
    @GetMapping("/student/login")
    public String studentLogin(Model model) {
        // Add current year to model for display in the login form
        model.addAttribute("currentYear", java.time.Year.now().getValue());
        return "student/student_login";
    }

    @GetMapping("/student/home")
    public String studentHome() {
        return "student/student_home";
    }

    // Additional common routes
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access_denied";
    }
}