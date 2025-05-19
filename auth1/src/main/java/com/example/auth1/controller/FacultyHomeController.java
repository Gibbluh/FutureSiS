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

    public FacultyHomeController(
            FacultyRepository facultyRepository,
            SubjectRepository subjectRepository,
            StudentRepository studentRepository,
            GradeRepository gradeRepository) {
        this.facultyRepository = facultyRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
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
            
            // Log teaching assignments for debugging
            if (faculty.getTeachingAssignments() != null) {
                faculty.getTeachingAssignments().forEach(assignment -> {
                    if (assignment.getSubject() != null) {
                        logger.info("Teaching Assignment - Subject: {}, Code: {}, Year: {}, Semester: {}",
                            assignment.getSubject().getName(),
                            assignment.getSubject().getCode(),
                            assignment.getSubject().getCourse() != null ? assignment.getSubject().getCourse().getYear() : "N/A",
                            assignment.getSemester());
                    } else {
                        logger.warn("Found teaching assignment with null subject for faculty: {}", facultyId);
                    }
                });
            }
            
            model.addAttribute("faculty", faculty);
            return "faculty/faculty_home";
            
        } catch (Exception e) {
            logger.error("Error loading faculty data: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading faculty data: " + e.getMessage());
            model.addAttribute("errorDetails", "Please contact support if the problem persists.");
            return "faculty/faculty_home";
        }
    }

    @GetMapping("/subjects/{subjectId}/students")
    @ResponseBody
    public ResponseEntity<?> getStudentsForSubject(
            @PathVariable Long subjectId,
            @RequestParam String academicYear,
            @RequestParam Integer semester) {
        try {
            // Get current faculty
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String facultyId = auth.getName();
            Faculty faculty = facultyRepository.findByFacultyId(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Verify that this faculty is assigned to this subject
            boolean isAssigned = faculty.getTeachingAssignments().stream()
                .anyMatch(assignment -> 
                    assignment.getSubject().getId().equals(subjectId) &&
                    assignment.getAcademicYear().equals(academicYear) &&
                    assignment.getSemester().equals(semester));

            if (!isAssigned) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "You are not assigned to teach this subject"));
            }

            // Get the subject
            Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

            // Get students enrolled in this subject
            List<Student> enrolledStudents = studentRepository.findBySubjectAndYearAndSemester(
                subject, semester);

            // Get grades for these students
            Map<Long, Grade> studentGrades = gradeRepository
                .findBySubjectIdAndAcademicYearAndSemester(subjectId, academicYear, semester)
                .stream()
                .collect(Collectors.toMap(
                    grade -> grade.getStudent().getId(),
                    grade -> grade
                ));

            // Build response
            List<Map<String, Object>> studentList = enrolledStudents.stream()
                .map(student -> {
                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("id", student.getId());
                    studentMap.put("studentNumber", student.getStudentNumber());
                    studentMap.put("firstName", student.getFirstName());
                    studentMap.put("lastName", student.getLastName());
                    studentMap.put("program", student.getProgram().getName());
                    studentMap.put("yearLevel", student.getEnrollmentYear());
                    
                    // Add grade if exists
                    Grade grade = studentGrades.get(student.getId());
                    if (grade != null) {
                        studentMap.put("grade", grade.getRawGrade());
                    }
                    
                    return studentMap;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("students", studentList));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error fetching students: " + e.getMessage()));
        }
    }

    @GetMapping("/grades/{subjectId}")
    public String manageGrades(@PathVariable Long subjectId, Model model) {
        try {
            // Get current faculty
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String facultyId = auth.getName();
            
            // Load faculty with teaching assignments
            Faculty faculty = facultyRepository.findByFacultyIdWithTeachingAssignments(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Get the subject
            Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

            // Get current academic year and semester from the subject's course
            String currentAcademicYear = getCurrentAcademicYear();
            Integer subjectSemester = subject.getCourse().getSemester();

            logger.info("Checking teaching assignment for faculty {} subject {} year {} semester {}", 
                facultyId, subjectId, currentAcademicYear, subjectSemester);

            // Check if faculty is assigned to this subject in any semester
            boolean isAssigned = faculty.getTeachingAssignments().stream()
                .anyMatch(a -> a.getSubject().getId().equals(subjectId));

            if (!isAssigned) {
                throw new RuntimeException("You are not assigned to teach this subject");
            }

            // Get students and their grades using the subject's semester
            List<Student> students = studentRepository.findBySubjectAndYearAndSemester(
                subject, subjectSemester);

            List<Grade> grades = gradeRepository.findBySubjectIdAndAcademicYearAndSemester(
                subjectId, currentAcademicYear, subjectSemester);

            Map<Long, Grade> studentGrades = grades.stream()
                .collect(Collectors.toMap(
                    g -> g.getStudent().getId(),
                    g -> g,
                    (existing, replacement) -> existing
                ));

            model.addAttribute("subject", subject);
            model.addAttribute("students", students);
            model.addAttribute("grades", studentGrades);
            model.addAttribute("academicYear", currentAcademicYear);
            model.addAttribute("semester", subjectSemester);

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
            @RequestParam Long subjectId,
            @RequestParam Double grade) {
        try {
            // Get current faculty
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String facultyId = auth.getName();
            
            // Load faculty with teaching assignments
            Faculty faculty = facultyRepository.findByFacultyIdWithTeachingAssignments(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Get the subject to get its semester
            Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

            // Get current academic year and semester from the subject's course
            String academicYear = getCurrentAcademicYear();
            Integer semester = subject.getCourse().getSemester();

            logger.info("Checking teaching assignment for faculty {} subject {} year {} semester {}", 
                facultyId, subjectId, academicYear, semester);

            // Check if faculty is assigned to this subject in any semester
            boolean isAssigned = faculty.getTeachingAssignments().stream()
                .anyMatch(assignment -> assignment.getSubject().getId().equals(subjectId));

            if (!isAssigned) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "You are not assigned to teach this subject"));
            }

            // Validate grade
            if (grade < 0 || grade > 100) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Grade must be between 0 and 100"));
            }

            // Get or create grade
            Grade gradeEntity = gradeRepository
                .findByStudentIdAndSubjectId(studentId, subjectId, academicYear, semester)
                .orElseGet(() -> {
                    Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new RuntimeException("Student not found"));
                    return new Grade(student, subject, grade, academicYear, semester);
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

    private String getCurrentAcademicYear() {
        int year = LocalDate.now().getYear();
        return year + "-" + (year + 1);
    }

    private Integer getCurrentSemester() {
        return 1; // Default to first semester
    }
} 