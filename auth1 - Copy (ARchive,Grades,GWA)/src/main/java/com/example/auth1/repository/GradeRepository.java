package com.example.auth1.repository;

import java.util.List;
import com.example.auth1.model.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findBySubjectId(Long subjectId);
    List<Grade> findByStudentId(Long studentId); // Add this method
    Grade findByStudentIdAndSubjectIdAndYearAndSemester(
        Long studentId, Long subjectId, int year, int semester);
    
    @Query("SELECT g FROM Grade g WHERE g.subject.id = :subjectId AND g.year = :year AND g.semester = :semester")
    List<Grade> findBySubjectAndSemester(
        @Param("subjectId") Long subjectId,
        @Param("year") int year,
        @Param("semester") int semester);
}