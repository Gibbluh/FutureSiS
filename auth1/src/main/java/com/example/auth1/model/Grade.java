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
            this.gwa = convertToGWA(rawGrade);
        }
    }

    public Double getGwa() {
        return gwa;
    }

    // For backward compatibility
    public Double getGrade() {
        return rawGrade;
    }

    // For backward compatibility
    public void setGrade(Double grade) {
        setRawGrade(grade);
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

    private Double convertToGWA(Double percentageGrade) {
        if (percentageGrade == null) return null;
        if (percentageGrade >= 95) return 1.0;
        if (percentageGrade >= 92) return 1.25;
        if (percentageGrade >= 89) return 1.5;
        if (percentageGrade >= 86) return 1.75;
        if (percentageGrade >= 83) return 2.0;
        if (percentageGrade >= 80) return 2.25;
        if (percentageGrade >= 77) return 2.5;
        if (percentageGrade >= 75) return 2.75;
        if (percentageGrade >= 72) return 3.0;
        return 5.0;
    }

    public String getLetterGrade() {
        if (gwa == null) return "N/A";
        if (gwa == 1.0) return "A";
        if (gwa <= 1.5) return "A-";
        if (gwa <= 2.0) return "B";
        if (gwa <= 2.5) return "B-";
        if (gwa <= 3.0) return "C";
        return "F";
    }

    @Override
    public String toString() {
        return String.format("Grade[id=%d, student=%s, subject=%s, rawGrade=%.2f, gwa=%.2f, academicYear=%s, semester=%d]",
            id, student.getStudentNumber(), subject.getCode(), rawGrade, gwa, academicYear, semester);
    }
} 