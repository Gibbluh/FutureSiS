package com.example.auth1.model;

import jakarta.persistence.*;
import java.time.LocalDate;

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
    private Role role = Role.STUDENT;  // Using your enum with default value
    
    
    // Constructors
    public Student() {}
    
    public Student(String studentNumber, LocalDate birthDate, String password) {
        this.studentNumber = studentNumber;
        this.birthDate = birthDate;
        this.password = password;
        this.role = Role.STUDENT; // Ensure role is always set
    }
    
    // Getters and Setters
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
    
    
    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentNumber='" + studentNumber + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}