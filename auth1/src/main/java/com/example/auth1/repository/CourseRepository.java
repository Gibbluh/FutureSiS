package com.example.auth1.repository;

import com.example.auth1.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByProgramId(Long programId);
    boolean existsByProgramIdAndYearAndSemester(Long programId, int year, int semester);
    List<Course> findByProgramIdOrderByYearAscSemesterAsc(Long programId);
    List<Course> findByProgramIdAndYearAndSemester(Long programId, int year, int semester);
}