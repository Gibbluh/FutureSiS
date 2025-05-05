package com.example.auth1.repository;

import com.example.auth1.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByCourseId(Long courseId);
    boolean existsByCourseIdAndCode(Long courseId, String code);
}