package com.example.auth1.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "faculty")
public class Faculty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "faculty_id", unique = true, nullable = false)
    private String facultyId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.FACULTY;

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FacultyProgram> facultyPrograms = new HashSet<>();

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TeachingAssignment> teachingAssignments = new HashSet<>();

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String address;

    @Column(nullable = false)
    private String position;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
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

    public Set<FacultyProgram> getFacultyPrograms() {
        return facultyPrograms;
    }

    public void setFacultyPrograms(Set<FacultyProgram> facultyPrograms) {
        this.facultyPrograms = facultyPrograms;
    }

    public Set<TeachingAssignment> getTeachingAssignments() {
        return teachingAssignments;
    }

    public void setTeachingAssignments(Set<TeachingAssignment> teachingAssignments) {
        this.teachingAssignments = teachingAssignments;
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

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods for managing relationships
    public void addFacultyProgram(FacultyProgram facultyProgram) {
        if (facultyProgram != null) {
            facultyPrograms.add(facultyProgram);
            if (facultyProgram.getFaculty() != this) {
                facultyProgram.setFaculty(this);
            }
        }
    }

    public void removeFacultyProgram(FacultyProgram facultyProgram) {
        if (facultyProgram != null) {
            facultyPrograms.remove(facultyProgram);
            if (facultyProgram.getFaculty() == this) {
                facultyProgram.setFaculty(null);
            }
        }
    }

    public void addTeachingAssignment(TeachingAssignment assignment) {
        teachingAssignments.add(assignment);
        if (assignment.getFaculty() != this) {
            assignment.setFaculty(this);
        }
    }

    public void removeTeachingAssignment(TeachingAssignment assignment) {
        teachingAssignments.remove(assignment);
        if (assignment.getFaculty() == this) {
            assignment.setFaculty(null);
        }
    }

    public void clearFacultyPrograms() {
        facultyPrograms.clear();
    }

    public void clearTeachingAssignments() {
        teachingAssignments.clear();
    }
} 