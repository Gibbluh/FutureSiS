package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin/faculty")
public class FacultyController {

    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyProgramRepository facultyProgramRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(FacultyController.class);

    public FacultyController(FacultyRepository facultyRepository,
                           ProgramRepository programRepository,
                           SubjectRepository subjectRepository,
                           FacultyProgramRepository facultyProgramRepository,
                           TeachingAssignmentRepository teachingAssignmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.facultyRepository = facultyRepository;
        this.programRepository = programRepository;
        this.subjectRepository = subjectRepository;
        this.facultyProgramRepository = facultyProgramRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/add")
    public String addFaculty(@RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String facultyId,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam List<Long> programIds,
                           @RequestParam(required = false) List<Long> subjectIds,
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

            // Create new faculty
            Faculty faculty = new Faculty();
            faculty.setFirstName(firstName);
            faculty.setLastName(lastName);
            faculty.setFacultyId(facultyId);
            faculty.setEmail(email);
            faculty.setPassword(passwordEncoder.encode(password));
            faculty.setPhoneNumber(phoneNumber);
            faculty.setPosition(position);
            faculty.setAddress(address);
            faculty.setRole(Role.FACULTY);

            // Save faculty first to get the ID
            faculty = facultyRepository.save(faculty);

            // Add programs
            for (Long programId : programIds) {
                Program program = programRepository.findById(programId)
                    .orElseThrow(() -> new RuntimeException("Program not found: " + programId));
                FacultyProgram facultyProgram = new FacultyProgram(faculty, program);
                facultyProgramRepository.save(facultyProgram);
            }

            // Add subject assignments if provided
            if (subjectIds != null && !subjectIds.isEmpty()) {
                String currentAcademicYear = getCurrentAcademicYear();
                Integer currentSemester = getCurrentSemester();

                for (Long subjectId : subjectIds) {
                    Subject subject = subjectRepository.findById(subjectId)
                        .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectId));
                    TeachingAssignment assignment = new TeachingAssignment(faculty, subject, currentAcademicYear, currentSemester);
                    teachingAssignmentRepository.save(assignment);
                }
            }

            // Save faculty again with all relationships
            facultyRepository.save(faculty);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty member added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding faculty member: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    @GetMapping("/subjects/by-programs")
    @ResponseBody
    public ResponseEntity<?> getSubjectsByPrograms(@RequestParam List<Long> programIds) {
        try {
            logger.info("Fetching subjects for programs: {}", programIds);
            
            // Log the programs being queried
            programIds.forEach(programId -> {
                Program program = programRepository.findById(programId)
                    .orElse(null);
                if (program != null) {
                    logger.info("Found program: {} (ID: {})", program.getName(), program.getId());
                } else {
                    logger.warn("Program not found for ID: {}", programId);
                }
            });
            
            List<Subject> subjects = subjectRepository.findByProgramIds(programIds);
            logger.info("Found {} subjects", subjects.size());
            
            // Log each subject for debugging
            subjects.forEach(subject -> {
                logger.debug("Subject: {} - {} (Program: {})", 
                    subject.getCode(), 
                    subject.getName(),
                    subject.getCourse().getProgram().getName());
            });
            
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            logger.error("Error fetching subjects for programs {}: {}", programIds, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching subjects: " + e.getMessage()));
        }
    }

    private String getCurrentAcademicYear() {
        // Implement logic to determine current academic year
        // For example: 2023-2024
        return "2023-2024";
    }

    private Integer getCurrentSemester() {
        // Implement logic to determine current semester
        // For example: 1 for first semester, 2 for second semester
        return 1;
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
            @RequestParam List<Long> programIds,
            @RequestParam String position,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {
        try {
            Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

            faculty.setFirstName(firstName);
            faculty.setLastName(lastName);
            faculty.setEmail(email);
            faculty.setPosition(position);
            faculty.setPhoneNumber(phoneNumber);
            faculty.setAddress(address);
            
            // Update password only if provided
            if (password != null && !password.isEmpty()) {
                faculty.setPassword(passwordEncoder.encode(password));
            }

            // Clear existing programs and add new ones
            faculty.getFacultyPrograms().clear();
            for (Long programId : programIds) {
                Program program = programRepository.findById(programId)
                    .orElseThrow(() -> new RuntimeException("Program not found: " + programId));
                FacultyProgram facultyProgram = new FacultyProgram(faculty, program);
                facultyProgramRepository.save(facultyProgram);
            }

            facultyRepository.save(faculty);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty member updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating faculty member: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }
} 