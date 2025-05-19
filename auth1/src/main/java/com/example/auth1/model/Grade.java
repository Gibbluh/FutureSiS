package com.example.auth1.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "grades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "subject_id", "academic_year", "semester"})
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
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnoreProperties({"course"})
    private Subject subject;
    
    @Column(nullable = true)
    private Double rawGrade; // Percentage grade (0-100)
    
    @Column(nullable = true)
    private Double gwa; // GWA grade (1.0-5.0)
    
    @Column(nullable = false)
    private String academicYear;
    
    @Column(nullable = false)
    private Integer semester;

    // Constructors
    public Grade() {}
    
    public Grade(Student student, Subject subject, Double rawGrade, String academicYear, Integer semester) {
        this.student = student;
        this.subject = subject;
        setRawGrade(rawGrade);
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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Double getRawGrade() {
        return rawGrade;
    }

    public void setRawGrade(Double rawGrade) {
        this.rawGrade = rawGrade;
        if (rawGrade != null) {
            this.gwa = calculateGWA(rawGrade);
        }
    }

    public Double getGwa() {
        return gwa;
    }

    public void setGwa(Double gwa) {
        this.gwa = gwa;
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
        return String.format("Grade[id=%d, student=%s, subject=%s, rawGrade=%.2f, gwa=%.2f, academicYear=%s, semester=%d]",
            id, student.getStudentNumber(), subject.getCode(), rawGrade, gwa, academicYear, semester);
    }
} 