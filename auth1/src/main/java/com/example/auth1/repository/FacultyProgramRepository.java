package com.example.auth1.repository;

import com.example.auth1.model.FacultyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FacultyProgramRepository extends JpaRepository<FacultyProgram, Long> {
    List<FacultyProgram> findByFacultyId(Long facultyId);
    List<FacultyProgram> findByProgramId(Long programId);
    void deleteByFacultyId(Long facultyId);
} 