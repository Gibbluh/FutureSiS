package com.example.auth1.controller;

import com.example.auth1.model.Faculty;
import com.example.auth1.model.Program;
import com.example.auth1.repository.FacultyRepository;
import com.example.auth1.repository.ProgramRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin/faculty")
public class FacultyController {

    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final PasswordEncoder passwordEncoder;

    public FacultyController(FacultyRepository facultyRepository,
                           ProgramRepository programRepository,
                           PasswordEncoder passwordEncoder) {
        this.facultyRepository = facultyRepository;
        this.programRepository = programRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/add")
    public String addFaculty(@RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String facultyId,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam Long programId,
                           @RequestParam(required = false) String phoneNumber,
                           @RequestParam String position,
                           @RequestParam(required = false) String address,
                           RedirectAttributes redirectAttributes) {
        try {
            // Check if faculty ID already exists
            if (facultyRepository.existsByFacultyId(facultyId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Faculty ID already exists");
                return "redirect:/admin/home";
            }

            // Get the program
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));

            // Create new faculty
            Faculty faculty = new Faculty();
            faculty.setFirstName(firstName);
            faculty.setLastName(lastName);
            faculty.setFacultyId(facultyId);
            faculty.setEmail(email);
            faculty.setPassword(passwordEncoder.encode(password));
            faculty.setProgram(program);
            faculty.setPhoneNumber(phoneNumber);
            faculty.setPosition(position);
            faculty.setAddress(address);

            facultyRepository.save(faculty);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty member added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding faculty member: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    @GetMapping("/check-id/{facultyId}")
    @ResponseBody
    public Map<String, Boolean> checkFacultyIdUnique(@PathVariable String facultyId) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", facultyRepository.existsByFacultyId(facultyId));
        return response;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, String> deleteFaculty(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            facultyRepository.deleteById(id);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/edit/{id}")
    public String showEditFacultyForm(@PathVariable Long id, Model model) {
        Faculty faculty = facultyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Faculty not found"));
        List<Program> programs = programRepository.findAll();
        
        model.addAttribute("faculty", faculty);
        model.addAttribute("programs", programs);
        return "admin/edit_faculty";
    }

    @PostMapping("/update/{id}")
    public String updateFaculty(
            @PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam Long programId,
            @RequestParam String position,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {
        try {
            Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));

            faculty.setFirstName(firstName);
            faculty.setLastName(lastName);
            faculty.setEmail(email);
            faculty.setProgram(program);
            faculty.setPosition(position);
            faculty.setPhoneNumber(phoneNumber);
            faculty.setAddress(address);
            
            // Update password only if provided
            if (password != null && !password.isEmpty()) {
                faculty.setPassword(passwordEncoder.encode(password));
            }

            facultyRepository.save(faculty);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty member updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating faculty member: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }
} 