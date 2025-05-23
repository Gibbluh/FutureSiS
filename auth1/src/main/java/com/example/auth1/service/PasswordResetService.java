package com.example.auth1.service;

import com.example.auth1.model.Student;
import com.example.auth1.model.Faculty;
import com.example.auth1.repository.StudentRepository;
import com.example.auth1.repository.FacultyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class PasswordResetService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public boolean resetStudentPassword(String email) {
        return studentRepository.findByEmail(email)
                .map(student -> {
                    String newPassword = generateRandomPassword();
                    student.setPassword(passwordEncoder.encode(newPassword));
                    studentRepository.save(student);
                    emailService.sendPasswordResetEmail(email, newPassword);
                    return true;
                })
                .orElse(false);
    }

    public boolean resetFacultyPassword(String email) {
        return facultyRepository.findByEmail(email)
                .map(faculty -> {
                    String newPassword = generateRandomPassword();
                    faculty.setPassword(passwordEncoder.encode(newPassword));
                    facultyRepository.save(faculty);
                    emailService.sendPasswordResetEmail(email, newPassword);
                    return true;
                })
                .orElse(false);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
} 