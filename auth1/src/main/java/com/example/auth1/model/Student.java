package com.example.auth1.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Year;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 20)
    private String studentNumber;
    
    @Column(nullable = false)
    private LocalDate birthDate;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;
    
    @Transient // This field won't be persisted in the database
    private boolean numberGenerated = false;
    
    // Constructors remain the same
    public Student() {}
    
    public Student(String studentNumber, LocalDate birthDate, String password) {
        this.studentNumber = studentNumber;
        this.birthDate = birthDate;
        this.password = password;
        this.role = Role.STUDENT;
    }
    
    // Add this callback method to generate student number before persisting
    @PrePersist
    public void generateStudentNumber() {
        if (!numberGenerated && this.studentNumber == null) {
            int currentYear = Year.now().getValue();
            // The actual ID will be set after persistence
            // We'll update it in the @PostPersist callback
            this.studentNumber = String.format("STU-%d-#####", currentYear);
            this.numberGenerated = true;
        }
    }
    
    // Add this callback method to update the student number after ID is assigned
    @PostPersist
    public void updateStudentNumber() {
        if (this.studentNumber != null && this.studentNumber.contains("#####")) {
            this.studentNumber = this.studentNumber.replace("#####", 
                String.format("%05d", this.id));
        }
    }
    
    // Keep all existing getters and setters exactly as they are
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { 
        this.studentNumber = studentNumber; 
    }
    
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { 
        this.birthDate = birthDate; 
    }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { 
        this.password = password; 
    }
    
    public Role getRole() { return role; }
    public void setRole(Role role) {
        this.role = role;
    }
    
    // toString remains the same
    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentNumber='" + studentNumber + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}