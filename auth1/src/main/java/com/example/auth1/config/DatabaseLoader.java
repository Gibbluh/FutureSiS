package com.example.auth1.config;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

        // 2. Create programs with complete curriculum
        Map<String, Map<Integer, Map<Integer, List<String>>>> curriculumData = createCurriculumData();
        
        List<Program> programs = List.of(
            createProgramWithFullCurriculum("Computer Science", curriculumData.get("Computer Science")),
            createProgramWithFullCurriculum("Business Administration", curriculumData.get("Business Administration")),
            createProgramWithFullCurriculum("Electrical Engineering", curriculumData.get("Electrical Engineering"))
        );
        programRepository.saveAll(programs);

        // 3. Create sample students
        List<Student> students = List.of(
            createStudent("STU-2023-00001", "John", "Doe", "john.doe@school.edu", 
                         LocalDate.of(2000, 5, 15), programs.get(0), 1, 1),
            createStudent("STU-2023-00002", "Jane", "Smith", "jane.smith@school.edu", 
                         LocalDate.of(2001, 6, 20), programs.get(1), 2, 1),
            createStudent("STU-2023-00003", "Mike", "Johnson", "mike.johnson@school.edu", 
                         LocalDate.of(1999, 7, 10), programs.get(2), 1, 1)
        );
        studentRepository.saveAll(students);

        System.out.println("=== Sample Data Loaded ===");
        System.out.println("Admins: " + admins.size());
        System.out.println("Programs: " + programs.size());
        System.out.println("Courses: " + courseRepository.count());
        System.out.println("Subjects: " + subjectRepository.count());
        System.out.println("Students: " + students.size());
    }

    private Map<String, Map<Integer, Map<Integer, List<String>>>> createCurriculumData() {
        Map<String, Map<Integer, Map<Integer, List<String>>>> curriculum = new HashMap<>();
        
        // Computer Science Curriculum
        Map<Integer, Map<Integer, List<String>>> csYears = new HashMap<>();
        csYears.put(1, Map.of(
            1, List.of(
                "Introduction to Programming",
                "Computer Fundamentals",
                "Mathematics for Computing",
                "English Communication",
                "Physical Education 1"
            ),
            2, List.of(
                "Object-Oriented Programming",
                "Discrete Mathematics",
                "Data Structures",
                "Technical Writing",
                "Physical Education 2"
            )
        ));
        csYears.put(2, Map.of(
            1, List.of(
                "Database Management Systems",
                "Web Development",
                "Computer Architecture",
                "Probability and Statistics",
                "Filipino 1"
            ),
            2, List.of(
                "Advanced Database Systems",
                "Operating Systems",
                "Software Engineering",
                "Network Fundamentals",
                "Filipino 2"
            )
        ));
        curriculum.put("Computer Science", csYears);

        // Business Administration Curriculum
        Map<Integer, Map<Integer, List<String>>> baYears = new HashMap<>();
        baYears.put(1, Map.of(
            1, List.of(
                "Principles of Management",
                "Basic Economics",
                "Business Mathematics",
                "Communication Arts",
                "Physical Education 1"
            ),
            2, List.of(
                "Business Economics",
                "Accounting Fundamentals",
                "Marketing Principles",
                "Business Communication",
                "Physical Education 2"
            )
        ));
        baYears.put(2, Map.of(
            1, List.of(
                "Financial Management",
                "Business Law",
                "Organizational Behavior",
                "Management Accounting",
                "Filipino 1"
            ),
            2, List.of(
                "Strategic Management",
                "Operations Management",
                "Human Resource Management",
                "Business Research",
                "Filipino 2"
            )
        ));
        curriculum.put("Business Administration", baYears);

        // Electrical Engineering Curriculum
        Map<Integer, Map<Integer, List<String>>> eeYears = new HashMap<>();
        eeYears.put(1, Map.of(
            1, List.of(
                "Engineering Mathematics 1",
                "Physics for Engineers",
                "Engineering Drawing",
                "Chemistry for Engineers",
                "Physical Education 1"
            ),
            2, List.of(
                "Engineering Mathematics 2",
                "Circuit Theory",
                "Electronics 1",
                "Programming for Engineers",
                "Physical Education 2"
            )
        ));
        eeYears.put(2, Map.of(
            1, List.of(
                "Engineering Mathematics 3",
                "Electronics 2",
                "Digital Logic Design",
                "Electrical Machines",
                "Filipino 1"
            ),
            2, List.of(
                "Control Systems",
                "Power Systems",
                "Microprocessors",
                "Electromagnetics",
                "Filipino 2"
            )
        ));
        curriculum.put("Electrical Engineering", eeYears);

        return curriculum;
    }

    private Program createProgramWithFullCurriculum(String name, Map<Integer, Map<Integer, List<String>>> yearData) {
        final Program program = new Program(name);
        final Program savedProgram = programRepository.save(program);

        // Create courses and subjects for each year and semester
        yearData.forEach((year, semesterData) -> {
            semesterData.forEach((semester, subjects) -> {
                Course course = new Course(year, semester, savedProgram);
                course = courseRepository.save(course);
                addSubjectsToCourse(course, subjects);
            });
        });

        return savedProgram;
    }

    private void addSubjectsToCourse(Course course, List<String> subjectNames) {
        String programPrefix = course.getProgram().getName().substring(0, 3).toUpperCase();
        int year = course.getYear();
        int semester = course.getSemester();
        int baseCode = (year * 100) + (semester * 10);
        
        for (int i = 0; i < subjectNames.size(); i++) {
            String subjectCode = String.format("%s%d", programPrefix, baseCode + i + 1);
            Subject subject = new Subject(
                subjectCode,
                subjectNames.get(i),
                3, // Default 3 units
                course
            );
            subjectRepository.save(subject);
        }
    }

    private Student createStudent(String studentNumber, String firstName, String lastName, 
                                String email, LocalDate birthDate, Program program,
                                int enrollmentYear, int currentSemester) {
        Student student = new Student();
        student.setStudentNumber(studentNumber);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setBirthDate(birthDate);
        student.setPassword(passwordEncoder.encode("Student123!"));
        student.setRole(Role.STUDENT);
        student.setProgram(program);
        student.setEnrollmentYear(enrollmentYear);
        student.setCurrentSemester(currentSemester);
        student.setEnrollmentDate(LocalDate.now());
        return student;
    }
}