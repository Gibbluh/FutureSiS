package com.example.auth1.repository;

import com.example.auth1.model.Section;
import com.example.auth1.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    boolean existsByName(String name);
    List<Section> findByProgramId(Long programId);
    List<Section> findByProgramIdIn(List<Long> programIds);
    boolean existsByNameAndProgramId(String name, Long programId);
    Optional<Section> findByNameAndProgramId(String name, Long programId);
    List<Section> findByProgramIdAndNameStartsWith(Long programId, String name);
    List<Section> findByProgram(Program program);

    Optional<Section> findByName(String name);

    List<Section> findByProgramIdAndYearLevel(Long programId, Integer yearLevel);
    List<Section> findByProgramIdAndYearLevelAndNameEndingWith(Long programId, Integer yearLevel, String suffix);

    @Query("SELECT new com.example.auth1.model.SectionListDTO(s.id, s.name, p.name, s.yearLevel, ss.academicYear, COUNT(DISTINCT g.student.id)) " +
           "FROM Section s " +
           "JOIN s.program p " +
           "LEFT JOIN SubjectSection ss ON ss.section.id = s.id " +
           "LEFT JOIN Grade g ON g.subjectSection.id = ss.id " +
           "GROUP BY s.id, s.name, p.name, s.yearLevel, ss.academicYear")
    java.util.List<com.example.auth1.model.SectionListDTO> findAllForListView();
} 