package com.example.auth1.service;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class SemesterApprovalService {

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

    @Transactional
    public SemesterApprovalRequest createApprovalRequest(Long studentId, String academicYear, Integer semester) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if there's already a pending or approved request
        Optional<SemesterApprovalRequest> existingRequest = approvalRequestRepository
            .findActiveByStudentIdAndAcademicYearAndSemester(studentId, academicYear, semester);
        
        if (existingRequest.isPresent()) {
            throw new RuntimeException("A request for this semester is already pending or approved");
        }

        // Check if all grades are completed
        List<Course> courses = courseRepository.findByProgramIdAndYearAndSemester(
            student.getProgram().getId(),
            student.getEnrollmentYear(),
            semester
        );

        for (Course course : courses) {
            for (Subject subject : course.getSubjects()) {
                Optional<Grade> grade = gradeRepository.findByStudentIdAndSubjectId(
                    studentId,
                    subject.getId(),
                    academicYear,
                    semester
                );
                
                if (grade.isEmpty() || grade.get().getRawGrade() == null) {
                    throw new RuntimeException("Not all grades are completed for this semester");
                }
            }
        }

        // Create and save the approval request
        SemesterApprovalRequest request = new SemesterApprovalRequest(student, academicYear, semester);
        return approvalRequestRepository.save(request);
    }

    @Transactional
    public SemesterApprovalRequest processApprovalRequest(Long requestId, boolean approved, String comments, Long adminId) {
        SemesterApprovalRequest request = approvalRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Approval request not found"));

        if (request.getStatus() != SemesterApprovalRequest.ApprovalStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        request.setStatus(approved ? 
            SemesterApprovalRequest.ApprovalStatus.APPROVED : 
            SemesterApprovalRequest.ApprovalStatus.REJECTED);
        request.setComments(comments);
        request.setApprovalDate(java.time.LocalDateTime.now());
        
        if (approved) {
            Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
            request.setApprovedBy(admin);

            // If this is the second semester and it's approved, update the student's year level
            if (request.getSemester() == 2) {
                Student student = request.getStudent();
                student.setEnrollmentYear(student.getEnrollmentYear() + 1);
                student.setCurrentSemester(1);
                studentRepository.save(student);
            } else {
                // If it's first semester, just update the current semester
                Student student = request.getStudent();
                student.setCurrentSemester(2);
                studentRepository.save(student);
            }
        }

        return approvalRequestRepository.save(request);
    }

    public List<SemesterApprovalRequest> getPendingRequests() {
        return approvalRequestRepository.findAllPendingRequests();
    }

    public List<SemesterApprovalRequest> getStudentRequests(Long studentId) {
        return approvalRequestRepository.findByStudentIdOrderByRequestDateDesc(studentId);
    }

    public boolean canRequestNextSemester(Long studentId, String academicYear) {
        List<SemesterApprovalRequest> approvedRequests = approvalRequestRepository
            .findApprovedRequestsByStudentAndYear(studentId, academicYear);
        
        // Check if both semesters are approved for the current academic year
        return approvedRequests.size() == 2;
    }
} 