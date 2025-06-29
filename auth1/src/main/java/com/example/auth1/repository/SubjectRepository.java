package com.example.auth1.repository;

import com.example.auth1.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByCourseId(Long courseId);
    boolean existsByCourseIdAndCode(Long courseId, String code);

    @Query("SELECT DISTINCT s FROM Subject s " +
           "JOIN FETCH s.course c " +
           "JOIN FETCH c.program p " +
           "WHERE p.id IN :programIds")
    List<Subject> findByProgramIds(@Param("programIds") List<Long> programIds);

    @Query("SELECT s FROM Subject s WHERE (:programId IS NULL OR s.program.id = :programId) AND (:semester IS NULL OR s.semester = :semester) AND (:yearLevel IS NULL OR s.yearLevel = :yearLevel)")
    List<Subject> findByProgramIdAndSemesterAndYearLevel(@Param("programId") Long programId, @Param("semester") Integer semester, @Param("yearLevel") Integer yearLevel);
}