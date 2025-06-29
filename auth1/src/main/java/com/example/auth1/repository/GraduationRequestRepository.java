package com.example.auth1.repository;

import com.example.auth1.model.GraduationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GraduationRequestRepository extends JpaRepository<GraduationRequest, Long> {
    
    @Query("SELECT r FROM GraduationRequest r WHERE r.status = 'PENDING' ORDER BY r.requestDate ASC")
    List<GraduationRequest> findAllPendingRequests();

    @Query("SELECT r FROM GraduationRequest r WHERE r.student.id = :studentId ORDER BY r.requestDate DESC")
    List<GraduationRequest> findByStudentIdOrderByRequestDateDesc(
        @Param("studentId") Long studentId
    );

    @Query("SELECT r FROM GraduationRequest r WHERE r.student.id = :studentId AND r.status = 'PENDING'")
    Optional<GraduationRequest> findPendingRequestByStudentId(
        @Param("studentId") Long studentId
    );

    @Query("SELECT r FROM GraduationRequest r WHERE r.student.id = :studentId AND r.status = 'APPROVED'")
    Optional<GraduationRequest> findApprovedRequestByStudentId(
        @Param("studentId") Long studentId
    );
} 