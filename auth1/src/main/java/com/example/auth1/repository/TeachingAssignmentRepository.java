package com.example.auth1.repository;

import com.example.auth1.model.Faculty;
import com.example.auth1.model.TeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {
    List<TeachingAssignment> findByFacultyId(Long facultyId);
    List<TeachingAssignment> findByFacultyIdAndAcademicYearAndSemester(Long facultyId, String academicYear, Integer semester);
    List<TeachingAssignment> findBySubjectId(Long subjectId);
    void deleteByFacultyId(Long facultyId);
    
    @Modifying
    @Transactional
    void deleteByFaculty(Faculty faculty);

    @Query("SELECT ta FROM TeachingAssignment ta " +
           "JOIN FETCH ta.faculty f " +
           "JOIN FETCH ta.subject s " +
           "WHERE s.name LIKE %:subjectName%")
    List<TeachingAssignment> findBySubjectName(@Param("subjectName") String subjectName);
} 