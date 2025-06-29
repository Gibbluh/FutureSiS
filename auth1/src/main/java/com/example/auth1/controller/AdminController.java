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
import org.springframework.data.domain.Sort;
import org.springframework.validation.BindingResult;
import com.example.auth1.service.StudentService;
import com.example.auth1.service.SectionService;
import com.example.auth1.service.EmailService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;
import com.example.auth1.model.FacultyProgram;

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
    private final SubjectSectionRepository subjectSectionRepository;
    private final SectionRepository sectionRepository;
    private final GradeRepository gradeRepository;
    private final AdminRepository adminRepository;
    private final StudentService studentService;
    private final SectionService sectionService;
    private final EmailService emailService;

    public AdminController(StudentRepository studentRepository,
                        StudentNumberGenerator studentNumberGenerator,
                        PasswordEncoder passwordEncoder,
                        ProgramRepository programRepository,
                        FacultyRepository facultyRepository,
                        CourseRepository courseRepository,
                        SubjectRepository subjectRepository,
                        TeachingAssignmentRepository teachingAssignmentRepository,
                        SemesterApprovalService semesterApprovalService,
                        SubjectSectionRepository subjectSectionRepository,
                        SectionRepository sectionRepository,
                        GradeRepository gradeRepository,
                        AdminRepository adminRepository,
                        StudentService studentService,
                        SectionService sectionService,
                        EmailService emailService) {
        this.studentRepository = studentRepository;
        this.studentNumberGenerator = studentNumberGenerator;
        this.passwordEncoder = passwordEncoder;
        this.programRepository = programRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.semesterApprovalService = semesterApprovalService;
        this.subjectSectionRepository = subjectSectionRepository;
        this.sectionRepository = sectionRepository;
        this.gradeRepository = gradeRepository;
        this.adminRepository = adminRepository;
        this.studentService = studentService;
        this.sectionService = sectionService;
        this.emailService = emailService;
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
            @RequestParam Long studentSectionId,
            @RequestParam(required = false) String academicYear,
            RedirectAttributes redirectAttributes) {
        try {
            studentService.addStudent(firstName, lastName, email, birthDate, password, programId, phoneNumber, address, enrollmentYear, studentSectionId, academicYear);
            redirectAttributes.addFlashAttribute("successMessage", "Student added successfully and enrolled in default subjects!");
        } catch (Exception e) {
            logger.error("Failed to add student. See stack trace for details:", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/students/edit/{id}")
    public String showEditStudentForm(@PathVariable Long id, Model model) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        String currentAcademicYear = getCurrentAcademicYear();
        
        // Pass all subject sections for maximum flexibility
        List<SubjectSection> allSubjectSections = subjectSectionRepository.findAllWithDetails();
        model.addAttribute("allSubjectSections", allSubjectSections);

        // Add unique academic years for the filter dropdown
        Set<String> academicYears = allSubjectSections.stream()
            .map(SubjectSection::getAcademicYear)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        model.addAttribute("academicYears", academicYears);

        // Add default academic year for the filter (safe for null section)
        String defaultAcademicYear = "";
        if (student.getGrades() != null && !student.getGrades().isEmpty()) {
            Grade firstGrade = student.getGrades().get(0);
            if (firstGrade != null && firstGrade.getSubjectSection() != null && firstGrade.getSubjectSection().getAcademicYear() != null) {
                defaultAcademicYear = firstGrade.getSubjectSection().getAcademicYear();
                }
            }
        model.addAttribute("defaultAcademicYear", defaultAcademicYear);

        // Get IDs of currently enrolled sections for this student
        Set<Long> enrolledSubjectSectionIds = student.getGrades().stream()
            .map(g -> g.getSubjectSection().getId())
            .collect(Collectors.toSet());
        model.addAttribute("enrolledSubjectSectionIds", enrolledSubjectSectionIds);

        model.addAttribute("student", student);
        model.addAttribute("programs", programRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll()); // For the main section dropdown

        return "admin/edit_student";
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
            @RequestParam(value = "studentSectionId") Long studentSectionId,
            @RequestParam(value = "subjectSectionIds", required = false) List<Long> subjectSectionIds,
            RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setBirthDate(java.time.LocalDate.parse(birthDate));
            if (password != null && !password.isEmpty()) {
                student.setPassword(passwordEncoder.encode(password));
            }
            student.setPhoneNumber(phoneNumber);
            student.setAddress(address);
            student.setEnrollmentYear(enrollmentYear);
            student.setEnrollmentDate(java.time.LocalDate.parse(enrollmentDate));
            student.setCurrentSemester(currentSemester);
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            student.setProgram(program);

            Section studentSection = sectionRepository.findById(studentSectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));
            student.setSection(studentSection);

            Set<Long> updatedSectionIds = subjectSectionIds != null ? new HashSet<>(subjectSectionIds) : new HashSet<>();
            
            // Remove grades for subjects that were unchecked
            student.getGrades().removeIf(grade -> !updatedSectionIds.contains(grade.getSubjectSection().getId()));
            
            // Add grades for newly checked subjects
            Set<Long> currentSectionIds = student.getGrades().stream()
                .map(g -> g.getSubjectSection().getId())
                .collect(Collectors.toSet());

            for (Long sectionId : updatedSectionIds) {
                if (!currentSectionIds.contains(sectionId)) {
                    SubjectSection subjectSection = subjectSectionRepository.findById(sectionId)
                        .orElseThrow(() -> new RuntimeException("SubjectSection not found"));
                    Grade newGrade = new Grade(student, subjectSection, null); // Assuming a constructor that takes student, subjectSection, and grade value
                    student.getGrades().add(newGrade);
                }
            }

            studentRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating student: " + e.getMessage());
        }
        return "redirect:/admin/students";
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
                    Subject subject = assignment.getSubjectSection().getSubject();
                    Course course = subject.getCourse();
                    subjectMap.put("id", subject.getId());
                    subjectMap.put("code", subject.getCode());
                    subjectMap.put("name", subject.getName());
                    subjectMap.put("programId", course.getProgram().getId());
                    subjectMap.put("yearLevel", course.getYear());
                    subjectMap.put("semester", assignment.getSubjectSection().getSemester());
                    subjectMap.put("sectionId", assignment.getSubjectSection().getSection().getId());
                    subjectMap.put("sectionName", assignment.getSubjectSection().getSection().getName());
                    return subjectMap;
                })
                .collect(Collectors.toList());
            response.put("subjects", subjects);
            
            // Get year level and semester if set
            if (!faculty.getTeachingAssignments().isEmpty()) {
                TeachingAssignment firstAssignment = faculty.getTeachingAssignments().iterator().next();
                response.put("yearLevel", firstAssignment.getSubjectSection().getSubject().getCourse().getYear());
                response.put("semester", firstAssignment.getSubjectSection().getSemester());
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
            Faculty faculty = facultyRepository.findByIdWithTeachingAssignments(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            if (!faculty.getFacultyId().equals(facultyId) && 
                facultyRepository.existsByFacultyId(facultyId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Faculty ID already exists"));
            }
            faculty.setFacultyId(facultyId);
            faculty.setFirstName(firstName);
            faculty.setLastName(lastName);
            faculty.setEmail(email);
            faculty.setPosition(position);
            faculty.setPhoneNumber(phoneNumber);
            faculty.setAddress(address);
            if (password != null && !password.trim().isEmpty()) {
                faculty.setPassword(passwordEncoder.encode(password));
            }
            faculty.clearFacultyPrograms();
            List<Program> programs = programRepository.findAllById(programIds);
            for (Program program : programs) {
                FacultyProgram fp = new FacultyProgram();
                fp.setProgram(program);
                fp.setFaculty(faculty);
                faculty.getFacultyPrograms().add(fp);
            }
            faculty = facultyRepository.saveAndFlush(faculty);
            final String currentAcademicYear = getCurrentAcademicYear();
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
            java.util.Set<String> newAssignmentKeys = new java.util.HashSet<>();
            for (Map<String, Object> assignment : newAssignments) {
                Long subjectId = ((Number) assignment.get("id")).longValue();
                Integer semester = (Integer) assignment.get("semester");
                newAssignmentKeys.add(subjectId + ":" + semester);
            }
            
            // Remove old assignments for current academic year
            faculty.getTeachingAssignments().removeIf(ta ->
                ta.getSubjectSection().getAcademicYear().equals(currentAcademicYear) &&
                !newAssignmentKeys.contains(ta.getSubjectSection().getSubject().getId() + ":" + ta.getSubjectSection().getSemester())
            );
            
            // Add new assignments
            for (Map<String, Object> assignment : newAssignments) {
                Long subjectId = ((Number) assignment.get("id")).longValue();
                Integer semester = (Integer) assignment.get("semester");
                boolean alreadyAssigned = faculty.getTeachingAssignments().stream().anyMatch(ta ->
                    ta.getSubjectSection().getSubject().getId().equals(subjectId) &&
                    ta.getSubjectSection().getAcademicYear().equals(currentAcademicYear) &&
                    ta.getSubjectSection().getSemester().equals(semester)
                );
                if (!alreadyAssigned) {
                    List<SubjectSection> subjectSections = subjectSectionRepository.findBySubjectIdAndAcademicYearAndSemester(
                        subjectId, currentAcademicYear, semester);
                    if (!subjectSections.isEmpty()) {
                        SubjectSection subjectSection = subjectSections.get(0);
                    TeachingAssignment teachingAssignment = new TeachingAssignment();
                        teachingAssignment.setFaculty(faculty);
                        teachingAssignment.setSubjectSection(subjectSection);
                    faculty.addTeachingAssignment(teachingAssignment);
                    }
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
                    "id", ta.getSubjectSection().getSubject().getId(),
                    "code", ta.getSubjectSection().getSubject().getCode(),
                    "name", ta.getSubjectSection().getSubject().getName(),
                    "semester", ta.getSubjectSection().getSemester()
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
        int month = LocalDate.now().getMonthValue();
        if (month < 8) { // Assuming academic year starts in August
            return (year - 1) + "-" + year;
        } else {
            return year + "-" + (year + 1);
        }
    }

    private Integer getCurrentSemester() {
        return 1; // Default to first semester
    }

    // Faculty Management
    @GetMapping("/faculty")
    public String manageFaculty(Model model) {
        model.addAttribute("faculties", semesterApprovalService.getAllFacultyWithPrograms());
        return "admin/manage_faculty";
    }

    @PostMapping("/faculty/add")
    public String addFaculty(@ModelAttribute Faculty faculty, @RequestParam String password, @RequestParam List<Long> programIds, RedirectAttributes redirectAttributes) {
        if (facultyRepository.findByFacultyId(faculty.getFacultyId()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Faculty with this ID already exists.");
            return "redirect:/admin/faculty/create";
        }
        faculty.setPassword(passwordEncoder.encode(password));

        Set<FacultyProgram> facultyPrograms = new HashSet<>();
        List<Program> programs = programRepository.findAllById(programIds);
        for (Program program : programs) {
            FacultyProgram facultyProgram = new FacultyProgram();
            facultyProgram.setFaculty(faculty);
            facultyProgram.setProgram(program);
            facultyPrograms.add(facultyProgram);
        }
        faculty.setFacultyPrograms(facultyPrograms);

        facultyRepository.save(faculty);
        // Send credentials email if email is provided
        if (faculty.getEmail() != null && !faculty.getEmail().trim().isEmpty()) {
            emailService.sendFacultyCredentialsEmail(faculty.getEmail(), faculty.getFacultyId(), password);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Faculty added successfully!");
        return "redirect:/admin/faculty";
    }

    @GetMapping("/faculty/edit/{id}")
    public String showEditFacultyForm(@PathVariable Long id, Model model) {
        Faculty faculty = facultyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid faculty Id:" + id));
        model.addAttribute("faculty", faculty);
        model.addAttribute("programs", programRepository.findAll());
        // Add a list of currently assigned program IDs for easier checking in Thymeleaf
        List<Long> assignedProgramIds = faculty.getFacultyPrograms().stream()
                .map(fp -> fp.getProgram().getId())
                .collect(Collectors.toList());
        model.addAttribute("assignedProgramIds", assignedProgramIds);
        return "admin/edit_faculty";
    }

    @PostMapping("/faculty/edit/{id}")
    public String updateFaculty(@PathVariable Long id, @ModelAttribute Faculty facultyDetails, @RequestParam(required = false) List<Long> programIds, RedirectAttributes redirectAttributes) {
        Faculty faculty = facultyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid faculty Id:" + id));
        faculty.setFacultyId(facultyDetails.getFacultyId());
        faculty.setEmail(facultyDetails.getEmail());
        faculty.setFirstName(facultyDetails.getFirstName());
        faculty.setLastName(facultyDetails.getLastName());
        faculty.setPosition(facultyDetails.getPosition());
        faculty.setPhoneNumber(facultyDetails.getPhoneNumber());
        faculty.setAddress(facultyDetails.getAddress());

        // Clear existing programs and add new ones
        faculty.getFacultyPrograms().clear();
        if (programIds != null) {
            List<Program> programs = programRepository.findAllById(programIds);
            for (Program program : programs) {
                FacultyProgram facultyProgram = new FacultyProgram();
                facultyProgram.setFaculty(faculty);
                facultyProgram.setProgram(program);
                faculty.getFacultyPrograms().add(facultyProgram);
            }
        }
        
        facultyRepository.save(faculty);
        redirectAttributes.addFlashAttribute("successMessage", "Faculty updated successfully!");
        return "redirect:/admin/faculty";
    }
    
    @GetMapping("/faculty/delete/{id}")
    public String deleteFaculty(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            facultyRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting faculty: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @GetMapping("/faculty/create")
    public String showAddFacultyForm(Model model) {
        model.addAttribute("faculty", new Faculty());
        model.addAttribute("programs", programRepository.findAll());
        return "admin/add_faculty";
    }

    // Section Management
    @GetMapping("/sections")
    public String manageSections(Model model) {
        model.addAttribute("sectionsData", sectionService.getAllSectionListDTOs());
        return "admin/manage_sections";
    }

    @GetMapping("/sections/edit-year")
    @Transactional(readOnly = true)
    public String showEditSectionYearForm(@RequestParam("id") Long sectionId, @RequestParam("academicYear") String academicYear, Model model) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid section Id:" + sectionId));
        
        model.addAttribute("section", section);
        model.addAttribute("academicYear", academicYear);
        return "admin/edit_section_year";
    }

    @PostMapping("/sections/edit-year")
    @Transactional
    public String updateSectionYear(@RequestParam("sectionId") Long sectionId,
                                  @RequestParam("oldAcademicYear") String oldAcademicYear,
                                  @RequestParam("newAcademicYear") String newAcademicYear,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (newAcademicYear == null || newAcademicYear.trim().isEmpty()) {
                throw new IllegalArgumentException("New academic year cannot be empty.");
            }
            subjectSectionRepository.updateAcademicYearForSection(sectionId, oldAcademicYear, newAcademicYear);
            redirectAttributes.addFlashAttribute("successMessage", "Academic year for the section has been updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating academic year: " + e.getMessage());
        }
        return "redirect:/admin/sections";
    }

    @GetMapping("/sections/{id}/edit")
    public String showEditSectionForm(@PathVariable("id") Long id, Model model) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid section Id:" + id));
        model.addAttribute("section", section);
        model.addAttribute("programs", programRepository.findAll());
        return "admin/edit_section";
    }

    @PostMapping("/sections/edit/{id}")
    public String updateSection(@PathVariable("id") Long id, @RequestParam String name, @RequestParam Long programId, @RequestParam Integer yearLevel, RedirectAttributes redirectAttributes) {
        try {
            sectionService.updateSection(id, name, programId, yearLevel);
        redirectAttributes.addFlashAttribute("successMessage", "Section updated successfully!");
        return "redirect:/admin/sections";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating section: " + e.getMessage());
            return "redirect:/admin/sections/edit/" + id;
        }
    }

    @GetMapping("/sections/create")
    public String showCreateSectionForm(Model model) {
        model.addAttribute("section", new Section());
        model.addAttribute("programs", programRepository.findAll(Sort.by("name")));
        return "admin/create_section";
    }

    @PostMapping("/sections/create")
    public String createSection(@ModelAttribute("section") Section section,
                                BindingResult bindingResult,
                                @RequestParam("programId") Long programId,
                                @RequestParam("yearLevel") Integer yearLevel,
                                RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("programs", programRepository.findAll(Sort.by("name")));
            return "admin/create_section";
        }

        try {
            sectionService.createSection(section.getName(), programId, yearLevel);
            redirectAttributes.addFlashAttribute("successMessage", "Section created successfully!");
        return "redirect:/admin/sections";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/sections/create";
        }
    }

    @GetMapping("/sections/{id}/students")
    public String viewSectionStudents(@PathVariable Long id, @RequestParam String academicYear, Model model) {
        Section section = sectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Section not found"));
        
        List<Student> students = gradeRepository.findDistinctStudentsBySectionAndAcademicYear(id, academicYear);

        model.addAttribute("section", section);
        model.addAttribute("academicYear", academicYear);
        model.addAttribute("students", students);
        return "admin/section_students";
    }

    // Subject Management
    @GetMapping("/subjects/create")
    public String showCreateSubjectForm(Model model) {
        model.addAttribute("subject", new Subject());
        model.addAttribute("programs", programRepository.findAll());
        model.addAttribute("courses", courseRepository.findAll());
        return "admin/create_subject";
    }

    @PostMapping("/subjects/create")
    public String createSubject(@ModelAttribute Subject subject,
                               @RequestParam Long programId,
                               @RequestParam Integer yearLevel,
                               @RequestParam Integer semester,
                               @RequestParam Long courseId,
                               RedirectAttributes redirectAttributes) {
        try {
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            subject.setProgram(program);
            subject.setYearLevel(yearLevel);
            subject.setSemester(semester);
            subject.setCourse(course);
            subjectRepository.save(subject);
            redirectAttributes.addFlashAttribute("successMessage", "Subject created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating subject: " + e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @GetMapping("/subjects")
    public String manageSubjects(Model model) {
        model.addAttribute("subjects", subjectRepository.findAll());
        return "admin/manage_subjects";
    }

    @GetMapping("/subjects/{id}/edit")
    public String showEditSubjectForm(@PathVariable Long id, Model model) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid subject Id:" + id));
        model.addAttribute("subject", subject);
        model.addAttribute("courses", courseRepository.findAll());
        return "admin/edit_subject";
    }

    @PostMapping("/subjects/{id}/edit")
    public String updateSubject(@PathVariable Long id, 
                              @ModelAttribute Subject subjectDetails, 
                              @RequestParam int year,
                              @RequestParam int semester,
                              RedirectAttributes redirectAttributes) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid subject Id:" + id));

        subject.setCode(subjectDetails.getCode());
        subject.setName(subjectDetails.getName());
        subject.setUnits(subjectDetails.getUnits());

        Program program = subject.getCourse().getProgram();
        
        Course course = courseRepository.findByProgramIdAndYearAndSemester(program.getId(), year, semester)
            .stream().findFirst().orElseGet(() -> {
                String courseName = String.format("%s - Year %d, Semester %d", program.getName(), year, semester);
                Course newCourse = new Course(courseName, year, semester, program);
                return courseRepository.save(newCourse);
            });

        subject.setCourse(course);
        subjectRepository.save(subject);
        
        redirectAttributes.addFlashAttribute("successMessage", "Subject updated successfully!");
        return "redirect:/admin/subjects";
    }

    @Transactional
    @PostMapping("/subjects/{id}/delete")
    public String deleteSubjectPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            subjectSectionRepository.deleteBySubjectId(id);
            subjectRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting subject: " + e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    // Program Management
    @GetMapping("/programs/add")
    public String showAddProgramForm(Model model) {
        model.addAttribute("program", new Program());
        return "admin/add_program";
    }

    @PostMapping("/programs/add")
    public String addProgram(@ModelAttribute Program program, RedirectAttributes redirectAttributes) {
        programRepository.save(program);
        // Create default courses for 1st to 4th year, 1st and 2nd semester
        for (int year = 1; year <= 4; year++) {
            for (int semester = 1; semester <= 2; semester++) {
                String courseName = String.format("%s - Year %d, Sem %d", program.getName(), year, semester);
                Course course = new Course(courseName, year, semester, program);
                courseRepository.save(course);
            }
        }
        redirectAttributes.addFlashAttribute("successMessage", "Program added successfully with default courses!");
        return "redirect:/admin/programs";
    }

    @GetMapping("/programs/edit/{id}")
    public String showEditProgramForm(@PathVariable Long id, Model model) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid program Id:" + id));
        model.addAttribute("program", program);
        return "admin/edit_program";
    }

    @PostMapping("/programs/edit/{id}")
    public String updateProgram(@PathVariable Long id, @ModelAttribute Program programDetails, RedirectAttributes redirectAttributes) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid program Id:" + id));
        program.setName(programDetails.getName());
        programRepository.save(program);
        redirectAttributes.addFlashAttribute("successMessage", "Program updated successfully!");
        return "redirect:/admin/programs";
    }

    @GetMapping("/programs/{id}/delete")
    public String deleteProgram(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            programRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Program deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    @GetMapping("/programs")
    public String managePrograms(Model model) {
        model.addAttribute("programs", programRepository.findAll());
        return "admin/manage_programs";
    }

    @GetMapping("/programs/{programId}/courses")
    @Transactional(readOnly = true)
    public String viewProgramCourses(@PathVariable Long programId, Model model) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new RuntimeException("Program not found"));
        List<Course> courses = courseRepository.findByProgramIdOrderByYearAscSemesterAsc(programId);
        
        model.addAttribute("program", program);
        model.addAttribute("courses", courses);
        return "admin/program_courses";
    }

    @GetMapping("/courses/{courseId}/subjects")
    @Transactional(readOnly = true)
    public String viewCourseSubjects(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        model.addAttribute("course", course);
        model.addAttribute("subjects", subjectRepository.findByCourseId(courseId));
        return "admin/course_subjects";
    }

    @GetMapping("/courses/{courseId}/subjects/add")
    public String showAddSubjectToCourseForm(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        Subject subject = new Subject();
        subject.setCourse(course);
        model.addAttribute("subject", subject);
        return "admin/add_subject_to_course";
    }

    @PostMapping("/courses/{courseId}/subjects/add")
    @Transactional
    public String addSubjectToCourse(@PathVariable Long courseId, @ModelAttribute Subject subject, RedirectAttributes redirectAttributes) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        subject.setCourse(course);
        subjectRepository.save(subject);
        redirectAttributes.addFlashAttribute("successMessage", "Subject added to course successfully!");
        return "redirect:/admin/programs/" + course.getProgram().getId() + "/courses";
    }

    @PostMapping("/courses/subjects/delete/{subjectId}")
    @Transactional
    public String deleteSubjectFromCourse(@PathVariable Long subjectId, RedirectAttributes redirectAttributes) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new RuntimeException("Subject not found"));
        Long programId = subject.getCourse().getProgram().getId();
        subjectRepository.deleteById(subjectId);
        redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully!");
        return "redirect:/admin/programs/" + programId + "/courses";
    }

    // Curriculum Management
