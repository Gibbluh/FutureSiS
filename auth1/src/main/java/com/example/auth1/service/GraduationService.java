package com.example.auth1.service;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class GraduationService {

    @Autowired
    private GraduationRequestRepository graduationRequestRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private SubjectSectionRepository subjectSectionRepository;

    @Transactional
    public GraduationRequest createGraduationRequest(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if student is in 4th year, 2nd semester
        if (student.getEnrollmentYear() != 4 || student.getCurrentSemester() != 2) {
            throw new RuntimeException("Student must be in 4th year, 2nd semester to request graduation");
        }

        // Check if student already has a pending graduation request
        Optional<GraduationRequest> existingPending = graduationRequestRepository
            .findPendingRequestByStudentId(studentId);
        if (existingPending.isPresent()) {
            throw new RuntimeException("A graduation request is already pending for this student");
        }

        // Check if student is already graduated
        if (student.getGraduationStatus() == 1) {
            throw new RuntimeException("Student has already graduated");
        }

        // Check if all grades are completed for the current semester
        List<SubjectSection> currentSubjectSections = subjectSectionRepository
            .findBySectionIdAndAcademicYearAndSemester(
                student.getSection().getId(), 
                student.getAcademicYear(), 
                student.getCurrentSemester()
            );

        for (SubjectSection ss : currentSubjectSections) {
            Optional<Grade> grade = gradeRepository.findByStudentIdAndSubjectSectionId(
                studentId, ss.getId()
            );
            if (grade.isEmpty() || grade.get().getRawGrade() == null) {
                throw new RuntimeException("Not all grades are completed for the current semester");
            }
        }

        // Create graduation request
        GraduationRequest request = new GraduationRequest(student);
        return graduationRequestRepository.save(request);
    }

    @Transactional
    public GraduationRequest processGraduationRequest(Long requestId, boolean approved, String comments) {
        GraduationRequest request = graduationRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Graduation request not found"));

        if (request.getStatus() != GraduationRequest.ApprovalStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        if (approved) {
            request.setStatus(GraduationRequest.ApprovalStatus.APPROVED);
            request.setComments(comments);
            request.setApprovalDate(LocalDateTime.now());
            // Update student graduation status
            Student student = request.getStudent();
            student.setGraduationStatus(1);
            studentRepository.save(student);
            return graduationRequestRepository.save(request);
        } else {
            request.setStatus(GraduationRequest.ApprovalStatus.REJECTED);
            request.setComments(comments);
            request.setApprovalDate(LocalDateTime.now());
            return graduationRequestRepository.save(request);
        }
    }

    public List<GraduationRequest> getPendingRequests() {
        return graduationRequestRepository.findAllPendingRequests();
    }

    public List<GraduationRequest> getStudentRequests(Long studentId) {
        return graduationRequestRepository.findByStudentIdOrderByRequestDateDesc(studentId);
    }

    public boolean canRequestGraduation(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        // Must be in 4th year, 2nd semester
        if (student.getEnrollmentYear() != 4 || student.getCurrentSemester() != 2) {
            return false;
        }

        // Must not already be graduated
        if (student.getGraduationStatus() == 1) {
            return false;
        }

        // Must not have a pending request
        Optional<GraduationRequest> pendingRequest = graduationRequestRepository
            .findPendingRequestByStudentId(studentId);
        if (pendingRequest.isPresent()) {
            return false;
        }

        // Must have all grades completed for current semester
        List<SubjectSection> currentSubjectSections = subjectSectionRepository
            .findBySectionIdAndAcademicYearAndSemester(
                student.getSection().getId(), 
                student.getAcademicYear(), 
                student.getCurrentSemester()
            );

        for (SubjectSection ss : currentSubjectSections) {
            Optional<Grade> grade = gradeRepository.findByStudentIdAndSubjectSectionId(
                studentId, ss.getId()
            );
            if (grade.isEmpty() || grade.get().getRawGrade() == null) {
                return false;
            }
        }

        return true;
    }

    public boolean isGraduated(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        return student.getGraduationStatus() == 1;
    }

    public boolean hasPendingGraduationRequest(Long studentId) {
        Optional<GraduationRequest> pendingRequest = graduationRequestRepository
            .findPendingRequestByStudentId(studentId);
        return pendingRequest.isPresent();
    }
} 