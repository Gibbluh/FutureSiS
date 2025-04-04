package com.example.auth1.service;

import com.example.auth1.model.Student;
import com.example.auth1.repository.StudentRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomStudentDetailsService implements UserDetailsService {
    private final StudentRepository studentRepository;
    
    public CustomStudentDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Attempting to load student with identifier: " + username);
        
        // 1. First try exact match
        Optional<Student> student = studentRepository.findByStudentNumber(username);
        if (student.isPresent()) {
            System.out.println("Found student by exact student number: " + student.get());
            return new CustomStudentDetails(student.get());
        }
        
        // 2. Try flexible matching if exact match not found
        System.out.println("Exact match not found, trying flexible matching...");
        String cleanInput = username.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        List<Student> possibleMatches = studentRepository.findByStudentNumberContainingIgnoreCase(cleanInput);
        
        if (!possibleMatches.isEmpty()) {
            // Get the first match (you might want to add more sophisticated matching logic)
            Student matchedStudent = possibleMatches.get(0);
            System.out.println("Found student by flexible matching: " + matchedStudent);
            return new CustomStudentDetails(matchedStudent);
        }
        
        // 3. If no matches found
        System.out.println("Student not found with identifier: " + username);
        throw new UsernameNotFoundException("Student not found with identifier: " + username);
    }
}