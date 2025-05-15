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

@Controller
@RequestMapping("/admin")
public class GradeController {
    private static final Logger logger = LoggerFactory.getLogger(GradeController.class);

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;

    public GradeController(StudentRepository studentRepository,
                          SubjectRepository subjectRepository,
                          GradeRepository gradeRepository,
                          CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
        this.courseRepository = courseRepository;
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

            logger.info("Student validation passed. Program: {}, Year: {}, Semester: {}", 
                student.getProgram().getName(),
                student.getEnrollmentYear(),
                student.getCurrentSemester());

            String currentAcademicYear = getCurrentAcademicYear();
            int currentSemester = student.getCurrentSemester();

            // Get subjects for the student's current year and semester
            logger.info("Fetching subjects for student {} in year {} semester {}", 
                studentId, student.getEnrollmentYear(), currentSemester);
            List<Subject> availableSubjects = getAvailableSubjects(student, currentSemester);
            
            if (availableSubjects.isEmpty()) {
                String message = String.format(
                    "No subjects found for %s, Year %d, Semester %d. Please ensure the curriculum is properly set up.", 
                    student.getProgram().getName(), 
                    student.getEnrollmentYear(), 
                    currentSemester
                );
                logger.warn(message);
                model.addAttribute("warningMessage", message);
            } else {
                logger.info("Found {} subjects for student", availableSubjects.size());
                for (Subject subject : availableSubjects) {
                    logger.info("Subject: {} - {}", subject.getCode(), subject.getName());
                }
            }
            
            logger.info("Fetching existing grades for student");
            List<Grade> existingGrades = gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
                    studentId, currentAcademicYear, currentSemester);
            logger.info("Found {} existing grades", existingGrades.size());

            // Add all necessary attributes to the model
            model.addAttribute("student", student);
            model.addAttribute("subjects", availableSubjects);
            model.addAttribute("grades", existingGrades);
            model.addAttribute("academicYear", currentAcademicYear);
            model.addAttribute("semester", currentSemester);

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
                             @RequestParam Long subjectId,
                             @RequestParam(required = false) Double grade,
                             @RequestParam String academicYear,
                             @RequestParam Integer semester,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        logger.debug("Received grade update request - Parameters: studentId={}, subjectId={}, grade={}, year={}, semester={}", 
            studentId, subjectId, grade, academicYear, semester);
            
        try {
            // Validate input parameters
            if (studentId == null || studentId <= 0) {
                throw new IllegalArgumentException("Invalid student ID");
            }
            if (subjectId == null || subjectId <= 0) {
                throw new IllegalArgumentException("Invalid subject ID");
            }
            if (grade == null) {
                throw new IllegalArgumentException("Grade cannot be null");
            }
            if (academicYear == null || academicYear.trim().isEmpty()) {
                throw new IllegalArgumentException("Academic year cannot be null or empty");
            }
            if (semester == null || semester < 1 || semester > 2) {
                throw new IllegalArgumentException("Invalid semester value");
            }
            
            // Validate grade range
            if (grade < 0 || grade > 100) {
                throw new IllegalArgumentException("Raw grade must be between 0 and 100");
            }

            // Find student
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
            logger.debug("Found student: {} {} (ID: {})", student.getFirstName(), student.getLastName(), student.getId());

            // Find subject
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new RuntimeException("Subject not found with ID: " + subjectId));
            logger.debug("Found subject: {} - {} (ID: {})", subject.getCode(), subject.getName(), subject.getId());

            // Find existing grade or create new one
            Grade gradeEntity;
            try {
                gradeEntity = gradeRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                        .orElse(new Grade(student, subject, null, academicYear, semester));
                logger.debug("Found existing grade: {}", gradeEntity != null ? gradeEntity.getId() : "new grade");
            } catch (Exception e) {
                logger.error("Error finding/creating grade entity", e);
                throw new RuntimeException("Error accessing grade data: " + e.getMessage());
            }

            // Update grade - this will automatically calculate GWA
            try {
                if (gradeEntity != null) {
                    gradeEntity.setRawGrade(grade);
                    gradeEntity.setAcademicYear(academicYear);
                    gradeEntity.setSemester(semester);
                    
                    logger.debug("Saving grade - Raw: {}, GWA: {}, Student: {}, Subject: {}", 
                        gradeEntity.getRawGrade(), 
                        gradeEntity.getGwa(),
                        student.getStudentNumber(), 
                        subject.getCode());

                    gradeRepository.save(gradeEntity);
                    logger.info("Grade saved successfully - ID: {}", gradeEntity.getId());
                } else {
                    throw new RuntimeException("Failed to create or find grade entity");
                }
            } catch (Exception e) {
                logger.error("Error saving grade entity", e);
                throw new RuntimeException("Error saving grade: " + e.getMessage());
            }

            String message = String.format("Grade saved successfully for %s - Raw: %.2f, GWA: %.2f (%s)", 
                subject.getName(), 
                gradeEntity.getRawGrade(),
                gradeEntity.getGwa(),
                gradeEntity.getLetterGrade());
            redirectAttributes.addFlashAttribute("successMessage", message);
            
            return "redirect:/admin/students/" + studentId + "/grades";
        } catch (Exception e) {
            logger.error("Error in updateGrades - Details: Student ID={}, Subject ID={}, Grade={}", 
                studentId != null ? studentId : "null", 
                subjectId != null ? subjectId : "null", 
                grade != null ? grade : "null", 
                e);
                
            // Try to load student data for error display
            try {
                if (studentId != null) {
                    Student student = studentRepository.findById(studentId).orElse(null);
                    if (student != null) {
                        String currentAcademicYear = getCurrentAcademicYear();
                        List<Subject> availableSubjects = getAvailableSubjects(student, student.getCurrentSemester());
                        List<Grade> existingGrades = gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
                            studentId, currentAcademicYear, student.getCurrentSemester());

                        model.addAttribute("student", student);
                        model.addAttribute("subjects", availableSubjects);
                        model.addAttribute("grades", existingGrades);
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
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        // If we're in the latter half of the year, academic year is year-year+1
        if (now.getMonthValue() > 6) {
            return year + "-" + (year + 1);
        }
        return (year - 1) + "-" + year;
    }
} 