package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import java.awt.Color;
import java.util.ArrayList;
import com.example.auth1.service.SemesterApprovalService;
import com.example.auth1.service.GraduationService;

@Controller
@RequestMapping("/student")
public class StudentController {
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);
    
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final GradeRepository gradeRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubjectSectionRepository subjectSectionRepository;
    private final SemesterApprovalService semesterApprovalService;
    private final GraduationService graduationService;

    public StudentController(StudentRepository studentRepository,
                           CourseRepository courseRepository,
                           GradeRepository gradeRepository,
                           TeachingAssignmentRepository teachingAssignmentRepository,
                           PasswordEncoder passwordEncoder,
                           SubjectSectionRepository subjectSectionRepository,
                           SemesterApprovalService semesterApprovalService,
                           GraduationService graduationService) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.gradeRepository = gradeRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.subjectSectionRepository = subjectSectionRepository;
        this.semesterApprovalService = semesterApprovalService;
        this.graduationService = graduationService;
    }

    @GetMapping("/home")
    public String studentHome(Model model) {
        try {
            // Get current student
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String studentNumber = auth.getName();
            Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            // Use the student's actual academic year
            String currentAcademicYear = student.getAcademicYear();
            int currentSemester = student.getCurrentSemester();

            // Check English Communication teaching assignments
            checkEnglishCommunicationAssignments(currentAcademicYear, currentSemester);

            // Get grades for current semester (only enrolled subjects)
            List<Grade> grades = gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
                student.getId(),
                currentAcademicYear,
                currentSemester
            );

            // Filter grades to only those matching the student's current year, section, and semester
            grades = grades.stream()
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

            // Create a list of course data with grades (only enrolled subjects)
            List<Map<String, Object>> currentCourses = grades.stream()
                .map(grade -> {
                    Subject subject = grade.getSubject();
                    String instructorName = getInstructorName(subject, currentAcademicYear, currentSemester);
                    SubjectSection subjectSection = grade.getSubjectSection();
                    String sectionName = subjectSection != null && subjectSection.getSection() != null ? subjectSection.getSection().getName() : "N/A";
                    String room = subjectSection != null && subjectSection.getRoom() != null ? subjectSection.getRoom() : "TBD";
                    String schedule = subjectSection != null && subjectSection.getSchedule() != null ? subjectSection.getSchedule() : "TBD";
                    Map<String, Object> courseData = Map.of(
                        "code", subject.getCode(),
                        "name", subject.getName(),
                        "instructor", instructorName,
                        "units", subject.getUnits(),
                        "section", sectionName,
                        "room", room,
                        "schedule", schedule,
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

            List<com.example.auth1.model.SemesterApprovalRequest> studentRequests = semesterApprovalService.getStudentRequests(student.getId());
            boolean hasPendingRequest = studentRequests.stream()
                .anyMatch(r -> r.getStatus() == com.example.auth1.model.SemesterApprovalRequest.ApprovalStatus.PENDING);
            boolean disableRequestButton = hasPendingRequest || !canRequestApproval;
            model.addAttribute("hasPendingRequest", hasPendingRequest);
            model.addAttribute("disableRequestButton", disableRequestButton);

            // Find the latest APPROVED semester approval request for the student
            String latestApprovedAcademicYear = studentRequests.stream()
                .filter(r -> r.getStatus() == com.example.auth1.model.SemesterApprovalRequest.ApprovalStatus.APPROVED)
                .sorted((r1, r2) -> r2.getRequestDate().compareTo(r1.getRequestDate()))
                .map(com.example.auth1.model.SemesterApprovalRequest::getAcademicYear)
                .findFirst()
                .orElse(null);
            model.addAttribute("latestApprovedAcademicYear", latestApprovedAcademicYear);

            // Add warning if the latest request was rejected
            boolean hasRejectedRequest = semesterApprovalService.getLatestRejectedRequest(student.getId()).isPresent();
            String latestRejectedRequestDate = semesterApprovalService.getLatestRejectedRequest(student.getId())
                .map(r -> r.getRequestDate().toString())
                .orElse(null);
            model.addAttribute("hasRejectedRequest", hasRejectedRequest);
            model.addAttribute("latestRejectedRequestDate", latestRejectedRequestDate);

            // Add graduation information
            boolean isGraduated = graduationService.isGraduated(student.getId());
            boolean canRequestGraduation = (student.getEnrollmentYear() == 4 && student.getCurrentSemester() == 2 && !isGraduated && !graduationService.hasPendingGraduationRequest(student.getId()));
            boolean hasPendingGraduationRequest = graduationService.hasPendingGraduationRequest(student.getId());
            boolean canSubmitGraduationRequest = graduationService.canRequestGraduation(student.getId());
            
            model.addAttribute("isGraduated", isGraduated);
            model.addAttribute("canRequestGraduation", canRequestGraduation);
            model.addAttribute("hasPendingGraduationRequest", hasPendingGraduationRequest);
            model.addAttribute("canSubmitGraduationRequest", canSubmitGraduationRequest);

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

    @GetMapping("/grades/previous")
    @ResponseBody
    public Map<String, Object> getPreviousGrades(@RequestParam int yearLevel, @RequestParam int semester, Principal principal) {
        String studentNumber = principal.getName();
        Student student = studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        Program program = student.getProgram();
        if (program == null) {
            return Map.of("grades", List.of(), "gwa", null);
        }
        // Find courses for the selected year and semester
        List<Course> courses = courseRepository.findByProgramIdAndYearAndSemester(
            program.getId(), yearLevel, semester
        );
        // Get grades for the selected year and semester
        String academicYear = getCurrentAcademicYearForYearLevel(student, yearLevel);
        List<Grade> grades = gradeRepository.findByStudentIdAndAcademicYearAndSemesterWithSubject(
            student.getId(), academicYear, semester
        );
        Map<Long, Grade> gradeMap = grades.stream()
            .collect(Collectors.toMap(
                grade -> grade.getSubject().getId(),
                grade -> grade
            ));
        List<Map<String, Object>> previousCourses = courses.stream()
            .flatMap(course -> course.getSubjects().stream())
            .map(subject -> {
                Grade grade = gradeMap.get(subject.getId());
                String instructorName = getInstructorName(subject, academicYear, semester);
                Map<String, Object> courseData = Map.of(
                    "code", subject.getCode(),
                    "name", subject.getName(),
                    "instructor", instructorName,
                    "units", subject.getUnits(),
                    "section", getSectionName(student, subject),
                    "rawGrade", getFormattedGrade(grade),
                    "letterGrade", getLetterGrade(grade),
                    "status", getGradeStatus(grade),
                    "numericGrade", grade != null && grade.getGwa() != null ? grade.getGwa() : null
                );
                return courseData;
            })
            .collect(Collectors.toList());
        // Calculate GWA for this period
        List<Double> gwaList = previousCourses.stream()
            .map(c -> (Double) c.get("numericGrade"))
            .filter(gwa -> gwa != null)
            .collect(Collectors.toList());
        Double gwa = null;
        if (!gwaList.isEmpty()) {
            gwa = gwaList.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        }
        return Map.of("grades", previousCourses, "gwa", gwa);
    }

    @GetMapping("/subjects-grades/all")
    @ResponseBody
    public Map<String, Object> getAllSubjectsAndGradesForStudent(Principal principal) {
        String studentNumber = principal.getName();
        Student student = studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        Program program = student.getProgram();
        if (program == null) {
            return Map.of("subjects", List.of());
        }
        List<Course> courses = program.getCourses();
        if (courses == null || courses.isEmpty()) {
            return Map.of("subjects", List.of());
        }
        // Get all grades for this student
        List<Grade> allGrades = gradeRepository.findByStudentId(student.getId());
        Map<Long, Grade> gradeMap = allGrades.stream()
            .collect(Collectors.toMap(
                grade -> grade.getSubject().getId(),
                grade -> grade
            ));
        List<Map<String, Object>> subjects = courses.stream()
            .flatMap(course -> course.getSubjects().stream().map(subject -> {
                Grade grade = gradeMap.get(subject.getId());
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("code", subject.getCode());
                map.put("name", subject.getName());
                map.put("yearLevel", course.getYear());
                map.put("semester", course.getSemester());
                map.put("units", subject.getUnits());
                map.put("section", student.getProgram().getName() + " " + course.getYear() + "-" + course.getSemester());
                map.put("instructor", "TBA");
                map.put("rawGrade", grade != null && grade.getRawGrade() != null ? getFormattedGrade(grade) : "N/A");
                map.put("letterGrade", grade != null && grade.getRawGrade() != null ? getLetterGrade(grade) : "N/A");
                map.put("status", grade != null && grade.getRawGrade() != null ? getGradeStatus(grade) : "N/A");
                map.put("numericGrade", grade != null && grade.getGwa() != null ? grade.getGwa() : null);
                return map;
            }))
            .collect(Collectors.toList());
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

    private String getCurrentAcademicYearForYearLevel(Student student, int yearLevel) {
        int startYear = student.getEnrollmentDate().getYear();
        int year = startYear + (yearLevel - 1);
        return year + "-" + (year + 1);
    }

    private String getInstructorName(Subject subject, String academicYear, Integer semester) {
        logger.info("Looking up instructor for subject: {} (ID: {}) in year {} semester {}", 
            subject.getName(), subject.getId(), academicYear, semester);
            
        List<SubjectSection> subjectSections = subjectSectionRepository.findBySubjectId(subject.getId());
        logger.info("Found {} teaching assignments for subject {}", subjectSections.size(), subject.getName());
        
        List<TeachingAssignment> assignments = new ArrayList<>();
        for (SubjectSection ss : subjectSections) {
            assignments.addAll(teachingAssignmentRepository.findBySubjectSectionId(ss.getId()));
        }
        
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

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("changePasswordError", "New passwords do not match!");
            return "student/student_home";
        }

        String username = principal.getName();
        Student student = studentRepository.findByStudentNumber(username).orElse(null);
        if (student == null) {
            model.addAttribute("changePasswordError", "Student not found.");
            return "student/student_home";
        }

        if (!passwordEncoder.matches(oldPassword, student.getPassword())) {
            model.addAttribute("changePasswordError", "Old password is incorrect.");
            return "student/student_home";
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        studentRepository.save(student);

        redirectAttributes.addFlashAttribute("message", "Password changed successfully!");
        return "redirect:/student/home";
    }

    @GetMapping("/download-form")
    public void downloadEnrolledForm(HttpServletResponse response, Principal principal) throws IOException {
        String studentNumber = principal.getName();
        Student student = studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        String currentAcademicYear = student.getAcademicYear();
        int currentSemester = student.getCurrentSemester();
        int yearLevel = student.getEnrollmentYear();
        List<Course> courses = courseRepository.findByProgramIdAndYearAndSemester(
            student.getProgram().getId(), yearLevel, currentSemester
        );
        List<Subject> subjects = courses.stream()
            .flatMap(course -> course.getSubjects().stream())
            .toList();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Certificate_of_Registration.pdf");
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();
            // Header with border
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[]{1, 3, 1});
            PdfPCell logoCell = new PdfPCell(new Phrase("[LOGO]", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            logoCell.setBorder(Rectangle.BOX);
            logoCell.setRowspan(3);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(logoCell);
            PdfPCell schoolCell = new PdfPCell();
            schoolCell.setBorder(Rectangle.BOX);
            schoolCell.addElement(new Paragraph("Republic of the Philippines", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            schoolCell.addElement(new Paragraph("POLYTECHNIC UNIVERSITY OF THE PHILIPPINES", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            schoolCell.addElement(new Paragraph("CERTIFICATE OF REGISTRATION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
            headerTable.addCell(schoolCell);
            PdfPCell qrCell = new PdfPCell(new Phrase("[QR]", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            qrCell.setBorder(Rectangle.BOX);
            qrCell.setRowspan(3);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(qrCell);
            document.add(headerTable);
            document.add(new Paragraph(" "));
            // Info rows
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new int[]{3, 3});
            infoTable.addCell(makeInfoCellBold(student.getLastName().toUpperCase() + ", " + student.getFirstName().toUpperCase()));
            infoTable.addCell(makeInfoCell("A.Y.: " + currentAcademicYear + "    TERM: " + (currentSemester == 1 ? "First Semester" : "Second Semester")));
            infoTable.addCell(makeInfoCell("" + student.getStudentNumber()));
            infoTable.addCell(makeInfoCell("PROGRAM CODE: [CODE]    PROGRAM: " + student.getProgram().getName()));
            document.add(infoTable);
            // Program description row
            PdfPTable descTable = new PdfPTable(1);
            descTable.setWidthPercentage(100);
            descTable.addCell(makeInfoCell("PROGRAM DESCRIPTION: " + student.getProgram().getName().toUpperCase() + " (PLACEHOLDER CAMPUS)"));
            document.add(descTable);
            // Details row
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.addCell(makeInfoCell("Campus: [PLACEHOLDER]    YEAR LEVEL: " + yearLevel + "    SECTION: [PLACEHOLDER]"));
            detailsTable.addCell(makeInfoCell("ADDRESS: [PLACEHOLDER]    CONTACT NO: [PLACEHOLDER]"));
            document.add(detailsTable);
            document.add(new Paragraph(" "));
            // Subjects Table (with as many columns as possible)
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            table.setWidths(new float[]{2.2f, 5.5f, 2.5f, 2.2f, 2.2f, 3.5f, 4.0f});
            table.addCell(makeHeaderCell("SUBJECT CODE"));
            table.addCell(makeHeaderCell("SUBJECT TITLE"));
            table.addCell(makeHeaderCell("SECTION CODE"));
            table.addCell(makeHeaderCell("TUITION UNITS"));
            table.addCell(makeHeaderCell("CREDITED UNITS"));
            table.addCell(makeHeaderCell("INSTRUCTOR"));
            table.addCell(makeHeaderCell("SCHEDULE"));
            for (Subject subject : subjects) {
                table.addCell(subject.getCode());
                table.addCell(subject.getName());
                table.addCell("[PLACEHOLDER]");
                table.addCell(String.valueOf(subject.getUnits()));
                table.addCell(String.valueOf(subject.getUnits()));
                String instructor = getInstructorName(subject, currentAcademicYear, currentSemester);
                table.addCell(instructor);
                table.addCell("[PLACEHOLDER]");
            }
            document.add(table);
            // Footer rows
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.addCell(makeInfoCell("MAX UNITS ALLOWED: [N/A]"));
            footerTable.addCell(makeInfoCell("TOTAL UNITS ENROLLED: " + subjects.stream().mapToInt(Subject::getUnits).sum()));
            footerTable.addCell(makeInfoCell("REGISTRAR: ___________________________"));
            document.add(footerTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("This document contains personal-identifiable information that is subject to Data Privacy.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));
            document.add(new Paragraph("Please keep this document protected and in a safe place.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));
            document.close();
        } catch (Exception e) {
            throw new IOException("Error generating PDF", e);
        }
    }

    private PdfPCell makeInfoCellBold(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell makeInfoCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell makeHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(230, 230, 230));
        return cell;
    }

    @PostMapping("/approvals/request")
    public String submitApprovalRequest(@RequestParam Long studentId,
                                        @RequestParam String academicYear,
                                        @RequestParam Integer semester,
                                        RedirectAttributes redirectAttributes) {
        try {
            semesterApprovalService.createApprovalRequest(studentId, academicYear, semester);
            redirectAttributes.addFlashAttribute("successMessage", "Request submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/home";
    }
} 