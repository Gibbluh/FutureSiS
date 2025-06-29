package com.example.auth1.service;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final SubjectSectionRepository subjectSectionRepository;
    private final GradeRepository gradeRepository;
    private final StudentNumberGenerator studentNumberGenerator;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public StudentService(StudentRepository studentRepository, ProgramRepository programRepository, SectionRepository sectionRepository, SubjectSectionRepository subjectSectionRepository, GradeRepository gradeRepository, StudentNumberGenerator studentNumberGenerator, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.sectionRepository = sectionRepository;
        this.subjectSectionRepository = subjectSectionRepository;
        this.gradeRepository = gradeRepository;
        this.studentNumberGenerator = studentNumberGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    private String getCurrentAcademicYear() {
        int year = java.time.Year.now().getValue();
        return year + "-" + (year + 1);
    }

    @Transactional
    public void addStudent(String firstName, String lastName, String email, String birthDate, String password, Long programId, String phoneNumber, String address, Integer enrollmentYear, Long studentSectionId, String academicYear) {
        if (studentRepository.existsByEmail(email)) {
            throw new RuntimeException("A student with this email already exists: " + email);
        }
        
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setBirthDate(LocalDate.parse(birthDate));
        student.setPassword(passwordEncoder.encode(password));
        student.setStudentNumber(studentNumberGenerator.generateNextStudentNumber());
        student.setPhoneNumber(phoneNumber);
        student.setAddress(address);
        student.setEnrollmentYear(enrollmentYear);

        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new RuntimeException("Program not found with ID: " + programId));
        student.setProgram(program);

        Section section = sectionRepository.findById(studentSectionId)
            .orElseThrow(() -> new RuntimeException("Section not found with ID: " + studentSectionId));
        student.setSection(section);

        student.setCurrentSemester(1);

        // Ensure graduated is set to false
        student.setGraduated(false);

        if (academicYear != null && !academicYear.trim().isEmpty()) {
            student.setAcademicYear(academicYear.trim());
        } else {
        student.setAcademicYear(getCurrentAcademicYear());
        }

        Student savedStudent = studentRepository.save(student);

        String yearToUse = student.getAcademicYear();
        List<SubjectSection> sectionsToEnroll = subjectSectionRepository.findWithFilters(
            savedStudent.getProgram().getId(),
            savedStudent.getEnrollmentYear(),
            savedStudent.getCurrentSemester(),
            savedStudent.getSection().getId(),
            yearToUse
        );
        sectionsToEnroll = sectionsToEnroll.stream()
            .filter(ss -> yearToUse.equals(ss.getAcademicYear()))
            .toList();

        for (SubjectSection ss : sectionsToEnroll) {
            Grade newGrade = new Grade(savedStudent, ss, null);
            gradeRepository.save(newGrade);
        }
    }

    @Cacheable("studentList")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }
} 