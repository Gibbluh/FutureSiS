package com.example.auth1.controller;

import org.springframework.stereotype.Controller;
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
        return "admin/admin_login"; // should point to templates/admin/admin_login.html
    }

    @GetMapping("/admin/home")
    public String adminHome() {
        return "admin/admin_home"; // should point to templates/admin/admin_home.html
    }

    // Student routes
    @GetMapping("/student/login")
    public String studentLogin() {
        return "student/student_login"; // should point to templates/student/student_login.html
    }

    @GetMapping("/student/home")
    public String studentHome() {
        return "student/student_home"; // should point to templates/student/student_home.html
    }
}