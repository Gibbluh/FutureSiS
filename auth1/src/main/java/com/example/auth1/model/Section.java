package com.example.auth1.model;

import jakarta.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "sections")
@JsonIgnoreProperties({"students", "program", "subjectSections"})
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY)
    private List<Student> students;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "program_id")
    private Program program;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("section")
    private List<SubjectSection> subjectSections;

    @Column(nullable = false)
    private Integer yearLevel;

    // Constructors
    public Section() {}
    public Section(String name) { this.name = name; }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Student> getStudents() { return students; }
    public void setStudents(List<Student> students) { this.students = students; }
    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }
    public List<SubjectSection> getSubjectSections() { return subjectSections; }
    public void setSubjectSections(List<SubjectSection> subjectSections) { this.subjectSections = subjectSections; }
    public Integer getYearLevel() { return yearLevel; }
    public void setYearLevel(Integer yearLevel) { this.yearLevel = yearLevel; }
} 