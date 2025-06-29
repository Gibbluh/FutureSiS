package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.transaction.Transactional;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Controller
@RequestMapping("/faculty")
public class FacultyHomeController {
    private static final Logger logger = LoggerFactory.getLogger(FacultyHomeController.class);
    private final FacultyRepository facultyRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final SubjectSectionRepository subjectSectionRepository;
    private final SectionRepository sectionRepository;
    private final PasswordEncoder passwordEncoder;

    public FacultyHomeController(
            FacultyRepository facultyRepository,
            SubjectRepository subjectRepository,
            StudentRepository studentRepository,
            GradeRepository gradeRepository,
            TeachingAssignmentRepository teachingAssignmentRepository,
            SubjectSectionRepository subjectSectionRepository,
            SectionRepository sectionRepository,
            PasswordEncoder passwordEncoder) {
        this.facultyRepository = facultyRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.subjectSectionRepository = subjectSectionRepository;
        this.sectionRepository = sectionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "faculty/faculty_login";
    }

    @GetMapping("/home")
    @Transactional
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String facultyId = auth.getName();
        
        try {
            logger.info("Loading faculty home page for faculty ID: {}", facultyId);
            
            // First try to load basic faculty data
            Faculty faculty = facultyRepository.findByFacultyId(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));
            
            logger.info("Found faculty: {} {}", faculty.getFirstName(), faculty.getLastName());
            
            // Load faculty with all relationships
            faculty = facultyRepository.findByFacultyIdWithTeachingAssignments(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with teaching assignments"));
            
            // Load programs separately to avoid circular references
            Faculty facultyWithPrograms = facultyRepository.findByFacultyIdWithPrograms(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with programs"));
            
            // Merge the data
            faculty.setFacultyPrograms(facultyWithPrograms.getFacultyPrograms());
            
            logger.info("Loaded faculty data:");
            logger.info("- Programs: {}", 
                faculty.getFacultyPrograms() != null ? faculty.getFacultyPrograms().size() : 0);
            logger.info("- Teaching Assignments: {}", 
                faculty.getTeachingAssignments() != null ? faculty.getTeachingAssignments().size() : 0);
            
            // Group teaching assignments by subject for better display
            Map<Subject, List<TeachingAssignment>> assignmentsBySubject = new HashMap<>();
            Set<String> uniqueRooms = new HashSet<>();
            Set<String> uniqueSchedules = new HashSet<>();
            if (faculty.getTeachingAssignments() != null) {
                for (TeachingAssignment assignment : faculty.getTeachingAssignments()) {
                    Subject subject = assignment.getSubject();
                    if (subject != null) {
                        assignmentsBySubject.computeIfAbsent(subject, k -> new ArrayList<>()).add(assignment);
                    }
                    SubjectSection ss = assignment.getSubjectSection();
                    if (ss != null) {
                        if (ss.getRoom() != null && !ss.getRoom().isEmpty()) {
                            uniqueRooms.add(ss.getRoom());
                        }
                        if (ss.getSchedule() != null && !ss.getSchedule().isEmpty()) {
                            uniqueSchedules.add(ss.getSchedule());
                        }
                    }
                }
            }
            
            // Get all program IDs the faculty is assigned to
            List<Long> programIds = faculty.getFacultyPrograms().stream()
                .map(fp -> fp.getProgram().getId())
                .distinct()
                .collect(Collectors.toList());

            // Get all sections for these programs
            List<Section> uniqueSections = sectionRepository.findByProgramIdIn(programIds);

            // Build a map of subjectSectionId to student count for all teaching assignments
            Map<Long, Integer> subjectSectionStudentCounts = new HashMap<>();
            if (faculty.getTeachingAssignments() != null) {
                for (TeachingAssignment assignment : faculty.getTeachingAssignments()) {
                    SubjectSection ss = assignment.getSubjectSection();
                    if (ss != null && !subjectSectionStudentCounts.containsKey(ss.getId())) {
                        int count = gradeRepository.findBySubjectSectionIdWithStudentDetails(ss.getId()).size();
                        subjectSectionStudentCounts.put(ss.getId(), count);
                    }
                }
            }

            model.addAttribute("faculty", faculty);
            model.addAttribute("assignmentsBySubject", assignmentsBySubject);
            model.addAttribute("uniqueRooms", uniqueRooms);
            model.addAttribute("uniqueSchedules", uniqueSchedules);
            model.addAttribute("uniqueSections", uniqueSections);
            model.addAttribute("subjectSectionStudentCounts", subjectSectionStudentCounts);
            return "faculty/faculty_home";
            
        } catch (Exception e) {
            logger.error("Error loading faculty data: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading faculty data: " + e.getMessage());
            model.addAttribute("errorDetails", "Please contact support if the problem persists.");
            return "faculty/faculty_home";
        }
    }

