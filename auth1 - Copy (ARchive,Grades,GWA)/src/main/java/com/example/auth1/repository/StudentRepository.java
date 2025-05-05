package com.example.auth1.repository;

import com.example.auth1.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    // Exact match
    Optional<Student> findByStudentNumber(String studentNumber);
    
    // For flexible matching
    List<Student> findByStudentNumberContainingIgnoreCase(String partialNumber);

    List<Student> findByArchivedTrue();

    List<Student> findByArchivedFalse();
}