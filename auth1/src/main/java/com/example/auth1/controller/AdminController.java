package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import com.example.auth1.service.StudentNumberGenerator;
import com.example.auth1.service.SemesterApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final StudentRepository studentRepository;
    private final StudentNumberGenerator studentNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ProgramRepository programRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final SemesterApprovalService semesterApprovalService;

    public AdminController(StudentRepository studentRepository,
                        StudentNumberGenerator studentNumberGenerator,
                        PasswordEncoder passwordEncoder,
                        ProgramRepository programRepository,
                        FacultyRepository facultyRepository,
                        CourseRepository courseRepository,
                        SubjectRepository subjectRepository,
                        TeachingAssignmentRepository teachingAssignmentRepository,
                        SemesterApprovalService semesterApprovalService) {
        this.studentRepository = studentRepository;
        this.studentNumberGenerator = studentNumberGenerator;
        this.passwordEncoder = passwordEncoder;
        this.programRepository = programRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.semesterApprovalService = semesterApprovalService;
    }

    @GetMapping("/login")
    public String adminLogin() {
        return "admin/admin_login";
    }

    @GetMapping("/home")
    public String adminHome(Model model) {
        try {
            logger.debug("Attempting to load admin home page");
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            logger.info("Admin user '{}' accessing home page", username);

            List<Student> students = studentRepository.findAll();
            List<Program> programs = programRepository.findAll();
            List<Faculty> faculties = facultyRepository.findAll();
            
            logger.debug("Loaded {} students, {} programs, and {} faculty members", 
                students.size(), programs.size(), faculties.size());
            
            model.addAttribute("username", username);
            model.addAttribute("students", students);
            model.addAttribute("programs", programs);
            model.addAttribute("faculties", faculties);
            
            // Add enrollment years for the dropdown (current year + 5 years)
            int currentYear = LocalDate.now().getYear();
            List<Integer> enrollmentYears = IntStream.rangeClosed(currentYear - 4, currentYear + 5)
                .boxed().collect(Collectors.toList());
            model.addAttribute("enrollmentYears", enrollmentYears);
            
            // Add pending approvals for the dashboard tab
            model.addAttribute("pendingApprovals", semesterApprovalService.getPendingRequests());
            
            logger.debug("Successfully prepared admin home page model");
            return "admin/admin_home";
        } catch (Exception e) {
            logger.error("Error loading admin home page", e);
            throw e;
        }
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

    @GetMapping("/subjects/available")
    @ResponseBody
    public ResponseEntity<?> getAvailableSubjects(
            @RequestParam int yearLevel,
            @RequestParam int semester,
            @RequestParam String departments) {
        
        try {
            logger.debug("Fetching subjects for year: {}, semester: {}, departments: {}", 
                yearLevel, semester, departments);

            // Parse department IDs
            List<Long> departmentIds = Arrays.stream(departments.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

            // Get programs
            List<Program> programs = programRepository.findAllById(departmentIds);
            if (programs.isEmpty()) {
                return ResponseEntity.ok(Map.of("subjects", new ArrayList<>()));
            }

            // Get courses for these programs matching year and semester
            List<Course> courses = courseRepository.findByProgramInAndYearAndSemester(
                programs, yearLevel, semester);

            // Extract subjects from courses
            List<Map<String, Object>> subjectsList = new ArrayList<>();
            Map<Long, List<Map<String, Object>>> subjectsByProgram = new HashMap<>();
            
            for (Course course : courses) {
                Program program = course.getProgram();
                List<Map<String, Object>> programSubjects = subjectsByProgram
                    .computeIfAbsent(program.getId(), k -> new ArrayList<>());
                
                for (Subject subject : course.getSubjects()) {
                    Map<String, Object> subjectMap = new HashMap<>();
                    subjectMap.put("id", subject.getId());
                    subjectMap.put("code", subject.getCode());
                    subjectMap.put("name", subject.getName());
                    subjectMap.put("programId", program.getId());
                    subjectMap.put("programName", program.getName());
                    programSubjects.add(subjectMap);
                    subjectsList.add(subjectMap);
                }
            }

            // Create response with both formats for compatibility
            Map<String, Object> response = new HashMap<>();
            response.put("subjects", subjectsList); // For backward compatibility
            response.put("subjectsByProgram", subjectsByProgram); // New grouped format
            response.put("programs", programs.stream()
                .map(p -> Map.of(
                    "id", p.getId(),
                    "name", p.getName()
                ))
                .collect(Collectors.toList()));

            logger.debug("Found {} subjects across {} programs", 
                subjectsList.size(), subjectsByProgram.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching available subjects", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error fetching subjects: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/faculty/{id}")
    @ResponseBody
    public ResponseEntity<?> getFacultyDetails(@PathVariable Long id) {
        try {
            Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", faculty.getId());
            response.put("facultyId", faculty.getFacultyId());
            response.put("firstName", faculty.getFirstName());
            response.put("lastName", faculty.getLastName());
            response.put("email", faculty.getEmail());
            response.put("position", faculty.getPosition());
            response.put("phoneNumber", faculty.getPhoneNumber());
            response.put("address", faculty.getAddress());
            
            // Get program IDs
            List<Long> programIds = faculty.getFacultyPrograms().stream()
                .map(fp -> fp.getProgram().getId())
                .collect(Collectors.toList());
            response.put("programIds", programIds);
            
            // Get current subject assignments with year level and semester
            List<Map<String, Object>> subjects = faculty.getTeachingAssignments().stream()
                .map(assignment -> {
                    Map<String, Object> subjectMap = new HashMap<>();
                    Subject subject = assignment.getSubject();
                    Course course = subject.getCourse();
                    subjectMap.put("id", subject.getId());
                    subjectMap.put("code", subject.getCode());
                    subjectMap.put("name", subject.getName());
                    subjectMap.put("programId", course.getProgram().getId());
                    subjectMap.put("yearLevel", course.getYear());
                    subjectMap.put("semester", course.getSemester());
                    return subjectMap;
                })
                .collect(Collectors.toList());
            response.put("subjects", subjects);
            
            // Get year level and semester if set
            if (!faculty.getTeachingAssignments().isEmpty()) {
                TeachingAssignment firstAssignment = faculty.getTeachingAssignments().iterator().next();
                response.put("yearLevel", firstAssignment.getSubject().getCourse().getYear());
                response.put("semester", firstAssignment.getSubject().getCourse().getSemester());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching faculty details", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/faculty/update-admin/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> updateFaculty(
            @PathVariable Long id,
            @RequestParam String facultyId,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam String position,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam List<Long> programIds,
            @RequestParam(required = false) String subjectAssignments) {
        try {
            logger.info("Updating faculty with ID: {}", id);
            logger.debug("Subject assignments data: {}", subjectAssignments);
            
            // Load faculty with all relationships
            Faculty faculty = facultyRepository.findByIdWithTeachingAssignments(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            // Check if facultyId is unique (if changed)
            if (!faculty.getFacultyId().equals(facultyId) && 
                facultyRepository.existsByFacultyId(facultyId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Faculty ID already exists"));
            }
            
            // Update basic information
            faculty.setFacultyId(facultyId);
            faculty.setFirstName(firstName);
            faculty.setLastName(lastName);
            faculty.setEmail(email);
            faculty.setPosition(position);
            faculty.setPhoneNumber(phoneNumber);
            faculty.setAddress(address);
            
            // Update password if provided
            if (password != null && !password.trim().isEmpty()) {
                faculty.setPassword(passwordEncoder.encode(password));
            }
            
            // Clear existing programs and add new ones
            faculty.clearFacultyPrograms();
            List<Program> programs = programRepository.findAllById(programIds);
            for (Program program : programs) {
                FacultyProgram fp = new FacultyProgram();
                fp.setProgram(program);
                fp.setFaculty(faculty);
                faculty.getFacultyPrograms().add(fp);
            }
            faculty = facultyRepository.saveAndFlush(faculty);
            
            // --- MERGE teaching assignments instead of clearing all ---
            final String currentAcademicYear = getCurrentAcademicYear();
            // Parse new assignments from request
            List<Map<String, Object>> newAssignments = new java.util.ArrayList<>();
            if (subjectAssignments != null && !subjectAssignments.trim().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    newAssignments = mapper.readValue(subjectAssignments, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception e) {
                    logger.error("Error processing subject assignments: {}", e.getMessage(), e);
                    throw new RuntimeException("Error processing subject assignments: " + e.getMessage());
                }
            }
            // Build a set of (subjectId, semester) for new assignments
            java.util.Set<String> newAssignmentKeys = new java.util.HashSet<>();
            for (Map<String, Object> assignment : newAssignments) {
                Long subjectId = ((Number) assignment.get("id")).longValue();
                Integer semester = (Integer) assignment.get("semester");
                newAssignmentKeys.add(subjectId + ":" + semester);
            }
            // Remove only assignments for the current academic year that are not in the new list
            faculty.getTeachingAssignments().removeIf(ta ->
                ta.getAcademicYear().equals(currentAcademicYear) &&
                !newAssignmentKeys.contains(ta.getSubject().getId() + ":" + ta.getSemester())
            );
            // Add new assignments that don't already exist
            for (Map<String, Object> assignment : newAssignments) {
                Long subjectId = ((Number) assignment.get("id")).longValue();
                Integer semester = (Integer) assignment.get("semester");
                boolean alreadyAssigned = faculty.getTeachingAssignments().stream().anyMatch(ta ->
                    ta.getSubject().getId().equals(subjectId) &&
                    ta.getAcademicYear().equals(currentAcademicYear) &&
                    ta.getSemester().equals(semester)
                );
                if (!alreadyAssigned) {
                    Subject subject = subjectRepository.findById(subjectId)
                        .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectId));
                    // Check if subject belongs to one of the faculty's programs
                    boolean isValidSubject = programs.stream()
                        .anyMatch(program -> subject.getCourse().getProgram().getId().equals(program.getId()));
                    if (!isValidSubject) {
                        throw new RuntimeException("Subject " + subject.getCode() + " does not belong to any of the faculty's assigned programs");
                    }
                    TeachingAssignment teachingAssignment = new TeachingAssignment();
                    teachingAssignment.setSubject(subject);
                    teachingAssignment.setAcademicYear(currentAcademicYear);
                    teachingAssignment.setSemester(semester);
                    faculty.addTeachingAssignment(teachingAssignment);
                }
            }
            faculty = facultyRepository.save(faculty);
            logger.info("Successfully updated faculty. Teaching assignments count: {}", faculty.getTeachingAssignments().size());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Faculty updated successfully");
            response.put("faculty", Map.of(
                "id", faculty.getId(),
                "facultyId", faculty.getFacultyId(),
                "name", faculty.getFirstName() + " " + faculty.getLastName(),
                "departments", faculty.getFacultyPrograms().stream().map(fp -> fp.getProgram().getName()).collect(Collectors.toList()),
                "subjects", faculty.getTeachingAssignments().stream().map(ta -> Map.of(
                    "id", ta.getSubject().getId(),
                    "code", ta.getSubject().getCode(),
                    "name", ta.getSubject().getName(),
                    "semester", ta.getSemester()
                )).collect(Collectors.toList())
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating faculty: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Error updating faculty: " + e.getMessage()));
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