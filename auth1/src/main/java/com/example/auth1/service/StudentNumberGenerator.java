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
        long count = studentRepository.count() + 1;
        return String.format("STU-%d-%05d", currentYear, count);
    }
}