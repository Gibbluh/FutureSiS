package com.example.auth1.repository;

import com.example.auth1.model.Faculty;
import com.example.auth1.model.TeachingAssignment;
import com.example.auth1.model.SubjectSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {
    
    // Find by faculty
    List<TeachingAssignment> findByFacultyId(Long facultyId);
    
    // Find by faculty with subject section details
    @Query("SELECT ta FROM TeachingAssignment ta " +
           "JOIN FETCH ta.subjectSection ss " +
           "JOIN FETCH ss.subject s " +
           "JOIN FETCH ss.section sec " +
           "WHERE ta.faculty.id = :facultyId")
    List<TeachingAssignment> findByFacultyIdWithDetails(@Param("facultyId") Long facultyId);
    
    // Find by faculty, academic year, and semester
    @Query("SELECT ta FROM TeachingAssignment ta " +
           "JOIN FETCH ta.subjectSection ss " +
           "WHERE ta.faculty.id = :facultyId " +
           "AND ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    List<TeachingAssignment> findByFacultyIdAndAcademicYearAndSemester(
        @Param("facultyId") Long facultyId, 
        @Param("academicYear") String academicYear, 
        @Param("semester") Integer semester);
    
    // Find by subject section
    List<TeachingAssignment> findBySubjectSectionId(Long subjectSectionId);
    
    // Find by subject (through subject section)
    @Query("SELECT ta FROM TeachingAssignment ta " +
           "JOIN FETCH ta.subjectSection ss " +
           "WHERE ss.subject.id = :subjectId " +
           "AND ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    List<TeachingAssignment> findBySubjectIdAndAcademicYearAndSemester(
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);
    
    // Find by section (through subject section)
    @Query("SELECT ta FROM TeachingAssignment ta " +
           "JOIN FETCH ta.subjectSection ss " +
           "WHERE ss.section.id = :sectionId " +
           "AND ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    List<TeachingAssignment> findBySectionIdAndAcademicYearAndSemester(
        @Param("sectionId") Long sectionId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);
    
    // Find by subject name
    @Query("SELECT ta FROM TeachingAssignment ta " +
           "JOIN FETCH ta.subjectSection ss " +
           "JOIN FETCH ss.subject s " +
           "WHERE s.name LIKE %:subjectName%")
    List<TeachingAssignment> findBySubjectName(@Param("subjectName") String subjectName);
    
    // Find by faculty and subject section
    Optional<TeachingAssignment> findByFacultyIdAndSubjectSectionId(Long facultyId, Long subjectSectionId);
    
    // Check if faculty is assigned to a specific subject section
    boolean existsByFacultyIdAndSubjectSectionId(Long facultyId, Long subjectSectionId);
    
    // Delete methods
    @Modifying
    @Transactional
    void deleteByFacultyId(Long facultyId);
    
    @Modifying
    @Transactional
    void deleteByFaculty(Faculty faculty);

    @Modifying
    @Transactional
    void deleteBySubjectSectionId(Long subjectSectionId);

    Optional<TeachingAssignment> findBySubjectSection(SubjectSection subjectSection);
} 