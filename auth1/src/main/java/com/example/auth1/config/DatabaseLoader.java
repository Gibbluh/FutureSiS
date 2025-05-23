package com.example.auth1.config;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Component
public class DatabaseLoader implements CommandLineRunner {
    
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyProgramRepository facultyProgramRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseLoader(AdminRepository adminRepository,
                        StudentRepository studentRepository,
                        ProgramRepository programRepository,
                        CourseRepository courseRepository,
                        SubjectRepository subjectRepository,
                        FacultyRepository facultyRepository,
                        FacultyProgramRepository facultyProgramRepository,
                        TeachingAssignmentRepository teachingAssignmentRepository,
                        PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.facultyProgramRepository = facultyProgramRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (shouldLoadData()) {
            loadInitialData();
        }
    }

    private boolean shouldLoadData() {
        return adminRepository.count() == 0 && 
               studentRepository.count() == 0 && 
               programRepository.count() == 0 &&
               facultyRepository.count() == 0;
    }

    @Transactional
    private void loadInitialData() {
        // 1. Create admin users
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

        // 3. Create faculty members with multiple program assignments
        List<Faculty> faculty = new ArrayList<>();
        
        // CS Faculty
        Faculty csProf1 = createFaculty("rwilson", "Robert", "Wilson", "robert.wilson@school.edu",
                         "Associate Professor", "+1234567890", "Wilson123!");
        Faculty csProf2 = createFaculty("jsmith", "Jane", "Smith", "jane.smith@school.edu",
                         "Assistant Professor", "+1234567891", "Smith123!");
        
        // Business Faculty
        Faculty busProf1 = createFaculty("mbrown", "Michael", "Brown", "michael.brown@school.edu",
                         "Professor", "+1234567892", "Brown123!");
        Faculty busProf2 = createFaculty("ldavis", "Linda", "Davis", "linda.davis@school.edu",
                         "Associate Professor", "+1234567893", "Davis123!");
        
        // Engineering Faculty
        Faculty engProf1 = createFaculty("tlee", "Thomas", "Lee", "thomas.lee@school.edu",
                         "Professor", "+1234567894", "Lee123!");
        Faculty engProf2 = createFaculty("kchen", "Karen", "Chen", "karen.chen@school.edu",
                         "Assistant Professor", "+1234567895", "Chen123!");

        faculty.addAll(List.of(csProf1, csProf2, busProf1, busProf2, engProf1, engProf2));
        facultyRepository.saveAll(faculty);

        // Assign programs to faculty (some faculty teach in multiple programs)
        assignFacultyToPrograms(csProf1, List.of(programs.get(0))); // CS only
        assignFacultyToPrograms(csProf2, List.of(programs.get(0), programs.get(2))); // CS and Engineering
        assignFacultyToPrograms(busProf1, List.of(programs.get(1))); // Business only
        assignFacultyToPrograms(busProf2, List.of(programs.get(1), programs.get(0))); // Business and CS
        assignFacultyToPrograms(engProf1, List.of(programs.get(2))); // Engineering only
        assignFacultyToPrograms(engProf2, List.of(programs.get(2), programs.get(1))); // Engineering and Business

        // Assign teaching assignments
        String currentAcademicYear = "2023-2024";
        assignTeachingLoad(csProf1, programs.get(0), 1, 1, currentAcademicYear); // First year, first sem CS subjects
        assignTeachingLoad(csProf2, programs.get(0), 1, 2, currentAcademicYear); // First year, second sem CS subjects
        assignTeachingLoad(busProf1, programs.get(1), 1, 1, currentAcademicYear); // First year, first sem Business subjects
        assignTeachingLoad(busProf2, programs.get(1), 1, 2, currentAcademicYear); // First year, second sem Business subjects
        assignTeachingLoad(engProf1, programs.get(2), 1, 1, currentAcademicYear); // First year, first sem Engineering subjects
        assignTeachingLoad(engProf2, programs.get(2), 1, 2, currentAcademicYear); // First year, second sem Engineering subjects

        // 4. Create sample students
        // List<Student> students = new ArrayList<>();
        // 
        // // CS Students
        // students.addAll(List.of(
        //     createStudent("2023-CS-001", "John", "Doe", "john.doe@school.edu", 
        //                  LocalDate.of(2000, 5, 15), programs.get(0), 1, 1),
        //     createStudent("2023-CS-002", "Alice", "Johnson", "alice.j@school.edu", 
        //                  LocalDate.of(2000, 6, 20), programs.get(0), 1, 1),
        //     createStudent("2022-CS-001", "Bob", "Smith", "bob.s@school.edu", 
        //                  LocalDate.of(1999, 7, 10), programs.get(0), 2, 1)
        // ));
        // 
        // // Business Students
        // students.addAll(List.of(
        //     createStudent("2023-BA-001", "Emma", "Davis", "emma.d@school.edu", 
        //                  LocalDate.of(2001, 3, 25), programs.get(1), 1, 1),
        //     createStudent("2023-BA-002", "James", "Wilson", "james.w@school.edu", 
        //                  LocalDate.of(2001, 4, 15), programs.get(1), 1, 1),
        //     createStudent("2022-BA-001", "Sarah", "Brown", "sarah.b@school.edu", 
        //                  LocalDate.of(2000, 8, 5), programs.get(1), 2, 1)
        // ));
        // 
        // // Engineering Students
        // students.addAll(List.of(
        //     createStudent("2023-EE-001", "Michael", "Lee", "michael.l@school.edu", 
        //                  LocalDate.of(2000, 9, 30), programs.get(2), 1, 1),
        //     createStudent("2023-EE-002", "Emily", "Taylor", "emily.t@school.edu", 
        //                  LocalDate.of(2000, 10, 12), programs.get(2), 1, 1),
        //     createStudent("2022-EE-001", "David", "Anderson", "david.a@school.edu", 
        //                  LocalDate.of(1999, 11, 20), programs.get(2), 2, 1)
        // ));
        // 
        // studentRepository.saveAll(students);

        System.out.println("=== Sample Data Loaded ===");
        System.out.println("Admins: " + admins.size());
        System.out.println("Programs: " + programs.size());
        System.out.println("Faculty: " + faculty.size());
        System.out.println("Courses: " + courseRepository.count());
        System.out.println("Subjects: " + subjectRepository.count());
        System.out.println("Students: " + studentRepository.count());
        System.out.println("Faculty Programs: " + facultyProgramRepository.count());
        System.out.println("Teaching Assignments: " + teachingAssignmentRepository.count());
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
                "Electromagnetic Theory",
                "Filipino 1"
            ),
            2, List.of(
                "Control Systems",
                "Power Systems",
                "Microprocessors",
                "Electrical Design",
                "Filipino 2"
            )
        ));
        curriculum.put("Electrical Engineering", eeYears);

        return curriculum;
    }

    private Program createProgramWithFullCurriculum(String name, Map<Integer, Map<Integer, List<String>>> yearData) {
        Program program = new Program(name);
        program = programRepository.save(program);

        List<Course> courses = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, List<String>>> yearEntry : yearData.entrySet()) {
            int year = yearEntry.getKey();
            for (Map.Entry<Integer, List<String>> semesterEntry : yearEntry.getValue().entrySet()) {
                int semester = semesterEntry.getKey();
                Course course = new Course();
                course.setProgram(program);
                course.setYear(year);
                course.setSemester(semester);
                course.setSubjects(new ArrayList<>()); // Initialize the subjects list
                course = courseRepository.save(course);
                
                addSubjectsToCourse(course, semesterEntry.getValue());
                courses.add(course);
            }
        }
        program.setCourses(courses);
        return programRepository.save(program);
    }

    private void addSubjectsToCourse(Course course, List<String> subjectNames) {
        List<Subject> subjects = new ArrayList<>();
        for (String subjectName : subjectNames) {
            Subject subject = new Subject();
            subject.setName(subjectName);
            subject.setCode(generateSubjectCode(course.getProgram().getName(), subjectName));
            subject.setUnits(3); // Default to 3 units
            subject.setCourse(course);
            subjects.add(subject);
        }
        subjectRepository.saveAll(subjects);
        course.setSubjects(subjects);
        courseRepository.save(course);
    }

    private String generateSubjectCode(String programName, String subjectName) {
        String prefix = programName.substring(0, 2).toUpperCase();
        String number = String.format("%03d", Math.abs(subjectName.hashCode() % 1000));
        return prefix + number;
    }

    private Faculty createFaculty(String facultyId, String firstName, String lastName,
                                String email, String position, String phoneNumber,
                                String password) {
        Faculty faculty = new Faculty();
        faculty.setFacultyId(facultyId);
        faculty.setFirstName(firstName);
        faculty.setLastName(lastName);
        faculty.setEmail(email);
        faculty.setPosition(position);
        faculty.setPhoneNumber(phoneNumber);
        faculty.setPassword(passwordEncoder.encode(password));
        faculty.setRole(Role.FACULTY);
        return faculty;
    }

    private void assignFacultyToPrograms(Faculty faculty, List<Program> programs) {
        for (Program program : programs) {
            FacultyProgram fp = new FacultyProgram();
            fp.setFaculty(faculty);
            fp.setProgram(program);
            facultyProgramRepository.save(fp);
        }
    }

    private void assignTeachingLoad(Faculty faculty, Program program, int year, int semester, String academicYear) {
        List<Course> courses = courseRepository.findByProgramIdAndYearAndSemester(program.getId(), year, semester);
        if (courses.isEmpty()) {
            return;
        }
        
        Course course = courses.get(0);
        List<Subject> subjects = subjectRepository.findByCourseId(course.getId());
        
        for (Subject subject : subjects) {
            TeachingAssignment ta = new TeachingAssignment();
            ta.setFaculty(faculty);
            ta.setSubject(subject);
            ta.setAcademicYear(academicYear);
            ta.setSemester(semester);
            teachingAssignmentRepository.save(ta);
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
        student.setProgram(program);
        student.setEnrollmentYear(enrollmentYear);
        student.setCurrentSemester(currentSemester);
        student.setEnrollmentDate(LocalDate.now());
        return student;
    }
}