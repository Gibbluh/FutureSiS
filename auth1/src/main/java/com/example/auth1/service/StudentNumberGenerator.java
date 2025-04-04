// src/main/java/com/example/auth1/service/StudentNumberGenerator.java
package com.example.auth1.service;

import com.example.auth1.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class StudentNumberGenerator {
    private final StudentRepository studentRepository;
    
    public StudentNumberGenerator(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    
    public String generateNextStudentNumber() {
        int currentYear = Year.now().getValue();
        long nextId = studentRepository.count() + 1; // Simple approach
        
        return String.format("STU-%d-%05d", currentYear, nextId);
    }
}