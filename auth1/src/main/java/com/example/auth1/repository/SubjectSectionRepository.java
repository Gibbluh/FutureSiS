package com.example.auth1.repository;

import com.example.auth1.model.SubjectSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectSectionRepository extends JpaRepository<SubjectSection, Long> {
    
    // Find by subject
    List<SubjectSection> findBySubjectId(Long subjectId);
    
    // Find by section
    List<SubjectSection> findBySectionId(Long sectionId);
    
    // Find by subject and section
    Optional<SubjectSection> findBySubjectIdAndSectionId(Long subjectId, Long sectionId);
    
    // Find by subject, section, academic year, and semester
    Optional<SubjectSection> findBySubjectIdAndSectionIdAndAcademicYearAndSemester(
        Long subjectId, Long sectionId, String academicYear, Integer semester);
    
    // Find by academic year and semester
    List<SubjectSection> findByAcademicYearAndSemester(String academicYear, Integer semester);
    
    // Find by subject, academic year, and semester
    List<SubjectSection> findBySubjectIdAndAcademicYearAndSemester(Long subjectId, String academicYear, Integer semester);
    
    // Find by section, academic year, and semester
    List<SubjectSection> findBySectionIdAndAcademicYearAndSemester(Long sectionId, String academicYear, Integer semester);
    
    // Find by program (through section)
    @Query("SELECT ss FROM SubjectSection ss " +
           "JOIN ss.section s " +
           "WHERE s.program.id = :programId " +
           "AND ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    List<SubjectSection> findByProgramIdAndAcademicYearAndSemester(
        @Param("programId") Long programId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);
    
    // Find all with subject, section, and teaching assignment details
    @Query("SELECT DISTINCT ss FROM SubjectSection ss " +
           "JOIN FETCH ss.subject s " +
           "JOIN FETCH ss.section sec " +
           "LEFT JOIN FETCH ss.teachingAssignments ta " +
           "LEFT JOIN FETCH ta.faculty f " +
           "WHERE ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    List<SubjectSection> findByAcademicYearAndSemesterWithDetails(
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);
    
    // Find by subject with section details
    @Query("SELECT ss FROM SubjectSection ss " +
           "JOIN FETCH ss.section s " +
           "WHERE ss.subject.id = :subjectId " +
           "AND ss.academicYear = :academicYear " +
           "AND ss.semester = :semester")
    List<SubjectSection> findBySubjectIdAndAcademicYearAndSemesterWithSectionDetails(
        @Param("subjectId") Long subjectId,
        @Param("academicYear") String academicYear,
        @Param("semester") Integer semester);

    @Query("SELECT ss FROM SubjectSection ss " +
           "JOIN ss.subject s " +
           "WHERE (:programId IS NULL OR (s.program IS NOT NULL AND s.program.id = :programId)) " +
           "AND (:yearLevel IS NULL OR s.yearLevel = :yearLevel) " +
           "AND (:semester IS NULL OR ss.semester = :semester) " +
           "AND (:sectionId IS NULL OR ss.section.id = :sectionId) " +
           "AND (:academicYear IS NULL OR ss.academicYear = :academicYear)")
    List<SubjectSection> findWithFilters(
            @Param("programId") Long programId,
            @Param("yearLevel") Integer yearLevel,
            @Param("semester") Integer semester,
            @Param("sectionId") Long sectionId,
            @Param("academicYear") String academicYear
    );

    @Query("SELECT DISTINCT s.section, s.academicYear FROM SubjectSection s ORDER BY s.academicYear DESC, s.section.name ASC")
    List<Object[]> findDistinctSectionAndAcademicYear();

    @Modifying
    @Query("UPDATE SubjectSection ss SET ss.academicYear = :newAcademicYear WHERE ss.section.id = :sectionId AND ss.academicYear = :oldAcademicYear")
    void updateAcademicYearForSection(@Param("sectionId") Long sectionId, @Param("oldAcademicYear") String oldAcademicYear, @Param("newAcademicYear") String newAcademicYear);

    @Query("SELECT ss FROM SubjectSection ss " +
           "JOIN FETCH ss.subject s " +
           "JOIN FETCH s.program " +
           "JOIN FETCH ss.section")
    List<SubjectSection> findAllWithDetails(); // Recommend caching this in the service layer with @Cacheable

    @Query("SELECT new com.example.auth1.model.SubjectSectionListDTO(ss.id, s.code, s.name, sec.name, CONCAT(f.firstName, ' ', f.lastName), ss.academicYear, ss.semester, ss.schedule, ss.room, ss.maxStudents) " +
           "FROM SubjectSection ss " +
           "JOIN ss.subject s " +
           "JOIN ss.section sec " +
           "LEFT JOIN ss.teachingAssignments ta " +
           "LEFT JOIN ta.faculty f ")
    java.util.List<com.example.auth1.model.SubjectSectionListDTO> findAllForListView();

    @Modifying
    void deleteBySubjectId(Long subjectId);
} 