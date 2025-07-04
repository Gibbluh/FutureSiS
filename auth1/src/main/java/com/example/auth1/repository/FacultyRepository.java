package com.example.auth1.repository;

import com.example.auth1.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    @Query("SELECT DISTINCT f FROM Faculty f " +
           "LEFT JOIN FETCH f.facultyPrograms fp " +
           "LEFT JOIN FETCH fp.program p " +
           "WHERE f.facultyId = :facultyId")
    Optional<Faculty> findByFacultyIdWithPrograms(@Param("facultyId") String facultyId);

    @Query("SELECT DISTINCT f FROM Faculty f " +
           "LEFT JOIN FETCH f.teachingAssignments ta " +
           "LEFT JOIN FETCH ta.subjectSection ss " +
           "LEFT JOIN FETCH ss.subject s " +
           "LEFT JOIN FETCH s.course c " +
           "LEFT JOIN FETCH c.program p " +
           "WHERE f.facultyId = :facultyId " +
           "ORDER BY c.year, c.semester")
    Optional<Faculty> findByFacultyIdWithTeachingAssignments(@Param("facultyId") String facultyId);

    @Query("SELECT DISTINCT f FROM Faculty f " +
           "LEFT JOIN FETCH f.facultyPrograms fp " +
           "LEFT JOIN FETCH fp.program p " +
           "WHERE f.id = :id")
    Optional<Faculty> findByIdWithPrograms(@Param("id") Long id);

    @Query("SELECT DISTINCT f FROM Faculty f " +
           "LEFT JOIN FETCH f.teachingAssignments ta " +
           "LEFT JOIN FETCH ta.subjectSection ss " +
           "LEFT JOIN FETCH ss.subject s " +
           "LEFT JOIN FETCH s.course c " +
           "WHERE f.id = :id")
    Optional<Faculty> findByIdWithTeachingAssignments(@Param("id") Long id);

    Optional<Faculty> findByFacultyId(String facultyId);
    boolean existsByFacultyId(String facultyId);
    Optional<Faculty> findByEmail(String email);

    @Query("SELECT DISTINCT f FROM Faculty f LEFT JOIN FETCH f.facultyPrograms")
    List<Faculty> findAllWithPrograms(); // Recommend caching this in the service layer with @Cacheable

    List<Faculty> findByFacultyPrograms_Program_Id(Long programId);
} 