//    @GetMapping("/curriculums")
//    public String manageCurriculums(Model model) {
//        model.addAttribute("programs", programRepository.findAll());
//        return "admin/manage_curriculums";
//    }

    // Subject Section Management
    @GetMapping("/subject-sections")
    public String manageSubjectSections(Model model) {
        model.addAttribute("subjectSections", semesterApprovalService.getAllSubjectSectionListDTOs());
        return "admin/subject_sections";
    }

    @GetMapping("/subject-sections/{id}/edit")
    public String showEditSubjectSectionForm(@PathVariable Long id, Model model) {
        SubjectSection subjectSection = subjectSectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject section Id:" + id));
        model.addAttribute("subjectSection", subjectSection);
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("faculties", facultyRepository.findAll());
        // Load the current faculty for this section to pre-select in the dropdown
        teachingAssignmentRepository.findBySubjectSection(subjectSection).ifPresent(ta -> {
            model.addAttribute("assignedFacultyId", ta.getFaculty().getId());
        });
        return "admin/edit_subject_section";
    }

    @PostMapping("/subject-sections/edit/{id}")
    public String updateSubjectSection(@PathVariable Long id, 
                                     @ModelAttribute SubjectSection subjectSectionDetails, 
                                     @RequestParam(value = "facultyId", required = false) Long facultyId, 
                                     @RequestParam("semester") Integer semester,
                                     RedirectAttributes redirectAttributes) {
        try {
            SubjectSection subjectSection = subjectSectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject Section not found with id " + id));

            // Manually update fields from the form
            subjectSection.setSubject(subjectSectionDetails.getSubject());
            subjectSection.setSection(subjectSectionDetails.getSection());
            subjectSection.setSchedule(subjectSectionDetails.getSchedule());
            subjectSection.setRoom(subjectSectionDetails.getRoom());
            subjectSection.setMaxStudents(subjectSectionDetails.getMaxStudents());
            subjectSection.setAcademicYear(subjectSectionDetails.getAcademicYear());
            subjectSection.setSemester(semester);

            subjectSectionRepository.save(subjectSection);

            // Handle Faculty Assignment
            Optional<TeachingAssignment> existingAssignment = teachingAssignmentRepository.findBySubjectSection(subjectSection);

            if (facultyId != null) {
                Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found with id " + facultyId));
                if (existingAssignment.isPresent()) {
                    TeachingAssignment ta = existingAssignment.get();
                    ta.setFaculty(faculty);
                    teachingAssignmentRepository.save(ta);
                } else {
                    teachingAssignmentRepository.save(new TeachingAssignment(faculty, subjectSection));
                }
            } else {
                // If no faculty is selected, remove any existing assignment
                existingAssignment.ifPresent(teachingAssignmentRepository::delete);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Subject section updated successfully!");
            return "redirect:/admin/subject-sections";
        } catch (Exception e) {
            logger.error("Error updating subject section: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating subject section: " + e.getMessage());
            return "redirect:/admin/subject-sections/edit/" + id;
        }
    }

    @GetMapping("/subject-sections/create")
    public String showCreateSubjectSectionForm(Model model) {
        model.addAttribute("subjectSection", new SubjectSection());
        model.addAttribute("programs", programRepository.findAll());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("faculties", facultyRepository.findAll());
        model.addAttribute("academicYear", getCurrentAcademicYear());
        return "admin/create_subject_section";
    }

    @PostMapping("/subject-sections/create")
    public String createSubjectSection(@RequestParam("subjectIds") List<Long> subjectIds,
                                     @RequestParam Long sectionId,
                                     @RequestParam String academicYear,
                                     @RequestParam Integer semester,
                                     @RequestParam(required = false) Long facultyId,
                                     RedirectAttributes redirectAttributes) {
        try {
            Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));
            int createdCount = 0;
            for (Long subjectId : subjectIds) {
                Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectId));
            SubjectSection subjectSection = new SubjectSection(subject, section, academicYear, semester);
            subjectSectionRepository.save(subjectSection);
            if (facultyId != null) {
                Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));
                TeachingAssignment ta = new TeachingAssignment(faculty, subjectSection);
                teachingAssignmentRepository.save(ta);
            }
                createdCount++;
            }
            redirectAttributes.addFlashAttribute("successMessage", createdCount + " Subject Section(s) created successfully!");
            return "redirect:/admin/subject-sections";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating subject section(s): " + e.getMessage());
            return "redirect:/admin/subject-sections/create";
        }
    }

    // Pending Approvals
    @GetMapping("/pending-approvals")
    public String showPendingApprovals(
            @RequestParam(value = "programId", required = false) Long programId,
            @RequestParam(value = "yearLevel", required = false) Integer yearLevel,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            Model model) {
        List<SemesterApprovalRequest> pendingRequests = semesterApprovalService.getPendingRequests();
        // Filtering logic
        if (programId != null) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getStudent().getProgram() != null && r.getStudent().getProgram().getId().equals(programId))
                .collect(Collectors.toList());
        }
        if (yearLevel != null) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getStudent().getEnrollmentYear() != null && r.getStudent().getEnrollmentYear().equals(yearLevel))
                .collect(Collectors.toList());
        }
        if (sectionId != null) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getStudent().getSection() != null && r.getStudent().getSection().getId().equals(sectionId))
                .collect(Collectors.toList());
        }
        if (academicYear != null && !academicYear.isEmpty()) {
            pendingRequests = pendingRequests.stream()
                .filter(r -> r.getAcademicYear() != null && r.getAcademicYear().equals(academicYear))
                .collect(Collectors.toList());
        }
        // For filter dropdowns
        model.addAttribute("programs", programRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("academicYears", pendingRequests.stream().map(SemesterApprovalRequest::getAcademicYear).distinct().collect(Collectors.toList()));
        model.addAttribute("yearLevels", List.of(1,2,3,4));
        model.addAttribute("selectedProgramId", programId);
        model.addAttribute("selectedYearLevel", yearLevel);
        model.addAttribute("selectedSectionId", sectionId);
        model.addAttribute("selectedAcademicYear", academicYear);
        model.addAttribute("pendingApprovals", pendingRequests);
        return "admin/pending_approvals";
    }

    @PostMapping("/pending-approvals/{requestId}/process")
    public String processApprovalRequest(
            @PathVariable Long requestId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comments,
            RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            
            Admin admin = this.adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

            semesterApprovalService.processApprovalRequest(requestId, approved, comments, admin.getId());
            
            String message = approved ? 
                "Request approved successfully" : 
                "Request rejected successfully";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/pending-approvals";
    }

    @GetMapping("/add-student")
    public String showAddStudentForm(Model model) {
        model.addAttribute("programs", programRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("student", new Student()); // Add empty student object for form binding
        return "admin/add_student";
    }

    @GetMapping("/students")
    public String manageStudents(Model model) {
        model.addAttribute("students", studentRepository.findByGraduationStatusNot(1));
        model.addAttribute("programs", programRepository.findAll());
        int currentYear = LocalDate.now().getYear();
        List<Integer> enrollmentYears = IntStream.rangeClosed(currentYear - 10, currentYear)
            .boxed().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        model.addAttribute("enrollmentYears", enrollmentYears);
        return "admin/manage_students";
    }

    @GetMapping("/students/graduated")
    public String manageGraduatedStudents(Model model) {
        model.addAttribute("students", studentRepository.findByGraduationStatus(1));
        model.addAttribute("programs", programRepository.findAll());
        int currentYear = LocalDate.now().getYear();
        List<Integer> enrollmentYears = IntStream.rangeClosed(currentYear - 10, currentYear)
            .boxed().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        model.addAttribute("enrollmentYears", enrollmentYears);
        return "admin/manage_graduated_students";
    }

    // This API endpoint is no longer needed with the new simplified approach
    /*
    @GetMapping("/api/subject-sections")
    @ResponseBody
    public List<SubjectSectionDTO> getFilteredSubjectSections(
            @RequestParam(value = "programId", required = false) String programIdStr,
            @RequestParam(value = "yearLevel", required = false) String yearLevelStr,
            @RequestParam(value = "semester", required = false) String semesterStr,
            @RequestParam(value = "sectionId", required = false) String sectionIdStr) {
        try {
            logger.info("API /api/subject-sections called with programId={}, yearLevel={}, semester={}, sectionId={}", programIdStr, yearLevelStr, semesterStr, sectionIdStr);
            Long programId = "all".equalsIgnoreCase(programIdStr) || programIdStr == null || programIdStr.isEmpty() ? null : Long.parseLong(programIdStr);
            Integer yearLevel = "all".equalsIgnoreCase(yearLevelStr) || yearLevelStr == null || yearLevelStr.isEmpty() ? null : Integer.parseInt(yearLevelStr);
            Integer semester = "all".equalsIgnoreCase(semesterStr) || semesterStr == null || semesterStr.isEmpty() ? null : Integer.parseInt(semesterStr);
            Long sectionId = "all".equalsIgnoreCase(sectionIdStr) || sectionIdStr == null || sectionIdStr.isEmpty() ? null : Long.parseLong(sectionIdStr);
            
            List<SubjectSection> sections = subjectSectionRepository.findWithFilters(programId, yearLevel, semester, sectionId, academicYear);
            
            List<SubjectSectionDTO> dtoList = sections.stream()
                .map(ss -> new SubjectSectionDTO(
                    ss.getId(),
                    ss.getSubject().getCode(),
                    ss.getSubject().getName(),
                    ss.getSection().getName()
                ))
                .collect(Collectors.toList());

            logger.info("API /api/subject-sections returning {} results", dtoList.size());
            return dtoList;
        } catch (Exception e) {
            logger.error("Error in /api/subject-sections: {}", e.getMessage(), e);
            // In a real app, you might throw a custom exception that an exception handler can format into a proper HTTP response.
            throw new RuntimeException("Error fetching subject sections: " + e.getMessage());
        }
    }
    */
}