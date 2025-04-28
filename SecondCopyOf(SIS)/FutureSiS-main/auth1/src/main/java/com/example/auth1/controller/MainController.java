package com.example.auth1.controller;

import com.example.auth1.model.Student;
import com.example.auth1.model.Program;
import com.example.auth1.repository.StudentRepository;
import com.example.auth1.repository.ProgramRepository;
import com.example.auth1.service.StudentNumberGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MainController {

    private final StudentRepository studentRepository;
    private final StudentNumberGenerator studentNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ProgramRepository programRepository;

    public MainController(StudentRepository studentRepository,
                        StudentNumberGenerator studentNumberGenerator,
                        PasswordEncoder passwordEncoder,
                        ProgramRepository programRepository) {
        this.studentRepository = studentRepository;
        this.studentNumberGenerator = studentNumberGenerator;
        this.passwordEncoder = passwordEncoder;
        this.programRepository = programRepository;
    }

    // Basic routes
    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access_denied";
    }

    // Admin routes
    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/admin_login";
    }

    @GetMapping("/admin/home")
    public String adminHome(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("programs", programRepository.findAll());
        return "admin/admin_home";
    }

    @GetMapping("/admin/programs")
    public String managePrograms(Model model) {
        model.addAttribute("programs", programRepository.findAll());
        return "admin/manage_programs";
    }

    @GetMapping("/admin/programs/add")
    public String showAddProgramPage(Model model) {
        model.addAttribute("program", new Program());
        return "admin/add_program";
    }

    @PostMapping("/admin/programs/add")
    public String addProgram(@RequestParam String name, RedirectAttributes redirectAttributes) {
        try {
            Program program = new Program(name);
            programRepository.save(program);
            redirectAttributes.addFlashAttribute("successMessage", "Program added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding program: " + e.getMessage());
        }
        return "redirect:/admin/programs/add";
    }

    @PostMapping("/admin/programs/delete/{id}")
    public String deleteProgram(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            programRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Program deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/students/add")
    public String addStudent(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String birthDate,
            @RequestParam String password,
            @RequestParam Long programId,
            RedirectAttributes redirectAttributes) {
        
        try {
            Student student = new Student();
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setBirthDate(java.time.LocalDate.parse(birthDate));
            student.setPassword(passwordEncoder.encode(password));
            student.setStudentNumber(studentNumberGenerator.generateNextStudentNumber());
            student.setProgram(programRepository.findById(programId).orElseThrow(() -> new RuntimeException("Program not found")));
            
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding student: " + e.getMessage());
        }
        
        return "redirect:/admin/home";
    }

    @PostMapping("/admin/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting student: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    // Student routes
    @GetMapping("/student/login")
    public String studentLogin() {
        return "student/student_login";
    }

    @GetMapping("/student/home")
    public String studentHome(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String studentNumber = auth.getName();
        
        Student student = studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        
        model.addAttribute("student", student);
        return "student/student_home";
    }
}