package com.example.auth1.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;

@Entity
@Table(name = "teaching_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"faculty_id", "subject_section_id"})
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
    @JoinColumn(name = "subject_section_id", nullable = false)
    @JsonIgnoreProperties({"teachingAssignments", "subject", "section"})
    private SubjectSection subjectSection;

    // Constructors
    public TeachingAssignment() {}
    
    public TeachingAssignment(Faculty faculty, SubjectSection subjectSection) {
        this.setFaculty(faculty);
        this.setSubjectSection(subjectSection);
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

    public SubjectSection getSubjectSection() {
        return subjectSection;
    }

    public void setSubjectSection(SubjectSection subjectSection) {
        this.subjectSection = subjectSection;
    }

    // Convenience methods to access subject, section, academic year, and semester
    public Subject getSubject() {
        return subjectSection != null ? subjectSection.getSubject() : null;
    }

    public Section getSection() {
        return subjectSection != null ? subjectSection.getSection() : null;
    }

    public String getAcademicYear() {
        return subjectSection != null ? subjectSection.getAcademicYear() : null;
    }

    public Integer getSemester() {
        return subjectSection != null ? subjectSection.getSemester() : null;
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