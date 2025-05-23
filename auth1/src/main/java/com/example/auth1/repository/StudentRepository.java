package com.example.auth1.repository;

import com.example.auth1.model.Student;
import com.example.auth1.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    // Exact match
    Optional<Student> findByStudentNumber(String studentNumber);
    
    // For flexible matching
    List<Student> findByStudentNumberContainingIgnoreCase(String partialNumber);

    // Find students enrolled in a subject for a specific academic year and semester
    @Query("SELECT DISTINCT s FROM Student s " +
           "JOIN s.program p " +
           "JOIN p.courses c " +
           "JOIN c.subjects sub " +
           "WHERE sub = :subject " +
           "AND s.enrollmentYear = c.year " +
           "AND c.semester = :semester " +
           "ORDER BY s.lastName, s.firstName")
    List<Student> findBySubjectAndYearAndSemester(
        @Param("subject") Subject subject,
        @Param("semester") Integer semester);

    Optional<Student> findByEmail(String email);
}