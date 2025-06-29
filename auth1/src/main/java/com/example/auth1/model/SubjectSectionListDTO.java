package com.example.auth1.model;

public class SubjectSectionListDTO {
    private Long id;
    private String subjectCode;
    private String subjectName;
    private String sectionName;
    private String facultyName;
    private String academicYear;
    private Integer semester;
    private String schedule;
    private String room;
    private Integer maxStudents;

    public SubjectSectionListDTO(Long id, String subjectCode, String subjectName, String sectionName, String facultyName, String academicYear, Integer semester, String schedule, String room, Integer maxStudents) {
        this.id = id;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.sectionName = sectionName;
        this.facultyName = facultyName;
        this.academicYear = academicYear;
        this.semester = semester;
        this.schedule = schedule;
        this.room = room;
        this.maxStudents = maxStudents;
    }

    public Long getId() { return id; }
    public String getSubjectCode() { return subjectCode; }
    public String getSubjectName() { return subjectName; }
    public String getSectionName() { return sectionName; }
    public String getFacultyName() { return facultyName; }
    public String getAcademicYear() { return academicYear; }
    public Integer getSemester() { return semester; }
    public String getSchedule() { return schedule; }
    public String getRoom() { return room; }
    public Integer getMaxStudents() { return maxStudents; }
} 