package com.example.auth1.model;

public class SectionListDTO {
    private Long id;
    private String name;
    private String programName;
    private Integer yearLevel;
    private String academicYear;
    private Long studentCount;

    public SectionListDTO(Long id, String name, String programName, Integer yearLevel, String academicYear, Long studentCount) {
        this.id = id;
        this.name = name;
        this.programName = programName;
        this.yearLevel = yearLevel;
        this.academicYear = academicYear;
        this.studentCount = studentCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProgramName() { return programName; }
    public Integer getYearLevel() { return yearLevel; }
    public String getAcademicYear() { return academicYear; }
    public Long getStudentCount() { return studentCount; }
} 