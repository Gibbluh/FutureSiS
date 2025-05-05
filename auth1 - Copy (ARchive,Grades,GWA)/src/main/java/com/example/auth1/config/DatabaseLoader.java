package com.example.auth1.config;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DatabaseLoader implements CommandLineRunner {
    
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseLoader(AdminRepository adminRepository,
                        StudentRepository studentRepository,
                        ProgramRepository programRepository,
                        CourseRepository courseRepository,
                        SubjectRepository subjectRepository,
                        PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (shouldLoadData()) {
            loadInitialData();
        }
    }

    private boolean shouldLoadData() {
        return adminRepository.count() == 0 && 
               studentRepository.count() == 0 && 
               programRepository.count() == 0;
    }

    private void loadInitialData() {
        // 1. Create admin accounts
        List<Admin> admins = List.of(
            new Admin("admin1@school.edu", passwordEncoder.encode("Admin123!"), Role.ADMIN),
            new Admin("admin2@school.edu", passwordEncoder.encode("Admin123!"), Role.ADMIN)
        );
        adminRepository.saveAll(admins);

        // 2. Create sample programs with full 4-year structure
        List<Program> programs = List.of(
            createProgramWithCourses("Computer Science", List.of(
                "Introduction to Programming",
                "Discrete Mathematics",
                "Computer Fundamentals"
            )),
            createProgramWithCourses("Business Administration", List.of(
                "Principles of Management",
                "Business Economics",
                "Accounting Fundamentals"
            )),
            createProgramWithCourses("Electrical Engineering", List.of(
                "Circuit Theory",
                "Electronics Fundamentals",
                "Engineering Mathematics"
            ))
        );
        programRepository.saveAll(programs);

        // 3. Create sample students with program assignments
        List<Student> students = List.of(
            createStudent("STU-2023-00001", "John", "Doe", "john.doe@school.edu", 
                         LocalDate.of(2000, 5, 15), programs.get(0)),
            createStudent("STU-2023-00002", "Jane", "Smith", "jane.smith@school.edu", 
                         LocalDate.of(2001, 6, 20), programs.get(1)),
            createStudent("STU-2023-00003", "Mike", "Johnson", "mike.johnson@school.edu", 
                         LocalDate.of(1999, 7, 10), programs.get(2))
        );
        studentRepository.saveAll(students);

        System.out.println("=== Sample Data Loaded ===");
        System.out.println("Admins: " + admins.size());
        System.out.println("Programs: " + programs.size());
        System.out.println("Courses: " + courseRepository.count());
        System.out.println("Subjects: " + subjectRepository.count());
        System.out.println("Students: " + students.size());
    }

    private Program createProgramWithCourses(String name, List<String> firstYearSubjects) {
        Program program = new Program(name);
        program = programRepository.save(program);

        // Create 4 years with 2 semesters each
        for (int year = 1; year <= 4; year++) {
            for (int semester = 1; semester <= 2; semester++) {
                Course course = new Course(year, semester, program);
                course = courseRepository.save(course);

                // Add sample subjects to first year first semester
                if (year == 1 && semester == 1) {
                    addSubjectsToCourse(course, firstYearSubjects);
                }
            }
        }
        return program;
    }

    private void addSubjectsToCourse(Course course, List<String> subjectNames) {
        String programPrefix = course.getProgram().getName().substring(0, 3).toUpperCase();
        int subjectCode = 101; // Starting course code
        
        for (String subjectName : subjectNames) {
            Subject subject = new Subject(
                programPrefix + subjectCode++,
                subjectName,
                3, // Default 3 units
                course
            );
            subjectRepository.save(subject);
        }
    }

    private Student createStudent(String studentNumber, String firstName, String lastName, 
                                String email, LocalDate birthDate, Program program) {
        Student student = new Student();
        student.setStudentNumber(studentNumber);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setBirthDate(birthDate);
        student.setPassword(passwordEncoder.encode("Student123!"));
        student.setRole(Role.STUDENT);
        student.setProgram(program);
        return student;
    }
}