    @GetMapping("/subjects/{subjectId}/sections")
    @ResponseBody
    public ResponseEntity<?> getSectionsForSubject(
            @PathVariable Long subjectId,
            @RequestParam String academicYear,
            @RequestParam Integer semester) {
        try {
            // Get current faculty
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String facultyId = auth.getName();
            Faculty faculty = facultyRepository.findByFacultyId(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Get all program IDs the faculty is assigned to
            List<Long> programIds = faculty.getFacultyPrograms().stream()
                .map(fp -> fp.getProgram().getId())
                .distinct()
                .collect(Collectors.toList());

            // Get all sections for these programs
            List<Section> sections = sectionRepository.findByProgramIdIn(programIds);

            // Build response with sections
            List<Map<String, Object>> sectionList = sections.stream()
                .map(section -> {
                    Map<String, Object> sectionMap = new HashMap<>();
                    sectionMap.put("sectionId", section.getId());
                    sectionMap.put("sectionName", section.getName());
                    sectionMap.put("yearLevel", section.getYearLevel());
                    sectionMap.put("programName", section.getProgram().getName());
                    return sectionMap;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("sections", sectionList));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error fetching sections: " + e.getMessage()));
        }
    }

    @GetMapping("/grades/{subjectSectionId}")
    public String manageGrades(@PathVariable Long subjectSectionId, Model model) {
        try {
            // Get current faculty
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String facultyId = auth.getName();
            
            // Load faculty with teaching assignments
            Faculty faculty = facultyRepository.findByFacultyIdWithTeachingAssignments(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Get the subject section
            SubjectSection subjectSection = subjectSectionRepository.findById(subjectSectionId)
                .orElseThrow(() -> new RuntimeException("Subject Section not found"));

            // Check if faculty is assigned to this subject section
            boolean isAssigned = faculty.getTeachingAssignments().stream()
                .anyMatch(a -> a.getSubjectSection().getId().equals(subjectSectionId));

            if (!isAssigned) {
                throw new RuntimeException("You are not assigned to teach this subject section");
            }

            // Get students who have a grade for this subject section (even if they moved up)
            List<Grade> grades = gradeRepository.findBySubjectSectionIdWithStudentDetails(subjectSectionId);
            List<Student> students = grades.stream().map(Grade::getStudent).distinct().collect(Collectors.toList());
            Map<Long, Grade> studentGrades = grades.stream()
                .collect(Collectors.toMap(
                    g -> g.getStudent().getId(),
                    g -> g,
                    (existing, replacement) -> existing
                ));
            model.addAttribute("subjectSection", subjectSection);
            model.addAttribute("students", students);
            model.addAttribute("grades", studentGrades);
            model.addAttribute("academicYear", subjectSection.getAcademicYear());
            model.addAttribute("semester", subjectSection.getSemester());
            return "faculty/manage_grades";
        } catch (Exception e) {
            logger.error("Error in manageGrades: {}", e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "faculty/error";
        }
    }

    @PostMapping("/grades/update")
    @ResponseBody
    public ResponseEntity<?> updateGrade(
            @RequestParam Long studentId,
            @RequestParam Long subjectSectionId,
            @RequestParam Double grade) {
        try {
            // Get current faculty
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String facultyId = auth.getName();
            
            // Load faculty with teaching assignments
            Faculty faculty = facultyRepository.findByFacultyIdWithTeachingAssignments(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Check if faculty is assigned to this subject section
            boolean isAssigned = faculty.getTeachingAssignments().stream()
                .anyMatch(assignment -> assignment.getSubjectSection().getId().equals(subjectSectionId));

            if (!isAssigned) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "You are not assigned to teach this subject section"));
            }

            // Validate grade
            if (grade < 0 || grade > 100) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Grade must be between 0 and 100"));
            }

            // Get the subject section
            SubjectSection subjectSection = subjectSectionRepository.findById(subjectSectionId)
                .orElseThrow(() -> new RuntimeException("Subject Section not found"));

            // Get or create grade
            Grade gradeEntity = gradeRepository
                .findByStudentIdAndSubjectSectionId(studentId, subjectSectionId)
                .orElseGet(() -> {
                    Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new RuntimeException("Student not found"));
                    return new Grade(student, subjectSection, grade);
                });

            // Update grade
            gradeEntity.setRawGrade(grade);
            gradeRepository.save(gradeEntity);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Grade updated successfully",
                "grade", gradeEntity.getRawGrade(),
                "gwa", gradeEntity.getGwa()
            ));
        } catch (Exception e) {
            logger.error("Error updating grade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error updating grade: " + e.getMessage()));
        }
    }

    @GetMapping("/subject-sections/{subjectSectionId}/students/download")
    public void downloadStudentListExcel(@PathVariable Long subjectSectionId, HttpServletResponse response) throws Exception {
        SubjectSection subjectSection = subjectSectionRepository.findById(subjectSectionId)
            .orElseThrow(() -> new RuntimeException("Subject Section not found"));
        // Get all students who have a grade for this subject section
        List<Grade> grades = gradeRepository.findBySubjectSectionIdWithStudentDetails(subjectSectionId);
        List<Student> students = grades.stream().map(Grade::getStudent).distinct().collect(java.util.stream.Collectors.toList());

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Students");
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Student Number");
        header.createCell(1).setCellValue("Last Name");
        header.createCell(2).setCellValue("First Name");
        header.createCell(3).setCellValue("Program");
        int rowIdx = 1;
        for (Student student : students) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(student.getStudentNumber());
            row.createCell(1).setCellValue(student.getLastName());
            row.createCell(2).setCellValue(student.getFirstName());
            row.createCell(3).setCellValue(student.getProgram() != null ? student.getProgram().getName() : "");
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=students_section_" + subjectSectionId + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "faculty/faculty_change_password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String facultyId = auth.getName();
        Faculty faculty = facultyRepository.findByFacultyId(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        if (!passwordEncoder.matches(oldPassword, faculty.getPassword())) {
            model.addAttribute("error", "Old password is incorrect.");
            return "faculty/faculty_change_password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match.");
            return "faculty/faculty_change_password";
        }
        faculty.setPassword(passwordEncoder.encode(newPassword));
        facultyRepository.save(faculty);
        redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
        return "redirect:/faculty/home";
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String facultyId = auth.getName();
        Faculty faculty = facultyRepository.findByFacultyId(facultyId)
            .orElseThrow(() -> new RuntimeException("Faculty not found"));
        model.addAttribute("faculty", faculty);
        return "faculty/faculty_profile";
    }

    @PostMapping("/change-id")
    @ResponseBody
    public Map<String, Object> changeFacultyId(@RequestParam String newFacultyId, @RequestParam String currentPassword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentFacultyId = auth.getName();
        Faculty faculty = facultyRepository.findByFacultyId(currentFacultyId)
            .orElse(null);
        if (faculty == null) {
            return Map.of("success", false, "message", "Faculty not found.");
        }
        if (!passwordEncoder.matches(currentPassword, faculty.getPassword())) {
            return Map.of("success", false, "message", "Incorrect password.");
        }
        if (newFacultyId.equals(currentFacultyId)) {
            return Map.of("success", false, "message", "New Faculty ID must be different.");
        }
        if (facultyRepository.findByFacultyId(newFacultyId).isPresent()) {
            return Map.of("success", false, "message", "Faculty ID already taken.");
        }
        faculty.setFacultyId(newFacultyId);
        facultyRepository.save(faculty);
        return Map.of("success", true);
    }
} 