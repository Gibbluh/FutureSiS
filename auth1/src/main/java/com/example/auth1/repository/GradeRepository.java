package com.example.auth1.repository;

import com.example.auth1.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    @Query("SELECT g FROM Grade g WHERE g.student.id = :studentId AND g.academicYear = :academicYear AND g.semester = :semester")
    List<Grade> findByStudentIdAndAcademicYearAndSemesterWithSubject(
        @Param("studentId") Long studentId, 
        @Param("academicYear") String academicYear, 
        @Param("semester") Integer semester);
        
    @Query("SELECT g FROM Grade g WHERE g.student.id = :studentId AND g.subject.id = :subjectId AND g.academicYear = :academicYear AND g.semester = :semester")
    Optional<Grade> findByStudentIdAndSubjectId(
        @Param("studentId") Long studentId, 
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);

    @Query("SELECT g FROM Grade g WHERE g.student.id = :studentId AND g.subject.id = :subjectId")
    Optional<Grade> findByStudentIdAndSubjectIdOnly(
        @Param("studentId") Long studentId, 
        @Param("subjectId") Long subjectId);

    List<Grade> findByStudentId(Long studentId);

    @Query("SELECT g FROM Grade g " +
           "WHERE g.subject.id = :subjectId " +
           "AND g.academicYear = :academicYear " +
           "AND g.semester = :semester")
    List<Grade> findBySubjectIdAndAcademicYearAndSemester(
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);
} 