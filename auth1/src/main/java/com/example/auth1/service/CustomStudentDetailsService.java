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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Enumeration;

@Service
public class CustomStudentDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomStudentDetailsService.class);

    private final StudentRepository studentRepository;

    public CustomStudentDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            logger.info("=== Starting student authentication process ===");
            logger.info("Received student number for authentication: {}", username);
            
            // Get request attributes and log all parameters
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                logger.error("Request attributes are not available");
                throw new UsernameNotFoundException("Request attributes are not available");
            }

            HttpServletRequest request = attributes.getRequest();
            logger.info("Request method: {}", request.getMethod());
            logger.info("Request URI: {}", request.getRequestURI());
            
            logger.info("=== Request Parameters ===");
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (!paramName.toLowerCase().contains("password")) {
                    logger.info("Parameter: {} = {}", paramName, request.getParameter(paramName));
                }
            }
            
            String birthDate = request.getParameter("birthDate");
            logger.info("Birth date from request: {}", birthDate);

            if (birthDate == null || birthDate.trim().isEmpty()) {
                logger.error("Birth date is required but was not provided");
                throw new UsernameNotFoundException("Birth date is required");
            }

            // Try to find the student
            logger.info("Searching for student with student number: {}", username);
            Student student = studentRepository.findByStudentNumber(username)
                    .orElseThrow(() -> {
                        logger.error("Student not found with student number: {}", username);
                        return new UsernameNotFoundException("Student not found");
                    });
            
            logger.info("Found student: {} {} (ID: {})", 
                student.getFirstName(), 
                student.getLastName(),
                student.getId());
            logger.info("Stored birth date: {}", student.getBirthDate());
            logger.info("Provided birth date: {}", birthDate);

            // Verify birthdate matches
            LocalDate providedBirthDate = LocalDate.parse(birthDate);
            if (!student.getBirthDate().equals(providedBirthDate)) {
                logger.error("Birth date mismatch for student {}", username);
                logger.error("Expected: {}", student.getBirthDate());
                logger.error("Received: {}", providedBirthDate);
                throw new UsernameNotFoundException("Birth date does not match");
            }

            logger.info("Birth date verified successfully");
            logger.info("Creating UserDetails for student {}", username);
            
            // Return custom user details
            UserDetails userDetails = new CustomStudentDetails(student);
            
            logger.info("Student {} successfully authenticated", username);
            logger.info("=== Authentication process completed ===");
            
            return userDetails;
        } catch (Exception e) {
            logger.error("=== Authentication Error ===");
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            logger.error("Stack trace:", e);
            throw new UsernameNotFoundException("Authentication failed: " + e.getMessage());
        }
    }
}