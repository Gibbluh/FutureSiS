package com.example.auth1.controller;

import com.example.auth1.model.Student;
import com.example.auth1.repository.StudentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final StudentRepository studentRepository;

    public MainController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // Basic routes
    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access_denied";
    }

    // Student routes
    @GetMapping("/student/login")
    public String studentLogin() {
        return "student/student_login";
    }
}