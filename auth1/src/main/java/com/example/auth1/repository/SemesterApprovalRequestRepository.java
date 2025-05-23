package com.example.auth1.repository;

import com.example.auth1.model.SemesterApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterApprovalRequestRepository extends JpaRepository<SemesterApprovalRequest, Long> {
    
    @Query("SELECT r FROM SemesterApprovalRequest r WHERE r.student.id = :studentId AND r.academicYear = :academicYear AND r.semester = :semester")
    Optional<SemesterApprovalRequest> findByStudentIdAndAcademicYearAndSemester(
        @Param("studentId") Long studentId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester
    );

    @Query("SELECT r FROM SemesterApprovalRequest r WHERE r.student.id = :studentId ORDER BY r.requestDate DESC")
    List<SemesterApprovalRequest> findByStudentIdOrderByRequestDateDesc(
        @Param("studentId") Long studentId
    );

    @Query("SELECT r FROM SemesterApprovalRequest r WHERE r.status = 'PENDING' ORDER BY r.requestDate ASC")
    List<SemesterApprovalRequest> findAllPendingRequests();

    @Query("SELECT r FROM SemesterApprovalRequest r WHERE r.student.id = :studentId AND r.status = 'APPROVED' AND r.academicYear = :academicYear")
    List<SemesterApprovalRequest> findApprovedRequestsByStudentAndYear(
        @Param("studentId") Long studentId,
        @Param("academicYear") String academicYear
    );

    @Query("SELECT r FROM SemesterApprovalRequest r WHERE r.student.id = :studentId AND r.academicYear = :academicYear AND r.semester = :semester AND r.status IN ('PENDING', 'APPROVED')")
    Optional<SemesterApprovalRequest> findActiveByStudentIdAndAcademicYearAndSemester(
        @Param("studentId") Long studentId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester
    );
} 