package com.example.auth1.config;

import com.example.auth1.model.*;
import com.example.auth1.repository.AdminRepository;
import com.example.auth1.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DatabaseLoader implements CommandLineRunner {
    
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseLoader(AdminRepository adminRepository,
                        StudentRepository studentRepository,
                        PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (shouldLoadData()) {
            loadInitialData();
        }
    }

    private boolean shouldLoadData() {
        return adminRepository.count() == 0 && studentRepository.count() == 0;
    }

    private void loadInitialData() {
        // Create admin accounts
        List<Admin> admins = List.of(
            createAdmin("admin1@school.edu", "Admin123!", Role.ADMIN),
            createAdmin("admin2@school.edu", "Admin123!", Role.ADMIN)
        );
        adminRepository.saveAll(admins);

        // Create sample students
        List<Student> students = createSampleStudents(10);
        studentRepository.saveAll(students);

        System.out.println("Successfully loaded initial data:");
        System.out.println("- Created " + admins.size() + " admin accounts");
        System.out.println("- Created " + students.size() + " student accounts");
    }

    private Admin createAdmin(String email, String password, Role role) {
        return new Admin(
            email,
            passwordEncoder.encode(password),
            role
        );
    }

    private List<Student> createSampleStudents(int count) {
        int currentYear = LocalDate.now().getYear();
        
        return List.of(
            createStudent(currentYear, 1, LocalDate.of(2005, 5, 15), "Student123!"),
            createStudent(currentYear, 2, LocalDate.of(2005, 6, 20), "Student123!"),
            createStudent(currentYear, 3, LocalDate.of(2005, 7, 10), "Student123!"),
            createStudent(currentYear, 4, LocalDate.of(2005, 8, 5), "Student123!"),
            createStudent(currentYear, 5, LocalDate.of(2005, 9, 25), "Student123!"),
            createStudent(currentYear, 6, LocalDate.of(2005, 3, 12), "Student123!"),
            createStudent(currentYear, 7, LocalDate.of(2005, 4, 18), "Student123!"),
            createStudent(currentYear, 8, LocalDate.of(2005, 10, 8), "Student123!"),
            createStudent(currentYear, 9, LocalDate.of(2005, 11, 30), "Student123!"),
            createStudent(currentYear, 10, LocalDate.of(2005, 12, 22), "Student123!")
        );
    }

    private Student createStudent(int year, int sequence, LocalDate birthDate, String password) {
        Student student = new Student();
        student.setStudentNumber(String.format("STU-%d-%05d", year, sequence));
        student.setBirthDate(birthDate);
        student.setPassword(passwordEncoder.encode(password));
        student.setRole(Role.STUDENT);
        return student;
    }
}
