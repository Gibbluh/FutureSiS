package com.example.auth1.repository;

import com.example.auth1.model.Grade;
import com.example.auth1.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    
    // Find by student and academic year/semester (through subject section)
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.subjectSection ss " +
           "JOIN FETCH ss.subject s " +
           "WHERE g.student.id = :studentId " +
           "AND ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    List<Grade> findByStudentIdAndAcademicYearAndSemesterWithSubject(
        @Param("studentId") Long studentId, 
        @Param("academicYear") String academicYear, 
        @Param("semester") Integer semester);
        
    // Find by student and subject section
    @Query("SELECT g FROM Grade g " +
           "WHERE g.student.id = :studentId " +
           "AND g.subjectSection.id = :subjectSectionId")
    Optional<Grade> findByStudentIdAndSubjectSectionId(
        @Param("studentId") Long studentId, 
        @Param("subjectSectionId") Long subjectSectionId);

    // Find by student and subject (through subject section)
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.subjectSection ss " +
           "WHERE g.student.id = :studentId " +
           "AND ss.subject.id = :subjectId " +
           "AND ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    Optional<Grade> findByStudentIdAndSubjectId(
        @Param("studentId") Long studentId, 
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);

    // Find by student and subject only (legacy support)
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.subjectSection ss " +
           "WHERE g.student.id = :studentId " +
           "AND ss.subject.id = :subjectId")
    Optional<Grade> findByStudentIdAndSubjectIdOnly(
        @Param("studentId") Long studentId, 
        @Param("subjectId") Long subjectId);

    // Find all grades for a student
    List<Grade> findByStudentId(Long studentId);

    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.subjectSection ss " +
           "JOIN FETCH ss.subject s " +
           "WHERE g.student.id = :studentId")
    List<Grade> findAllByStudentIdWithDetails(@Param("studentId") Long studentId);

    // Find by subject section
    List<Grade> findBySubjectSectionId(Long subjectSectionId);

    // Find by subject (through subject section)
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "WHERE g.subjectSection.subject.id = :subjectId " +
           "AND g.subjectSection.academicYear = :academicYear " +
           "AND g.subjectSection.semester = :semester")
    List<Grade> findBySubjectIdAndAcademicYearAndSemester(
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);
        
    // Find by section (through subject section)
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "WHERE g.subjectSection.section.id = :sectionId " +
           "AND g.subjectSection.academicYear = :academicYear " +
           "AND g.subjectSection.semester = :semester")
    List<Grade> findBySectionIdAndAcademicYearAndSemester(
        @Param("sectionId") Long sectionId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);
        
    // Find by subject section with student details
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "WHERE g.subjectSection.id = :subjectSectionId")
    List<Grade> findBySubjectSectionIdWithStudentDetails(
        @Param("subjectSectionId") Long subjectSectionId);

    // Delete by subject section
    @Modifying
    @Transactional
    void deleteBySubjectSectionId(Long subjectSectionId);

    @Query("SELECT COUNT(DISTINCT g.student.id) FROM Grade g WHERE g.subjectSection.section.id = :sectionId AND g.subjectSection.academicYear = :academicYear")
    long countDistinctStudentsBySectionAndAcademicYear(@Param("sectionId") Long sectionId, @Param("academicYear") String academicYear);

    @Query("SELECT DISTINCT g.student FROM Grade g WHERE g.subjectSection.section.id = :sectionId AND g.subjectSection.academicYear = :academicYear")
    List<Student> findDistinctStudentsBySectionAndAcademicYear(@Param("sectionId") Long sectionId, @Param("academicYear") String academicYear);

    @Query("SELECT COUNT(g) FROM Grade g WHERE g.student.id = :studentId " +
           "AND g.subjectSection.subject.program.id = :programId " +
           "AND g.subjectSection.subject.yearLevel = :yearLevel " +
           "AND g.subjectSection.semester = :semester")
    long countSubjectsByStudentAndSemester(@Param("studentId") Long studentId,
                                           @Param("programId") Long programId,
                                           @Param("yearLevel") int yearLevel,
                                           @Param("semester") int semester);

    @Query("SELECT COUNT(g) FROM Grade g WHERE g.student.id = :studentId " +
           "AND g.subjectSection.academicYear = :academicYear " +
           "AND g.subjectSection.semester = :semester " +
           "AND g.rawGrade IS NOT NULL")
    long countCompletedGradesByStudentAndSemester(@Param("studentId") Long studentId,
                                                  @Param("academicYear") String academicYear,
                                                  @Param("semester") int semester);

    @Query("SELECT COUNT(g) FROM Grade g WHERE g.student.id = :studentId " +
           "AND g.subjectSection.academicYear = :academicYear " +
           "AND g.subjectSection.semester = :semester")
    long countEnrolledSubjectsByStudentAndSemester(@Param("studentId") Long studentId, @Param("academicYear") String academicYear, @Param("semester") int semester);
} 