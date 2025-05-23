package com.example.auth1.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "semester_approval_requests")
public class SemesterApprovalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column
    private LocalDateTime approvalDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by")
    private Admin approvedBy;

    @Column
    private String comments;

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    // Constructors
    public SemesterApprovalRequest() {}

    public SemesterApprovalRequest(Student student, String academicYear, Integer semester) {
        this.student = student;
        this.academicYear = academicYear;
        this.semester = semester;
        this.requestDate = LocalDateTime.now();
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

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public Admin getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Admin approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
} 