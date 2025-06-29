package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class GradeController {
    private static final Logger logger = LoggerFactory.getLogger(GradeController.class);

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final FacultyRepository facultyRepository;
    private final SubjectSectionRepository subjectSectionRepository;

    public GradeController(StudentRepository studentRepository,
                          SubjectRepository subjectRepository,
                          GradeRepository gradeRepository,
                          CourseRepository courseRepository,
                          TeachingAssignmentRepository teachingAssignmentRepository,
                          FacultyRepository facultyRepository,
                          SubjectSectionRepository subjectSectionRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
        this.courseRepository = courseRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.facultyRepository = facultyRepository;
        this.subjectSectionRepository = subjectSectionRepository;
    }

    @GetMapping("/students/{studentId}/grades")
    public String viewStudentGrades(@PathVariable Long studentId, Model model) {
        logger.info("Attempting to view grades for student ID: {}", studentId);
        try {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

            logger.info("Found student: {} {} (ID: {})", 
                student.getFirstName(), 
                student.getLastName(), 
                student.getId());
            
            // Validate required fields
            if (student.getEnrollmentYear() == null) {
                logger.error("Student {} has no enrollment year set", studentId);
                throw new RuntimeException("Student enrollment year is not set");
            }
            
            if (student.getCurrentSemester() == null) {
                logger.error("Student {} has no current semester set", studentId);
                throw new RuntimeException("Student semester is not set");
            }

            if (student.getProgram() == null) {
                logger.warn("Student {} has no program assigned", studentId);
                model.addAttribute("errorMessage", "Student must be assigned to a program to view grades");
                model.addAttribute("student", student);
                return "admin/student_grades";
            }

            String currentAcademicYear = student.getAcademicYear();
            logger.info("Student validation passed. Program: {}, Year: {}, Semester: {}, Academic Year: {}",
                student.getProgram().getName(),
                student.getEnrollmentYear(),
                student.getCurrentSemester(),
                currentAcademicYear);

            // Fetch grades for the student's current semester ONLY
            List<Grade> currentGrades = gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
                studentId,
                currentAcademicYear,
                student.getCurrentSemester()
            );

            // Filter grades to only those matching the student's current year, section, and semester
            currentGrades = currentGrades.stream()
                .filter(g -> {
                    SubjectSection ss = g.getSubjectSection();
                    return ss != null
                        && ss.getSection() != null
                        && ss.getSection().getId().equals(student.getSection() != null ? student.getSection().getId() : null)
                        && ss.getSubject().getYearLevel() == student.getEnrollmentYear()
                        && ss.getSemester() == student.getCurrentSemester()
                        && ss.getAcademicYear().equals(currentAcademicYear);
                })
                .collect(Collectors.toList());
            logger.info("Found {} grades for student {} in the current semester", currentGrades.size(), studentId);

            // Get all faculty members associated with the student's program
            List<Faculty> faculties = student.getProgram() != null ? 
                facultyRepository.findByFacultyPrograms_Program_Id(student.getProgram().getId()) : 
                List.of();
            logger.info("Found {} faculty members for program {}", faculties.size(), student.getProgram() != null ? student.getProgram().getName() : "N/A");

            // Get current teaching assignments for the grades' subject sections
            Map<Long, Long> assignedFacultyMap = new HashMap<>();
            for (Grade grade : currentGrades) {
                teachingAssignmentRepository.findBySubjectSection(grade.getSubjectSection()).ifPresent(ta -> {
                    assignedFacultyMap.put(grade.getSubjectSection().getId(), ta.getFaculty().getId());
                });
            }

            String assignedFacultyMapJson = "{}";
            try {
                assignedFacultyMapJson = new ObjectMapper().writeValueAsString(assignedFacultyMap);
            } catch (JsonProcessingException e) {
                logger.error("Error converting assigned faculty map to JSON", e);
            }

            // Add all necessary attributes to the model
            model.addAttribute("student", student);
            model.addAttribute("grades", currentGrades);
            model.addAttribute("faculties", faculties);
            model.addAttribute("assignedFacultyMapJson", assignedFacultyMapJson);

            logger.info("Successfully prepared grade view model");
            return "admin/student_grades";
        } catch (Exception e) {
            logger.error("Error in viewStudentGrades for student ID {}: {}", studentId, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading grades: " + e.getMessage());
            try {
                Student student = studentRepository.findById(studentId).orElse(null);
                if (student != null) {
                    model.addAttribute("student", student);
                }
            } catch (Exception ex) {
                logger.error("Could not load student data for error page", ex);
            }
            return "admin/student_grades";
        }
    }

    @PostMapping("/students/{studentId}/grades/save")
    public String updateGrades(@PathVariable(required = true) Long studentId,
                             @RequestParam Long subjectSectionId,
                             @RequestParam(required = false) Double grade,
                             @RequestParam(required = false) String facultyId,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        logger.debug("Received grade update request - Parameters: studentId={}, subjectSectionId={}, grade={}, facultyId={}", 
            studentId, subjectSectionId, grade, facultyId);
            
        try {
            // Validate input parameters
            if (studentId == null || studentId <= 0) {
                throw new IllegalArgumentException("Invalid student ID");
            }
            if (subjectSectionId == null || subjectSectionId <= 0) {
                throw new IllegalArgumentException("Invalid subject section ID");
            }
            if (grade == null) {
                throw new IllegalArgumentException("Grade cannot be null");
            }
            
            // Validate grade range
            if (grade < 0 || grade > 100) {
                throw new IllegalArgumentException("Raw grade must be between 0 and 100");
            }

            // Find student
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
            logger.debug("Found student: {} {} (ID: {})", student.getFirstName(), student.getLastName(), student.getId());

            // Find subject section
            SubjectSection subjectSection = subjectSectionRepository.findById(subjectSectionId)
                    .orElseThrow(() -> new RuntimeException("Subject Section not found with ID: " + subjectSectionId));
            logger.debug("Found subject section: {} - {} (Section: {})", 
                subjectSection.getSubject().getCode(), 
                subjectSection.getSubject().getName(), 
                subjectSection.getSection().getName());

            // Find or create teaching assignment if faculty is specified
            if (facultyId != null && !facultyId.trim().isEmpty()) {
                Faculty faculty = facultyRepository.findByFacultyId(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));
                
                // Check if teaching assignment already exists
                boolean hasAssignment = teachingAssignmentRepository
                    .existsByFacultyIdAndSubjectSectionId(faculty.getId(), subjectSectionId);
                
                if (!hasAssignment) {
                    TeachingAssignment teachingAssignment = new TeachingAssignment(faculty, subjectSection);
                    teachingAssignmentRepository.save(teachingAssignment);
                    
                    logger.info("Created new teaching assignment for faculty {} and subject section {}", 
                        faculty.getFacultyId(), subjectSectionId);
                }
            }

            // Find existing grade or create new one
            Grade gradeEntity;
            try {
                gradeEntity = gradeRepository.findByStudentIdAndSubjectSectionId(studentId, subjectSectionId)
                        .orElse(new Grade(student, subjectSection, null));
                logger.debug("Found existing grade: {}", gradeEntity != null ? gradeEntity.getId() : "new grade");
            } catch (Exception e) {
                logger.error("Error finding/creating grade entity", e);
                throw new RuntimeException("Error accessing grade data: " + e.getMessage());
            }

            // Update grade
            try {
                if (gradeEntity != null) {
                    gradeEntity.setRawGrade(grade);
                    
                    logger.debug("Saving grade - Raw: {}, GWA: {}, Student: {}, Subject: {}, Section: {}", 
                        gradeEntity.getRawGrade(), 
                        gradeEntity.getGwa(),
                        student.getStudentNumber(), 
                        subjectSection.getSubject().getCode(),
                        subjectSection.getSection().getName());

                    gradeRepository.save(gradeEntity);
                    logger.info("Grade saved successfully - ID: {}", gradeEntity.getId());
                } else {
                    throw new RuntimeException("Failed to create or find grade entity");
                }
            } catch (Exception e) {
                logger.error("Error saving grade entity", e);
                throw new RuntimeException("Error saving grade: " + e.getMessage());
            }

            String message = String.format("Grade saved successfully for %s - %s - Raw: %.2f, GWA: %.2f (%s)", 
                subjectSection.getSubject().getName(),
                subjectSection.getSection().getName(),
                gradeEntity.getRawGrade(),
                gradeEntity.getGwa(),
                gradeEntity.getLetterGrade());
            redirectAttributes.addFlashAttribute("successMessage", message);
            
            return "redirect:/admin/students/" + studentId + "/grades";
        } catch (Exception e) {
            logger.error("Error in updateGrades - Details: Student ID={}, Subject Section ID={}, Grade={}", 
                studentId != null ? studentId : "null", 
                subjectSectionId != null ? subjectSectionId : "null", 
                grade != null ? grade : "null", 
                e);
                
            // Try to load student data for error display
            try {
                if (studentId != null) {
                    Student student = studentRepository.findById(studentId).orElse(null);
                    if (student != null) {
                        String currentAcademicYear = student.getAcademicYear();
                        List<Subject> enrolledSubjects = gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
                            studentId, currentAcademicYear, student.getCurrentSemester())
                            .stream()
                            .map(Grade::getSubject)
                            .collect(Collectors.toList());

                        model.addAttribute("student", student);
                        model.addAttribute("subjects", enrolledSubjects);
                        model.addAttribute("grades", gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
                            studentId, currentAcademicYear, student.getCurrentSemester()));
                        model.addAttribute("academicYear", currentAcademicYear);
                        model.addAttribute("semester", student.getCurrentSemester());
                        
                        logger.debug("Successfully loaded student data for error page - Student ID: {}", student.getId());
                    }
                }
            } catch (Exception ex) {
                logger.error("Additional error while loading student data for error page", ex);
            }
            
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An unexpected error occurred while updating the grade. Please try again.";
            }
            model.addAttribute("errorMessage", errorMessage);
            
            // Return to the same page with error message
            return "admin/student_grades";
        }
    }

    @PostMapping("/grades/update/{gradeId}")
    @ResponseBody
    public Map<String, Object> updateGradeViaAjax(@PathVariable Long gradeId,
                                                @RequestParam Optional<Double> rawGrade,
                                                @RequestParam Long subjectSectionId,
                                                @RequestParam(required = false) Long facultyId) {
        try {
            Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Grade not found with ID: " + gradeId));

            grade.setRawGrade(rawGrade.orElse(null));
            gradeRepository.save(grade);

            // Handle faculty assignment
            if (facultyId != null) {
                Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + facultyId));
                
                SubjectSection subjectSection = subjectSectionRepository.findById(subjectSectionId)
                    .orElseThrow(() -> new RuntimeException("Subject Section not found with id: " + subjectSectionId));

                // Find existing teaching assignment for this subject section
                Optional<TeachingAssignment> existingAssignment = teachingAssignmentRepository.findBySubjectSection(subjectSection);

                if (existingAssignment.isPresent()) {
                    // Assignment exists, update it
                    TeachingAssignment ta = existingAssignment.get();
                    if (!ta.getFaculty().getId().equals(facultyId)) {
                        ta.setFaculty(faculty);
                        teachingAssignmentRepository.save(ta);
                        logger.info("Updated teaching assignment for subject section {} to faculty {}", subjectSectionId, facultyId);
                    }
                } else {
                    // No assignment exists, create a new one
                    TeachingAssignment newAssignment = new TeachingAssignment(faculty, subjectSection);
                    teachingAssignmentRepository.save(newAssignment);
                    logger.info("Created new teaching assignment for subject section {} with faculty {}", subjectSectionId, facultyId);
                }
            } else {
                 // if facultyId is null, it means we might need to un-assign
                 List<TeachingAssignment> assignments = teachingAssignmentRepository.findBySubjectSectionId(subjectSectionId);
                 if (!assignments.isEmpty()) {
                    teachingAssignmentRepository.deleteAll(assignments);
                    logger.info("Removed {} teaching assignment(s) for subject section {}", assignments.size(), subjectSectionId);
                }
            }

            return Map.of("success", true, "message", "Grade and assignment updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating grade via AJAX for grade ID {}: {}", gradeId, e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private List<Subject> getAvailableSubjects(Student student, int currentSemester) {
        if (student.getProgram() == null) {
            logger.warn("Student has no program assigned");
            return List.of();
        }

        int yearLevel = student.getEnrollmentYear();
        logger.info("Getting subjects for Program: {} (ID: {}), Year: {}, Semester: {}", 
            student.getProgram().getName(),
            student.getProgram().getId(),
            yearLevel, 
            currentSemester);

        try {
            // First check if the program exists and has courses
            Program program = student.getProgram();
            if (program.getCourses() == null || program.getCourses().isEmpty()) {
                logger.warn("Program {} has no courses defined", program.getName());
                return List.of();
            }

            // Find courses for the current year and semester
            List<Course> courses = courseRepository.findByProgramIdAndYearAndSemester(
                    program.getId(),
                    yearLevel,
                    currentSemester);
            
            logger.info("Found {} courses for program {} year {} semester {}", 
                courses.size(), program.getName(), yearLevel, currentSemester);

            if (courses.isEmpty()) {
                logger.warn("No courses found for Program: {}, Year: {}, Semester: {}", 
                    program.getName(), yearLevel, currentSemester);
                return List.of();
            }

            // Get all subjects for the first course
            Course currentCourse = courses.get(0);
            logger.info("Using course ID: {} (Year: {}, Semester: {})", 
                currentCourse.getId(), 
                currentCourse.getYear(), 
                currentCourse.getSemester());
            
            List<Subject> subjects = subjectRepository.findByCourseId(currentCourse.getId());
            
            if (subjects.isEmpty()) {
                logger.warn("No subjects found for Course ID: {}", currentCourse.getId());
            } else {
                logger.info("Found {} subjects for course {}", subjects.size(), currentCourse.getId());
                subjects.forEach(subject -> 
                    logger.debug("Subject: {} - {} ({} units)", 
                        subject.getCode(), subject.getName(), subject.getUnits()));
            }
            
            return subjects;
        } catch (Exception e) {
            logger.error("Error fetching subjects for student {} in program {}: {}", 
                student.getId(), student.getProgram().getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to load subjects: " + e.getMessage(), e);
        }
    }

    private String getCurrentAcademicYear() {
        int year = LocalDate.now().getYear();
        return year + "-" + (year + 1);
    }
} 