package com.example.auth1.repository;

import com.example.auth1.model.Program;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.courses WHERE p.id = :id")
    Optional<Program> findByIdWithCourses(@Param("id") Long id);
}