package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/student")
public class StudentController {
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);
    
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final GradeRepository gradeRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;

    public StudentController(StudentRepository studentRepository,
                           CourseRepository courseRepository,
                           GradeRepository gradeRepository,
                           TeachingAssignmentRepository teachingAssignmentRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.gradeRepository = gradeRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
    }

    @GetMapping("/home")
    public String studentHome(Model model) {
        try {
            // Get current student
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String studentNumber = auth.getName();
            Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            // Get current academic year and semester
            String currentAcademicYear = getCurrentAcademicYear();
            int currentSemester = student.getCurrentSemester();

            // Check English Communication teaching assignments
            checkEnglishCommunicationAssignments(currentAcademicYear, currentSemester);

            // Get courses for student's program, year, and semester
            List<Course> courses = courseRepository.findByProgramIdAndYearAndSemester(
                student.getProgram().getId(),
                student.getEnrollmentYear(),
                currentSemester
            );

            // Get grades for current semester
            List<Grade> grades = gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
                student.getId(),
                currentAcademicYear,
                currentSemester
            );

            // Create a map of subject ID to grade for easy lookup
            Map<Long, Grade> gradeMap = grades.stream()
                .collect(Collectors.toMap(
                    grade -> grade.getSubject().getId(),
                    grade -> grade
                ));

            // Create a list of course data with grades
            List<Map<String, Object>> currentCourses = courses.stream()
                .flatMap(course -> course.getSubjects().stream())
                .map(subject -> {
                    Grade grade = gradeMap.get(subject.getId());
                    String instructorName = getInstructorName(subject, currentAcademicYear, currentSemester);
                    Map<String, Object> courseData = Map.of(
                        "code", subject.getCode(),
                        "name", subject.getName(),
                        "instructor", instructorName,
                        "units", subject.getUnits(),
                        "section", getSectionName(student, subject),
                        "rawGrade", getFormattedGrade(grade),
                        "letterGrade", getLetterGrade(grade),
                        "status", getGradeStatus(grade)
                    );
                    return courseData;
                })
                .collect(Collectors.toList());

            // GWA calculation: average of all numeric grades (Philippine system, e.g., 1.25, 1.5, etc.)
            List<Double> gwaList = grades.stream()
                .filter(g -> g.getGwa() != null && g.getRawGrade() != null)
                .map(Grade::getGwa)
                .collect(Collectors.toList());
            Double gwa = null;
            if (!gwaList.isEmpty()) {
                gwa = gwaList.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            }

            boolean canRequestApproval = currentCourses.stream()
                .allMatch(course -> course.get("rawGrade") != null && !"N/A".equals(course.get("rawGrade")));

            model.addAttribute("student", student);
            model.addAttribute("currentAcademicYear", currentAcademicYear);
            model.addAttribute("currentCourses", currentCourses);
            model.addAttribute("gwa", gwa);
            model.addAttribute("canRequestApproval", canRequestApproval);
            return "student/student_home";
        } catch (Exception e) {
            logger.error("Error loading student dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading student dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/subjects/all")
    @ResponseBody
    public Map<String, Object> getAllSubjectsForStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String studentNumber = auth.getName();
        Student student = studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        Program program = student.getProgram();
        if (program == null) {
            return Map.of("subjects", List.of());
        }
        // Get all courses for the student's program
        List<Course> courses = program.getCourses();
        if (courses == null || courses.isEmpty()) {
            return Map.of("subjects", List.of());
        }
        List<Map<String, Object>> subjects = courses.stream()
            .flatMap(course -> course.getSubjects().stream().map(subject -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("code", subject.getCode());
                map.put("name", subject.getName());
                map.put("yearLevel", course.getYear());
                map.put("semester", course.getSemester());
                return map;
            }))
            .sorted((a, b) -> {
                int cmp = Integer.compare((int)a.get("yearLevel"), (int)b.get("yearLevel"));
                if (cmp != 0) return cmp;
                cmp = Integer.compare((int)a.get("semester"), (int)b.get("semester"));
                if (cmp != 0) return cmp;
                return ((String)a.get("code")).compareTo((String)b.get("code"));
            })
            .collect(java.util.stream.Collectors.toList());
        return Map.of("subjects", subjects);
    }

    private void checkEnglishCommunicationAssignments(String academicYear, Integer semester) {
        logger.info("Checking teaching assignments for English Communication");
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findBySubjectName("English Communication");
        logger.info("Found {} teaching assignments for English Communication", assignments.size());
        
        assignments.forEach(ta -> {
            logger.info("Assignment - Faculty: {} {}, Year: {}, Semester: {}, Subject: {}",
                ta.getFaculty().getFirstName(),
                ta.getFaculty().getLastName(),
                ta.getAcademicYear(),
                ta.getSemester(),
                ta.getSubject().getName());
        });
    }

    private String getCurrentAcademicYear() {
        int year = LocalDate.now().getYear();
        return year + "-" + (year + 1);
    }

    private String getInstructorName(Subject subject, String academicYear, Integer semester) {
        logger.info("Looking up instructor for subject: {} (ID: {}) in year {} semester {}", 
            subject.getName(), subject.getId(), academicYear, semester);
            
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findBySubjectId(subject.getId());
        logger.info("Found {} teaching assignments for subject {}", assignments.size(), subject.getName());
        
        return assignments.stream()
            .filter(ta -> {
                boolean matches = ta.getAcademicYear().equals(academicYear) && 
                                ta.getSemester().equals(semester);
                if (matches) {
                    logger.info("Found matching assignment for faculty: {} {}", 
                        ta.getFaculty().getLastName(), ta.getFaculty().getFirstName());
                }
                return matches;
            })
            .findFirst()
            .map(ta -> ta.getFaculty().getLastName() + ", " + ta.getFaculty().getFirstName())
            .orElseGet(() -> {
                logger.info("No matching assignment found for subject {} in year {} semester {}", 
                    subject.getName(), academicYear, semester);
                return "TBA";
            });
    }

    private String getSectionName(Student student, Subject subject) {
        return student.getProgram().getName() + " " + student.getEnrollmentYear() + "-" + student.getCurrentSemester();
    }

    private String getFormattedGrade(Grade grade) {
        if (grade == null || grade.getRawGrade() == null) {
            return "N/A";
        }
        double rawGrade = grade.getRawGrade();
        return convertToGradeSystem(rawGrade);
    }

    private String getLetterGrade(Grade grade) {
        if (grade == null || grade.getRawGrade() == null) {
            return "N/A";
        }
        double rawGrade = grade.getRawGrade();
        return convertToLetterGrade(rawGrade);
    }

    private String getGradeStatus(Grade grade) {
        if (grade == null || grade.getRawGrade() == null) {
            return "N/A";
        }
        double rawGrade = grade.getRawGrade();
        if (rawGrade >= 75) {
            return "PASSED";
        } else {
            return "FAILED";
        }
    }

    private String convertToGradeSystem(double rawGrade) {
        // Convert raw grade to Philippine grading system
        if (rawGrade >= 97) return "1.00";
        if (rawGrade >= 94) return "1.25";
        if (rawGrade >= 91) return "1.50";
        if (rawGrade >= 88) return "1.75";
        if (rawGrade >= 85) return "2.00";
        if (rawGrade >= 82) return "2.25";
        if (rawGrade >= 79) return "2.50";
        if (rawGrade >= 76) return "2.75";
        if (rawGrade >= 75) return "3.00";
        if (rawGrade >= 70) return "4.00";
        return "5.00";
    }

    private String convertToLetterGrade(double rawGrade) {
        // Convert raw grade to letter grade
        if (rawGrade >= 97) return "A+";
        if (rawGrade >= 94) return "A";
        if (rawGrade >= 91) return "A-";
        if (rawGrade >= 88) return "B+";
        if (rawGrade >= 85) return "B";
        if (rawGrade >= 82) return "B-";
        if (rawGrade >= 79) return "C+";
        if (rawGrade >= 76) return "C";
        if (rawGrade >= 75) return "C-";
        if (rawGrade >= 70) return "D";
        return "F";
    }
} 