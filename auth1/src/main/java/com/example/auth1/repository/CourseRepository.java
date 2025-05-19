package com.example.auth1.repository;

import com.example.auth1.model.Course;
import com.example.auth1.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByProgramInAndYearAndSemester(List<Program> programs, int year, int semester);
    List<Course> findByProgramId(Long programId);
    boolean existsByProgramIdAndYearAndSemester(Long programId, int year, int semester);
    List<Course> findByProgramIdOrderByYearAscSemesterAsc(Long programId);
    List<Course> findByProgramIdAndYearAndSemester(Long programId, int year, int semester);
}