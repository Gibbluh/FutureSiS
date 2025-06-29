package com.example.auth1.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "grades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "subject_section_id"})
})
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"grades", "program"})
    private Student student;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_section_id", nullable = false)
    @JsonIgnoreProperties({"grades", "subject", "section"})
    private SubjectSection subjectSection;
    
    @Column(nullable = true)
    private Double rawGrade; // Percentage grade (0-100)
    
    @Column(nullable = true)
    private Double gwa; // GWA grade (1.0-5.0)

    // Constructors
    public Grade() {}
    
    public Grade(Student student, SubjectSection subjectSection, Double rawGrade) {
        this.student = student;
        this.subjectSection = subjectSection;
        setRawGrade(rawGrade);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public SubjectSection getSubjectSection() {
        return subjectSection;
    }

    public void setSubjectSection(SubjectSection subjectSection) {
        this.subjectSection = subjectSection;
    }

    public Double getRawGrade() {
        return rawGrade;
    }

    public void setRawGrade(Double rawGrade) {
        this.rawGrade = rawGrade;
        if (rawGrade != null) {
            this.gwa = calculateGWA(rawGrade);
        } else {
            this.gwa = null;
        }
    }

    public Double getGwa() {
        return gwa;
    }

    public void setGwa(Double gwa) {
        this.gwa = gwa;
        if (gwa != null) {
            this.rawGrade = estimateRawGrade(gwa);
        } else {
            this.rawGrade = null;
        }
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

    private Double estimateRawGrade(Double gwa) {
        if (gwa == null) return null;
        if (gwa.equals(1.0)) return 98.0;
        if (gwa.equals(1.25)) return 95.0;
        if (gwa.equals(1.5)) return 92.0;
        if (gwa.equals(1.75)) return 89.0;
        if (gwa.equals(2.0)) return 86.0;
        if (gwa.equals(2.25)) return 83.0;
        if (gwa.equals(2.5)) return 80.0;
        if (gwa.equals(2.75)) return 77.0;
        if (gwa.equals(3.0)) return 75.0;
        if (gwa.equals(5.0)) return 74.0; // Represents below 75
        return 0.0; // Default for any other case
    }

    // Grade calculation methods
    private Double calculateGWA(Double rawGrade) {
        if (rawGrade == null) return null;
        
        // Philippine Grading Scale:
        // 1.0 = 97-100% (Excellent)
        // 1.25 = 94-96%
        // 1.5 = 91-93%
        // 1.75 = 88-90%
        // 2.0 = 85-87%
        // 2.25 = 82-84%
        // 2.5 = 79-81%
        // 2.75 = 76-78%
        // 3.0 = 75%
        // 5.0 = Below 75 (Failed)
        
        if (rawGrade >= 97) return 1.0;
        if (rawGrade >= 94) return 1.25;
        if (rawGrade >= 91) return 1.5;
        if (rawGrade >= 88) return 1.75;
        if (rawGrade >= 85) return 2.0;
        if (rawGrade >= 82) return 2.25;
        if (rawGrade >= 79) return 2.5;
        if (rawGrade >= 76) return 2.75;
        if (rawGrade >= 75) return 3.0;
        return 5.0;
    }

    public String getLetterGrade() {
        if (rawGrade == null) return "N/A";
        
        if (rawGrade >= 97) return "A+";
        if (rawGrade >= 94) return "A";
        if (rawGrade >= 91) return "A-";
        if (rawGrade >= 88) return "B+";
        if (rawGrade >= 85) return "B";
        if (rawGrade >= 82) return "B-";
        if (rawGrade >= 79) return "C+";
        if (rawGrade >= 76) return "C";
        if (rawGrade >= 75) return "C-";
        return "F";
    }

    @Override
    public String toString() {
        return String.format("Grade[id=%d, student=%s, subject=%s, section=%s, rawGrade=%.2f, gwa=%.2f, academicYear=%s, semester=%d]",
            id, 
            student != null ? student.getStudentNumber() : "null", 
            subjectSection != null && subjectSection.getSubject() != null ? subjectSection.getSubject().getCode() : "null",
            subjectSection != null && subjectSection.getSection() != null ? subjectSection.getSection().getName() : "null",
            rawGrade, gwa, 
            subjectSection != null ? subjectSection.getAcademicYear() : "null", 
            subjectSection != null ? subjectSection.getSemester() : null);
    }
} 