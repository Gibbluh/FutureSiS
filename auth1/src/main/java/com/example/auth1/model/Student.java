package com.example.auth1.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
   
    @Column(unique = true, length = 20)
    private String studentNumber;
   
    @Column(nullable = false)
    private String firstName;
   
    @Column(nullable = false)
    private String lastName;
   
    @Column(nullable = false, unique = true)
    private String email;
   
    @Column(nullable = false)
    private LocalDate birthDate;
   
    @Column(nullable = false)
    private String password;
   
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;
   
    @Column(nullable = false)
    private LocalDate enrollmentDate = LocalDate.now();
   
    @Column(nullable = false)
    private Integer enrollmentYear;  // Academic year level (1=Freshman, 2=Sophomore, etc.)
   
    @Column(nullable = false)
    private Integer currentSemester = 1; // Track current semester (1 or 2)
   
    private String phoneNumber;
   
    private String address;
   
    @Column(nullable = false)
    private boolean active = true;
    
    @ManyToOne
    @JoinColumn(name = "program_id")
    private Program program;
    
    @ManyToOne
    @JoinColumn(name = "section_id")
    private Section section;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<Grade> grades = new java.util.ArrayList<>();
    
    @Column(nullable = false)
    private String academicYear; // Student's original academic year (e.g., '2025-2026')
    
    private int rejectedApprovalCount = 0;
    
    @Column(nullable = false)
    private Integer graduationStatus = 0; // 0 = not graduated, 1 = graduated
    
    @Column(nullable = false)
    private boolean graduated = false;
    
    // Constructors
    public Student() {
    }
    
    public Student(String firstName, String lastName, String email,
                  LocalDate birthDate, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
        this.password = password;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getStudentNumber() {
        return studentNumber;
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public Integer getEnrollmentYear() {
        return enrollmentYear;
    }
    
    public void setEnrollmentYear(Integer enrollmentYear) {
        this.enrollmentYear = enrollmentYear;
    }
    
    public Integer getCurrentSemester() {
        return currentSemester;
    }
    
    public void setCurrentSemester(Integer currentSemester) {
        this.currentSemester = currentSemester;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public Program getProgram() {
        return program;
    }
    
    public void setProgram(Program program) {
        this.program = program;
    }
    
    public Section getSection() {
        return section;
    }
    
    public void setSection(Section section) {
        this.section = section;
    }
    
    public java.util.List<Grade> getGrades() {
        return grades;
    }
    
    public void setGrades(java.util.List<Grade> grades) {
        this.grades = grades;
    }
    
    public String getAcademicYear() {
        return academicYear;
    }
    
    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }
    
    public int getRejectedApprovalCount() {
        return rejectedApprovalCount;
    }
    
    public void setRejectedApprovalCount(int count) {
        this.rejectedApprovalCount = count;
    }
    
    public Integer getGraduationStatus() {
        return graduationStatus;
    }
    
    public void setGraduationStatus(Integer graduationStatus) {
        this.graduationStatus = graduationStatus;
    }
    
    public boolean isGraduated() {
        return graduated;
    }
    
    public void setGraduated(boolean graduated) {
        this.graduated = graduated;
    }
    
    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentNumber='" + studentNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", enrollmentYear=" + enrollmentYear +
                ", role=" + role +
                '}';
    }
}