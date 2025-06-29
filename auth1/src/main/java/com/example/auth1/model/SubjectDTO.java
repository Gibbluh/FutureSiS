package com.example.auth1.model;

public class SubjectDTO {
    private Long id;
    private String code;
    private String name;
    private Integer yearLevel;
    private Integer semester;

    public SubjectDTO(Long id, String code, String name, Integer yearLevel, Integer semester) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.yearLevel = yearLevel;
        this.semester = semester;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getYearLevel() { return yearLevel; }
    public Integer getSemester() { return semester; }
} 