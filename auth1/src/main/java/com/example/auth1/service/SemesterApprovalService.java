package com.example.auth1.service;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import org.springframework.cache.annotation.Cacheable;

@Service
public class SemesterApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(SemesterApprovalService.class);

    @Autowired
    private SemesterApprovalRequestRepository approvalRequestRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private SubjectSectionRepository subjectSectionRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Transactional
    public SemesterApprovalRequest createApprovalRequest(Long studentId, String academicYear, Integer semester) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        // 1. Find the latest APPROVED request for this student
        List<SemesterApprovalRequest> approvedRequests = approvalRequestRepository.findByStudentIdOrderByRequestDateDesc(studentId)
            .stream()
            .filter(r -> r.getStatus() == SemesterApprovalRequest.ApprovalStatus.APPROVED)
            .toList();
        int baseYearLevel;
        int baseSemester;
        String baseAcademicYear;
        if (!approvedRequests.isEmpty()) {
            SemesterApprovalRequest latest = approvedRequests.get(0);
            baseYearLevel = latest.getYearLevel();
            baseSemester = latest.getSemester();
            baseAcademicYear = latest.getAcademicYear();
        } else {
            // If no approved requests, use student record
            baseYearLevel = student.getEnrollmentYear();
            baseSemester = student.getCurrentSemester();
            baseAcademicYear = student.getAcademicYear();
        }

        // 2. Calculate the next semester/year
        int nextSemester, nextYearLevel;
        String nextAcademicYear;
        if (baseSemester == 1) {
            nextSemester = 2;
            nextYearLevel = baseYearLevel;
            nextAcademicYear = baseAcademicYear;
        } else {
            nextSemester = 1;
            nextYearLevel = baseYearLevel + 1;
            // Advance academic year string (e.g., 2025-2026 -> 2026-2027)
            try {
                int startYear = Integer.parseInt(baseAcademicYear.split("-")[0]);
                nextAcademicYear = (startYear + 1) + "-" + (startYear + 2);
            } catch (Exception e) {
                nextAcademicYear = baseAcademicYear; // fallback
            }
        }

        // 3. Check for any existing request for this student, year, yearLevel, and semester
        List<SemesterApprovalRequest> existingRequests = approvalRequestRepository
            .findAllByStudentIdAndAcademicYearAndYearLevelAndSemester(studentId, nextAcademicYear, nextYearLevel, nextSemester)
            .stream()
            .filter(r -> r.getStatus() == SemesterApprovalRequest.ApprovalStatus.PENDING)
            .toList();
        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("A request for this semester and year level is already pending. Please wait for admin action.");
        }

        // 4. Always check grades for the PREVIOUS semester/year/academicYear
        int prevSemester, prevYearLevel;
        String prevAcademicYear;
        if (nextSemester == 1) {
            prevSemester = 2;
            prevYearLevel = nextYearLevel - 1;
            // Previous academic year
            try {
                int startYear = Integer.parseInt(nextAcademicYear.split("-")[0]);
                prevAcademicYear = (startYear - 1) + "-" + (startYear);
            } catch (Exception e) {
                prevAcademicYear = nextAcademicYear;
            }
        } else {
            prevSemester = 1;
            prevYearLevel = nextYearLevel;
            prevAcademicYear = nextAcademicYear;
        }
        // Only require grades for the subjects in the student's actual section
        List<SubjectSection> prevSubjectSections = subjectSectionRepository.findBySectionIdAndAcademicYearAndSemester(
            student.getSection().getId(), prevAcademicYear, prevSemester
        );
        for (SubjectSection ss : prevSubjectSections) {
            Optional<Grade> grade = gradeRepository.findByStudentIdAndSubjectSectionId(
                    studentId,
                ss.getId()
                );
                if (grade.isEmpty() || grade.get().getRawGrade() == null) {
                    throw new RuntimeException("Not all grades are completed for the previous semester");
                }
            }
        // 5. Create and save the approval request
        SemesterApprovalRequest request = new SemesterApprovalRequest();
        request.setStudent(student);
        request.setAcademicYear(nextAcademicYear);
        request.setYearLevel(nextYearLevel);
        request.setSemester(nextSemester);
        request.setStatus(SemesterApprovalRequest.ApprovalStatus.PENDING);
        request.setRequestDate(java.time.LocalDateTime.now());
        try {
            return approvalRequestRepository.save(request);
        } catch (Exception e) {
            // Handle DB unique constraint violation gracefully
            throw new RuntimeException("A request for this semester already exists. Please wait for admin action.");
        }
    }

    @Transactional
    public SemesterApprovalRequest processApprovalRequest(Long requestId, boolean approved, String comments, Long adminId) {
        SemesterApprovalRequest request = approvalRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Approval request not found"));
        if (request.getStatus() != SemesterApprovalRequest.ApprovalStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }
        if (approved) {
            request.setStatus(SemesterApprovalRequest.ApprovalStatus.APPROVED);
            request.setComments(comments);
            request.setApprovalDate(java.time.LocalDateTime.now());
            Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
            request.setApprovedBy(admin);
            Student student = request.getStudent();
            int oldYearLevel = student.getEnrollmentYear();
            // Update student year/semester to match the approved request
            student.setEnrollmentYear(request.getYearLevel());
            student.setCurrentSemester(request.getSemester());
            // If year level increased, update academic year
            if (request.getYearLevel() > oldYearLevel) {
                student.setAcademicYear(request.getAcademicYear());
            }
            updateStudentSectionForNewYear(student);
            studentRepository.save(student);
            enrollStudentInNewSemesterSubjects(student, request.getAcademicYear());
        return approvalRequestRepository.save(request);
        } else {
            // If rejected, increment the student's rejectedApprovalCount and delete the request
            Student student = request.getStudent();
            student.setRejectedApprovalCount(student.getRejectedApprovalCount() + 1);
            studentRepository.save(student);
            approvalRequestRepository.delete(request);
            return null;
        }
    }

    private void updateStudentSectionForNewYear(Student student) {
        Program program = student.getProgram();
        Integer newYearLevel = student.getEnrollmentYear();
        if (program == null || newYearLevel == null) {
            logger.warn("Student {} missing program or year level.", student.getId());
            return;
        }
        String prefix = program.getAcronym() != null ? program.getAcronym() : program.getName();
        String currentSectionName = student.getSection() != null ? student.getSection().getName() : null;
        String currentSuffix = null;
        if (currentSectionName != null) {
            String[] parts = currentSectionName.split("-");
            if (parts.length == 3) {
                currentSuffix = parts[2];
            }
        }
        // Always look for the exact format: PREFIX-YEARLEVEL-SUFFIX
        if (currentSuffix != null) {
            String targetSectionName = prefix + "-" + newYearLevel + "-" + currentSuffix;
            Optional<Section> targetSection = sectionRepository.findByNameAndProgramId(targetSectionName, program.getId());
            if (targetSection.isPresent()) {
                student.setSection(targetSection.get());
                logger.info("Assigned section '{}' (exact match) to student {} for year level {}", targetSectionName, student.getId(), newYearLevel);
                return;
            }
        }
        // Fallback: try suffix 'A'
        String fallbackSectionName = prefix + "-" + newYearLevel + "-A";
        Optional<Section> fallbackSection = sectionRepository.findByNameAndProgramId(fallbackSectionName, program.getId());
        if (fallbackSection.isPresent()) {
            student.setSection(fallbackSection.get());
            logger.info("Assigned section '{}' (fallback A) to student {} for year level {}", fallbackSectionName, student.getId(), newYearLevel);
            return;
        }
        // Last fallback: any section for this program and year level
        List<Section> possibleSections = sectionRepository.findByProgramIdAndYearLevel(program.getId(), newYearLevel);
        if (!possibleSections.isEmpty()) {
            student.setSection(possibleSections.get(0));
            logger.info("Assigned fallback section '{}' to student {} for year level {}", possibleSections.get(0).getName(), student.getId(), newYearLevel);
        } else {
            logger.error("No section found for program {} and year level {}", program.getId(), newYearLevel);
        }
    }

    private void enrollStudentInNewSemesterSubjects(Student student, String academicYear) {
        if (student.getProgram() == null || student.getSection() == null) {
            return; // Cannot enroll without program and section
        }
        // Only enroll in subject sections for the student's section, year, semester, and academic year
        List<SubjectSection> sectionsToEnroll = subjectSectionRepository.findWithFilters(
            student.getProgram().getId(),
            student.getEnrollmentYear(),
            student.getCurrentSemester(),
            student.getSection().getId(),
            academicYear
        );
        for (SubjectSection ss : sectionsToEnroll) {
            // Only create a grade if one does not already exist for this student and subject section
            boolean alreadyEnrolled = student.getGrades().stream()
                .anyMatch(g -> g.getSubjectSection() != null && g.getSubjectSection().getId().equals(ss.getId()));
            if (!alreadyEnrolled) {
                Grade newGrade = new Grade(student, ss, null);
                gradeRepository.save(newGrade);
            }
        }
    }

    @Cacheable("pendingRequests")
    public List<SemesterApprovalRequest> getPendingRequests() {
        return approvalRequestRepository.findAllPendingRequests();
    }

    public List<SemesterApprovalRequest> getStudentRequests(Long studentId) {
        return approvalRequestRepository.findByStudentIdOrderByRequestDateDesc(studentId);
    }

    public static class NextSemesterInfo {
        public final int nextSemester;
        public final int nextYear;
        public final String nextAcademicYear;
        public NextSemesterInfo(int nextSemester, int nextYear, String nextAcademicYear) {
            this.nextSemester = nextSemester;
            this.nextYear = nextYear;
            this.nextAcademicYear = nextAcademicYear;
        }
    }

    public NextSemesterInfo getNextSemesterInfo(int currentSemester, int currentYear, String ignoredAcademicYear) {
        if (currentSemester == 1) {
            return new NextSemesterInfo(2, currentYear, null); // academicYear is always the student's original
        } else {
            return new NextSemesterInfo(1, currentYear + 1, null);
        }
    }

    public boolean canRequestNextSemester(Long studentId, String academicYear) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        Integer currentSemester = student.getCurrentSemester();
        Integer currentYear = student.getEnrollmentYear();

        // Only check grades for THIS student, current year and semester
        long totalSubjects = student.getGrades().stream()
            .filter(g -> g.getSubjectSection() != null
                && g.getSubjectSection().getSubject() != null
                && g.getSubjectSection().getSubject().getYearLevel() == currentYear
                && g.getSubjectSection().getSemester() == currentSemester)
            .count();
        long completedSubjects = student.getGrades().stream()
            .filter(g -> g.getSubjectSection() != null
                && g.getSubjectSection().getSubject() != null
                && g.getSubjectSection().getSubject().getYearLevel() == currentYear
                && g.getSubjectSection().getSemester() == currentSemester
                && g.getRawGrade() != null)
            .count();
        return totalSubjects > 0 && completedSubjects == totalSubjects;
    }

    @Transactional
    public void createApprovalRequest(Student student, String academicYear, String comments) {
        // Always use the next semester/year for the request
        Integer currentSemester = student.getCurrentSemester();
        Integer currentYear = student.getEnrollmentYear();
        NextSemesterInfo next = getNextSemesterInfo(currentSemester, currentYear, academicYear);
        // Check eligibility for the next semester (per-student only)
        if (!canRequestNextSemester(student.getId(), academicYear)) {
            throw new RuntimeException("Student is not eligible to request approval for the next semester yet.");
        }
        // Check for existing request for the next semester/year (per-student only)
        List<SemesterApprovalRequest> existingRequests = approvalRequestRepository
            .findAllByStudentIdAndAcademicYearAndSemester(student.getId(), next.nextAcademicYear, next.nextSemester)
            .stream()
            .filter(r -> r.getStatus() == SemesterApprovalRequest.ApprovalStatus.PENDING)
            .toList();
        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("A request for this semester is already pending. Please wait for admin action.");
        }
        // Create and save the approval request for the next semester/year
        SemesterApprovalRequest request = new SemesterApprovalRequest();
        request.setStudent(student);
        request.setAcademicYear(next.nextAcademicYear);
        request.setSemester(next.nextSemester);
        request.setComments(comments);
        request.setStatus(SemesterApprovalRequest.ApprovalStatus.PENDING);
        request.setRequestDate(LocalDateTime.now());
        try {
            approvalRequestRepository.save(request);
        } catch (Exception e) {
            throw new RuntimeException("A request for this semester already exists. Please wait for admin action.");
        }
    }

    private String getCurrentAcademicYear() {
        int year = java.time.Year.now().getValue();
        return year + "-" + (year + 1);
    }

    public Optional<SemesterApprovalRequest> getLatestRejectedRequest(Long studentId) {
        List<SemesterApprovalRequest> rejected = approvalRequestRepository.findRejectedRequestsByStudentIdOrderByRequestDateDesc(studentId);
        return rejected.isEmpty() ? Optional.empty() : Optional.of(rejected.get(0));
    }

    @Cacheable("subjectSectionList")
    public List<SubjectSection> getAllSubjectSectionsWithDetails() {
        return subjectSectionRepository.findAllWithDetails();
    }

    @Cacheable("facultyList")
    public List<Faculty> getAllFacultyWithPrograms() {
        return facultyRepository.findAllWithPrograms();
    }

    @Cacheable("subjectSectionListDTO")
    public java.util.List<com.example.auth1.model.SubjectSectionListDTO> getAllSubjectSectionListDTOs() {
        return subjectSectionRepository.findAllForListView();
    }
} 