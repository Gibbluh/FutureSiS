package com.example.auth1.service;

import com.example.auth1.model.Student;
import com.example.auth1.repository.StudentRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomStudentDetailsService implements UserDetailsService {
    private final StudentRepository studentRepository;
    
    public CustomStudentDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    
    @Override
    public UserDetails loadUserByUsername(String studentNumber) throws UsernameNotFoundException {
        System.out.println("Attempting to load student with student number: " + studentNumber);
        
        try {
            Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> {
                    System.out.println("Student not found with student number: " + studentNumber);
                    return new UsernameNotFoundException("Student not found with student number: " + studentNumber);
                });
            
            System.out.println("Found student: " + student);
            return new CustomStudentDetails(student);
        } catch (Exception e) {
            System.out.println("Error loading student: " + e.getMessage());
            throw new UsernameNotFoundException("Error loading student: " + e.getMessage(), e);
        }
    }
}