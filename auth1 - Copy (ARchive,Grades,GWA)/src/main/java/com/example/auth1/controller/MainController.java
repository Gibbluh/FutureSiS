package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import com.example.auth1.service.StudentNumberGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MainController {

    private final StudentRepository studentRepository;
    private final StudentNumberGenerator studentNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    public MainController(StudentRepository studentRepository,
                        StudentNumberGenerator studentNumberGenerator,
                        PasswordEncoder passwordEncoder,
                        ProgramRepository programRepository,
                        CourseRepository courseRepository,
                        SubjectRepository subjectRepository,
                        GradeRepository gradeRepository) {
        this.studentRepository = studentRepository;
        this.studentNumberGenerator = studentNumberGenerator;
        this.passwordEncoder = passwordEncoder;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
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
        model.addAttribute("students", studentRepository.findByArchivedFalse());
        model.addAttribute("programs", programRepository.findAll());
        
        // Add enrollment years for the dropdown (current year + 5 years)
        int currentYear = LocalDate.now().getYear();
        List<Integer> enrollmentYears = IntStream.rangeClosed(currentYear, currentYear + 5)
            .boxed().collect(Collectors.toList());
        model.addAttribute("enrollmentYears", enrollmentYears);
        
        return "admin/admin_home";
    }

    // Program Management
    @GetMapping("/admin/programs")
    @Transactional(readOnly = true)
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
    @Transactional
    public String addProgram(@RequestParam String name, RedirectAttributes redirectAttributes) {
        try {
            Program program = new Program(name);
            program = programRepository.save(program);
            
            for (int year = 1; year <= 4; year++) {
                for (int semester = 1; semester <= 2; semester++) {
                    Course course = new Course();
                    course.setYear(year);
                    course.setSemester(semester);
                    course.setProgram(program);
                    courseRepository.save(course);
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Program added successfully with 4-year course structure!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error adding program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    @GetMapping("/admin/programs/edit/{id}")
    @Transactional(readOnly = true)
    public String showEditProgramPage(@PathVariable Long id, Model model) {
        try {
            Program program = programRepository.findByIdWithCourses(id)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            
            if (program.getCourses() != null) {
                program.setCourses(program.getCourses().stream()
                    .sorted(Comparator.comparingInt(Course::getYear)
                        .thenComparingInt(Course::getSemester))
                    .collect(Collectors.toList()));
            }
            
            model.addAttribute("program", program);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading program: " + e.getMessage());
        }
        return "admin/edit_program";
    }

    @PostMapping("/admin/programs/edit/{id}")
    @Transactional
    public String editProgram(
            @PathVariable Long id,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            program.setName(name);
            programRepository.save(program);
            redirectAttributes.addFlashAttribute("successMessage", "Program updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/programs/delete/{id}")
    @Transactional
    public String deleteProgram(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            programRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Program deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    // Course Management
    @GetMapping("/admin/programs/{programId}/courses")
    @Transactional(readOnly = true)
    public String viewProgramCourses(@PathVariable Long programId, Model model) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new RuntimeException("Program not found"));
        List<Course> courses = courseRepository.findByProgramIdOrderByYearAscSemesterAsc(programId);
        
        model.addAttribute("program", program);
        model.addAttribute("courses", courses);
        return "admin/program_courses";
    }

    // Subject Management - Fully Updated
    @GetMapping("/admin/courses/{courseId}/subjects")
    @Transactional(readOnly = true)
    public String viewCourseSubjects(@PathVariable Long courseId, Model model) {
        try {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            List<Subject> subjects = subjectRepository.findByCourseId(courseId);
            
            model.addAttribute("course", course);
            model.addAttribute("subjects", subjects);
            return "admin/course_subjects";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading subjects: " + e.getMessage());
            return "redirect:/admin/programs";
        }
    }

    @GetMapping("/admin/courses/{courseId}/subjects/add")
    public String showAddSubjectForm(@PathVariable Long courseId, Model model) {
        try {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            Subject subject = new Subject();
            subject.setCourse(course);
            
            model.addAttribute("course", course);
            model.addAttribute("subject", subject);
            return "admin/add_subject";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading form: " + e.getMessage());
            return "redirect:/admin/courses/" + courseId + "/subjects";
        }
    }

    @PostMapping("/admin/courses/{courseId}/subjects/add")
    @Transactional
    public String addSubject(
            @PathVariable Long courseId,
            @ModelAttribute("subject") Subject subject,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            if (result.hasErrors()) {
                model.addAttribute("course", course);
                return "admin/add_subject";
            }
            
            subject.setCourse(course);
            subjectRepository.save(subject);
            redirectAttributes.addFlashAttribute("success", "Subject added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding subject: " + e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/subjects";
    }
    
    @GetMapping("/admin/students/{studentId}/grades")
    public String viewStudentGrades(@PathVariable Long studentId, Model model) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Grade> studentGrades = gradeRepository.findByStudentId(studentId);

        // Ensure 'equivalent' is calculated for each grade
        studentGrades.forEach(grade -> {
            double score = grade.getScore();
            if (score >= 97) {
                grade.setEquivalent(1.0);
            } else if (score >= 94) {
                grade.setEquivalent(1.25);
            } else if (score >= 91) {
                grade.setEquivalent(1.5);
            } else if (score >= 88) {
                grade.setEquivalent(1.75);
            } else if (score >= 85) {
                grade.setEquivalent(2.0);
            } else if (score >= 82) {
                grade.setEquivalent(2.25);
            } else if (score >= 79) {
                grade.setEquivalent(2.5);
            } else if (score >= 76) {
                grade.setEquivalent(2.75);
            } else if (score == 75) {
                grade.setEquivalent(3.0);
            } else if (score >= 65) {
                grade.setEquivalent(5.0);
            } else {
                grade.setEquivalent(null);
            }
        });

        // Group grades by school year and semester
        Map<Integer, Map<Integer, List<Grade>>> gradesByYearAndSemester = studentGrades.stream()
            .collect(Collectors.groupingBy(Grade::getSchoolYear,
                Collectors.groupingBy(Grade::getSemester)));

        // Calculate GWA for each semester
        Map<Integer, Map<Integer, Double>> gwaByYearAndSemester = gradesByYearAndSemester.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                yearEntry -> yearEntry.getValue().entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        semesterEntry -> {
                            List<Grade> grades = semesterEntry.getValue();
                            return grades.stream()
                                .filter(grade -> grade.getEquivalent() != null) // Exclude null equivalents
                                .mapToDouble(Grade::getEquivalent)
                                .average()
                                .orElse(0.0);
                        }
                    ))
            ));

        model.addAttribute("student", student);
        model.addAttribute("gradesByYearAndSemester", gradesByYearAndSemester);
        model.addAttribute("gwaByYearAndSemester", gwaByYearAndSemester);
        return "admin/student_grades";
    }

    @GetMapping("/admin/subjects/{subjectId}/grades")
    public String viewSubjectGrades(@PathVariable Long subjectId, Model model) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new RuntimeException("Subject not found"));
        
        // Get all grades for the subject
        List<Grade> subjectGrades = gradeRepository.findBySubjectId(subjectId);
        
        // Group grades by year and semester
        Map<Integer, Map<Integer, List<Grade>>> gradesByYearAndSemester = subjectGrades
            .stream()
            .collect(Collectors.groupingBy(Grade::getYear,
                Collectors.groupingBy(Grade::getSemester)));
        
        // Add current year for the dropdown
        int currentYear = LocalDate.now().getYear();
        model.addAttribute("currentYear", currentYear);
        
        model.addAttribute("subject", subject);
        model.addAttribute("gradesByYearAndSemester", gradesByYearAndSemester);
        return "admin/subject_grades";
    }

    @GetMapping("/admin/grades/edit/{gradeId}")
    public String showEditGradeForm(@PathVariable("gradeId") Long gradeId, Model model) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new RuntimeException("Grade not found"));
        model.addAttribute("grade", grade);
        model.addAttribute("subjects", subjectRepository.findAll());
        return "admin/edit_grade";
    }

    @PostMapping("/admin/grades/save")
    public String saveGrade(
            @ModelAttribute Grade grade,
            RedirectAttributes redirectAttributes) {
        
        // Manual validation
        if (grade.getScore() < 0 || grade.getScore() > 100) {
            redirectAttributes.addFlashAttribute("error", "Score must be between 0-100");
            return "redirect:/admin/grades/edit/" + grade.getId();
        }
        
        if (grade.getSemester() != 1 && grade.getSemester() != 2) {
            redirectAttributes.addFlashAttribute("error", "Semester must be 1 or 2");
            return "redirect:/admin/grades/edit/" + grade.getId();
        }
        
        try {
            gradeRepository.save(grade);
            redirectAttributes.addFlashAttribute("success", "Grade saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving grade: " + e.getMessage());
        }
        
        // Return to either student or subject grades view based on context
        if (grade.getStudent() != null) {
            return "redirect:/admin/students/" + grade.getStudent().getId() + "/grades";
        } else {
            return "redirect:/admin/subjects/" + grade.getSubject().getId() + "/grades";
        }
    }

    @PostMapping("/admin/grades/delete/{gradeId}")
    public String deleteGrade(@PathVariable Long gradeId, RedirectAttributes redirectAttributes) {
        try {
            Grade grade = gradeRepository.findById(gradeId).orElseThrow(() -> new RuntimeException("Grade not found"));
            Long studentId = grade.getStudent().getId();
            gradeRepository.deleteById(gradeId);
            redirectAttributes.addFlashAttribute("successMessage", "Grade deleted successfully!");
            return "redirect:/admin/students/" + studentId + "/grades";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting grade: " + e.getMessage());
            return "redirect:/admin/home";
        }
    }

    @PostMapping("/admin/courses/{courseId}/subjects/delete/{subjectId}")
    @Transactional
    public String deleteSubject(
            @PathVariable Long courseId,
            @PathVariable Long subjectId,
            RedirectAttributes redirectAttributes) {
        try {
            subjectRepository.deleteById(subjectId);
            redirectAttributes.addFlashAttribute("success", "Subject deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting subject: " + e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/subjects";
    }

    // Student Management - Updated with new fields
    @PostMapping("/admin/students/add")
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
            
            // Set enrollment date using the selected year (Jan 1st of selected year)
            student.setEnrollmentDate(LocalDate.of(enrollmentYear, 1, 1));
            
            // Set program
            student.setProgram(programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found")));
            
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding student: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    @GetMapping("/admin/students/edit/{id}")
    public String showEditStudentForm(@PathVariable Long id, Model model) {
        try {
            Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            model.addAttribute("student", student);
            model.addAttribute("programs", programRepository.findAll());
            
            // Add enrollment years for the dropdown (current year + 5 years)
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

    @PostMapping("/admin/students/edit/{id}")
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
            
            // Update enrollment date
            student.setEnrollmentDate(LocalDate.of(enrollmentYear, 1, 1));
            
            // Update program
            student.setProgram(programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found")));
            
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating student: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    @PostMapping("/admin/students/archive/{id}")
    public String archiveStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            student.setArchived(true);
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student archived successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error archiving student: " + e.getMessage());
        }
        return "redirect:/admin/home";
    }

    @PostMapping("/admin/students/unarchive/{id}")
    public String unarchiveStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            student.setArchived(false);
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student unarchived successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error unarchiving student: " + e.getMessage());
        }
        return "redirect:/admin/students/archived";
    }

    @GetMapping("/admin/students/archived")
    public String viewArchivedStudents(Model model) {
        List<Student> archivedStudents = studentRepository.findByArchivedTrue();
        model.addAttribute("archivedStudents", archivedStudents);
        return "admin/archived_students";
    }

    @PostMapping("/admin/students/{studentId}/add-sample-grades")
    public String addSampleGrades(@PathVariable Long studentId, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            Subject subject1 = subjectRepository.findById(1L).orElseThrow(() -> new RuntimeException("Subject 1 not found"));
            Subject subject2 = subjectRepository.findById(2L).orElseThrow(() -> new RuntimeException("Subject 2 not found"));

            Grade grade1 = new Grade();
            grade1.setStudent(student);
            grade1.setSubject(subject1);
            grade1.setYear(2023);
            grade1.setSemester(1);
            grade1.setScore(95);
            gradeRepository.save(grade1);

            Grade grade2 = new Grade();
            grade2.setStudent(student);
            grade2.setSubject(subject2);
            grade2.setYear(2023);
            grade2.setSemester(2);
            grade2.setScore(88);
            gradeRepository.save(grade2);

            redirectAttributes.addFlashAttribute("successMessage", "Sample grades added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding sample grades: " + e.getMessage());
        }
        return "redirect:/admin/students/" + studentId + "/grades";
    }

    @GetMapping("/admin/students/{studentId}/grades/add")
    public String showAddGradeForm(@PathVariable Long studentId, Model model) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        model.addAttribute("student", student);
        model.addAttribute("grade", new Grade());
        model.addAttribute("subjects", subjectRepository.findAll());
        return "admin/add_grade";
    }

    @PostMapping("/admin/students/{studentId}/grades/add")
    public String addGrade(@PathVariable Long studentId, @ModelAttribute Grade grade, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            grade.setStudent(student);
            gradeRepository.save(grade);
            redirectAttributes.addFlashAttribute("successMessage", "Grade added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding grade: " + e.getMessage());
        }
        return "redirect:/admin/students/" + studentId + "/grades";
    }

    @PostMapping("/admin/grades/edit/{gradeId}")
    public String editGrade(@PathVariable("gradeId") Long gradeId, @ModelAttribute Grade grade, RedirectAttributes redirectAttributes) {
        try {
            Grade existingGrade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Grade not found"));
            existingGrade.setSubject(grade.getSubject());
            existingGrade.setYear(grade.getYear());
            existingGrade.setSemester(grade.getSemester());
            existingGrade.setScore(grade.getScore());
            gradeRepository.save(existingGrade);
            redirectAttributes.addFlashAttribute("successMessage", "Grade updated successfully!");
            return "redirect:/admin/students/" + existingGrade.getStudent().getId() + "/grades";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating grade: " + e.getMessage());
            return "redirect:/admin/home";
        }
    }

    // Student routes
    @GetMapping("/student/login")
    public String studentLogin() {
        return "student/student_login";
    }

    @GetMapping("/student/home")
    @Transactional(readOnly = true)
    public String studentHome(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String studentNumber = auth.getName();
        
        Student student = studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        
        model.addAttribute("student", student);
        return "student/student_home";
    }
}