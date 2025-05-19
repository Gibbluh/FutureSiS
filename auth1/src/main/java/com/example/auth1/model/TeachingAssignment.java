package com.example.auth1.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;

@Entity
@Table(name = "teaching_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"faculty_id", "subject_id", "academic_year", "semester"})
})
public class TeachingAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    @JsonIgnoreProperties({"teachingAssignments", "facultyPrograms"})
    private Faculty faculty;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnoreProperties({"course"})
    private Subject subject;
    
    @Column(nullable = false)
    private String academicYear;
    
    @Column(nullable = false)
    private Integer semester;

    // Constructors
    public TeachingAssignment() {}
    
    public TeachingAssignment(Faculty faculty, Subject subject, String academicYear, Integer semester) {
        this.setFaculty(faculty);
        this.setSubject(subject);
        this.academicYear = academicYear;
        this.semester = semester;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        if (this.faculty != null && this.faculty != faculty) {
            Set<TeachingAssignment> oldFacultyAssignments = this.faculty.getTeachingAssignments();
            if (oldFacultyAssignments != null) {
                oldFacultyAssignments.remove(this);
            }
        }
        this.faculty = faculty;
        if (faculty != null) {
            Set<TeachingAssignment> newFacultyAssignments = faculty.getTeachingAssignments();
            if (newFacultyAssignments != null && !newFacultyAssignments.contains(this)) {
                newFacultyAssignments.add(this);
            }
        }
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeachingAssignment that = (TeachingAssignment) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
} 