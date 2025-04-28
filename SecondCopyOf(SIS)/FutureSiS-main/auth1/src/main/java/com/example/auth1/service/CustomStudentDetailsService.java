package com.example.auth1.service;

import com.example.auth1.model.Student;
import com.example.auth1.repository.StudentRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;

@Service
public class CustomStudentDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;

    public CustomStudentDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Get birthdate from request parameters
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new UsernameNotFoundException("Request attributes are not available");
        }
        String birthDate = attributes.getRequest().getParameter("birthDate");

        Student student = studentRepository.findByStudentNumber(username)
                .orElseThrow(() -> new UsernameNotFoundException("Student not found"));

        // Verify birthdate matches
        if (!student.getBirthDate().equals(LocalDate.parse(birthDate))) {
            throw new UsernameNotFoundException("Birth date does not match");
        }

        return User.withUsername(student.getStudentNumber())
                .password(student.getPassword())
                .roles("STUDENT")
                .build();
    }
}