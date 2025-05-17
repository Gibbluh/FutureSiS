package com.example.auth1.controller;

import com.example.auth1.model.Faculty;
import com.example.auth1.repository.FacultyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/faculty")
public class FacultyHomeController {

    private final FacultyRepository facultyRepository;

    public FacultyHomeController(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "faculty/faculty_login";
    }

    @GetMapping("/home")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String facultyId = auth.getName();
        
        Faculty faculty = facultyRepository.findByFacultyId(facultyId)
            .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        model.addAttribute("faculty", faculty);
        return "faculty/faculty_home";
    }
} 