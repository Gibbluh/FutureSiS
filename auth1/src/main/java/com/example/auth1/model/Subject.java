package com.example.auth1.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private int units;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"subjects", "program"})
    private Course course;
    
    // Constructors
    public Subject() {}
    
    public Subject(String code, String name, int units, Course course) {
        this.code = code;
        this.name = name;
        this.units = units;
        this.course = course;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getUnits() {
        return units;
    }
    
    public void setUnits(int units) {
        this.units = units;
    }
    
    public Course getCourse() {
        return course;
    }
    
    public void setCourse(Course course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return String.format("Subject[id=%d, code=%s, name=%s, units=%d]",
            id, code, name, units);
    }
}