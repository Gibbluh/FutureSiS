package com.example.auth1.config;

import com.example.auth1.model.Admin;
import com.example.auth1.model.Student;
import com.example.auth1.repository.AdminRepository;
import com.example.auth1.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DatabaseLoader {
    
    @Bean
    public CommandLineRunner loadData(AdminRepository adminRepository, 
                                    StudentRepository studentRepository,
                                    PasswordEncoder passwordEncoder) {
        return args -> {
            // Create admin accounts
            Admin admin1 = new Admin();
            admin1.setEmail("admin1@example.com");
            admin1.setPassword(passwordEncoder.encode("admin123"));
            
            Admin admin2 = new Admin();
            admin2.setEmail("admin2@example.com");
            admin2.setPassword(passwordEncoder.encode("admin456"));
            
            adminRepository.saveAll(List.of(admin1, admin2));
            
            // Create student accounts with birth dates
            Student student1 = new Student();
            student1.setStudentNumber("STU001");
            student1.setBirthDate(LocalDate.of(2000, 5, 15));
            student1.setPassword(passwordEncoder.encode("student123"));
            
            Student student2 = new Student();
            student2.setStudentNumber("STU002");
            student2.setBirthDate(LocalDate.of(2001, 8, 22));
            student2.setPassword(passwordEncoder.encode("student456"));
            
            studentRepository.saveAll(List.of(student1, student2));
            
            System.out.println("Sample admin and student accounts created!");
        };
    }
}
