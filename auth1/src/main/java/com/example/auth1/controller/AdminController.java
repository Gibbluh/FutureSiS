package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import com.example.auth1.service.StudentNumberGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StudentRepository studentRepository;
    private final StudentNumberGenerator studentNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ProgramRepository programRepository;

    public AdminController(StudentRepository studentRepository,
                        StudentNumberGenerator studentNumberGenerator,
                        PasswordEncoder passwordEncoder,
                        ProgramRepository programRepository) {
        this.studentRepository = studentRepository;
        this.studentNumberGenerator = studentNumberGenerator;
        this.passwordEncoder = passwordEncoder;
        this.programRepository = programRepository;
    }

    @GetMapping("/login")
    public String adminLogin() {
        return "admin/admin_login";
    }

    @GetMapping("/home")
    public String adminHome(Model model) {
        org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("programs", programRepository.findAll());
        
        // Add enrollment years for the dropdown (current year + 5 years)
        int currentYear = LocalDate.now().getYear();
        List<Integer> enrollmentYears = IntStream.rangeClosed(currentYear - 4, currentYear + 5)
            .boxed().collect(Collectors.toList());
        model.addAttribute("enrollmentYears", enrollmentYears);
        
        return "admin/admin_home"; // Fixed the path to include the folder
    }

    // Student Management
    @PostMapping("/students/add")
    public String addStudent(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String birthDate,
            @RequestParam String password,
            @RequestParam Long programId,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam Integer enrollmentYear,
            RedirectAttributes redirectAttributes) {
        
        try {
            Student student = new Student();
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setBirthDate(java.time.LocalDate.parse(birthDate));
            student.setPassword(passwordEncoder.encode(password));
            student.setStudentNumber(studentNumberGenerator.generateNextStudentNumber());
            
            // Set new fields
            student.setPhoneNumber(phoneNumber);
            student.setAddress(address);
            
            // Set enrollment year for proper year level display
            student.setEnrollmentYear(enrollmentYear);
            
            // Enrollment date is set automatically by the Student model's default value
            
            // Set program
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            student.setProgram(program);
            
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding student: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    @GetMapping("/students/edit/{id}")
    public String showEditStudentForm(@PathVariable Long id, Model model) {
        try {
            Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            model.addAttribute("student", student);
            model.addAttribute("programs", programRepository.findAll());
            
            // Add enrollment years for the dropdown
            int currentYear = LocalDate.now().getYear();
            List<Integer> enrollmentYears = IntStream.rangeClosed(currentYear - 10, currentYear + 5)
                .boxed().collect(Collectors.toList());
            model.addAttribute("enrollmentYears", enrollmentYears);
            
            return "admin/edit_student";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading student: " + e.getMessage());
            return "redirect:/admin/home";
        }
    }

    @PostMapping("/students/edit/{id}")
    public String updateStudent(
            @PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String birthDate,
            @RequestParam(required = false) String password,
            @RequestParam Long programId,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam Integer enrollmentYear,
            @RequestParam String enrollmentDate,
            @RequestParam Integer currentSemester,
            RedirectAttributes redirectAttributes) {
        
        try {
            Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setBirthDate(java.time.LocalDate.parse(birthDate));
            
            // Update password only if provided
            if (password != null && !password.isEmpty()) {
                student.setPassword(passwordEncoder.encode(password));
            }
            
            // Update new fields
            student.setPhoneNumber(phoneNumber);
            student.setAddress(address);
            
            // Update enrollment year for proper year level display
            student.setEnrollmentYear(enrollmentYear);
            
            // Update current semester
            student.setCurrentSemester(currentSemester);
            
            // Update enrollment date from the form input
            student.setEnrollmentDate(LocalDate.parse(enrollmentDate));
            
            // Update program
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            student.setProgram(program);
            
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating student: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting student: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }
}