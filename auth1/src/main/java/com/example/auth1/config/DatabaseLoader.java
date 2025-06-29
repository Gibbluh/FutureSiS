package com.example.auth1.config;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    private final SectionRepository sectionRepository;
    private final SubjectSectionRepository subjectSectionRepository;

    public DatabaseLoader(AdminRepository adminRepository,
                        StudentRepository studentRepository,
                        ProgramRepository programRepository,
                        CourseRepository courseRepository,
                        SubjectRepository subjectRepository,
                        FacultyRepository facultyRepository,
                        FacultyProgramRepository facultyProgramRepository,
                        TeachingAssignmentRepository teachingAssignmentRepository,
                        PasswordEncoder passwordEncoder,
                        SectionRepository sectionRepository,
                        SubjectSectionRepository subjectSectionRepository) {
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.facultyProgramRepository = facultyProgramRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.sectionRepository = sectionRepository;
        this.subjectSectionRepository = subjectSectionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        loadInitialData();
    }

    private boolean shouldLoadData() {
        return adminRepository.count() == 0 && 
               studentRepository.count() == 0 && 
               programRepository.count() == 0 &&
               facultyRepository.count() == 0;
    }

    @Transactional
    private void loadInitialData() {
        // Only seed if there are no sections, subjects, subject sections, or courses
        if (sectionRepository.count() > 0 || subjectRepository.count() > 0 || subjectSectionRepository.count() > 0 || courseRepository.count() > 0) {
            System.out.println("Database already seeded. Skipping initial data load.");
            return;
        }
        // Always ensure admin accounts exist
        if (adminRepository.count() == 0) {
            System.out.println("Creating admin accounts...");
            List<Admin> admins = List.of(
                new Admin("admin1@school.edu", passwordEncoder.encode("Admin123!"), Role.ADMIN),
                new Admin("admin2@school.edu", passwordEncoder.encode("Admin123!"), Role.ADMIN)
            );
            adminRepository.saveAll(admins);
            System.out.println("Admin accounts created successfully!");
        } else {
            System.out.println("Admin accounts already exist. Skipping admin creation.");
        }

        // --- PROGRAMS ---
        List<Program> programs = new ArrayList<>();
        Map<String, Program> programMap = new HashMap<>();
        String[][] programData = {
            {"Bachelor of Science in Information Technology", "BSIT"},
            {"Bachelor of Science in Business Administration", "BSBA"},
            {"Bachelor of Secondary Education", "BSED"}
        };
        for (String[] pd : programData) {
            Program p = programRepository.findByName(pd[0]).orElse(null);
            if (p == null) {
                p = new Program();
                p.setName(pd[0]);
            }
            p.setAcronym(pd[1]);
            p = programRepository.save(p);
            programs.add(p);
            programMap.put(pd[1], p);
        }

        // --- SECTIONS ---
        // Remove all existing sections and recreate
        sectionRepository.deleteAll();
        List<Section> allSections = new ArrayList<>();
        String[] sectionSuffixes = {"A", "B"};
        for (int i = 0; i < programData.length; i++) {
            String acronym = programData[i][1];
            Program program = programMap.get(acronym);
            for (int year = 1; year <= 4; year++) {
                for (String suffix : sectionSuffixes) {
                    String sectionName = acronym + "-" + year + "-" + suffix;
                    Section section = new Section();
                    section.setName(sectionName);
                    section.setProgram(program);
                    section.setYearLevel(year);
                    allSections.add(section);
                }
            }
        }
        allSections = sectionRepository.saveAll(allSections);

        // --- COURSES ---
        // Remove all existing courses and recreate
        courseRepository.deleteAll();
        List<Course> allCourses = new ArrayList<>();
        for (Program program : programs) {
            String acronym = program.getAcronym();
            for (int year = 1; year <= 4; year++) {
                for (int sem = 1; sem <= 2; sem++) {
                    String courseName = acronym + " " + year + "-" + sem;
                    Course course = new Course();
                    course.setName(courseName);
                    course.setYear(year);
                    course.setSemester(sem);
                    course.setProgram(program);
                    allCourses.add(course);
                }
            }
        }
        allCourses = courseRepository.saveAll(allCourses);

        // --- SUBJECTS ---
        // Remove all existing subjects and recreate
        subjectRepository.deleteAll();
        List<Subject> allSubjects = new ArrayList<>();
        // Example subject data for each program/year/sem (should be expanded as needed)
        Map<String, String[][][]> subjectData = new HashMap<>();
        subjectData.put("BSIT", new String[][][] {
            { // Year 1, Semester 1
                {"IT101", "Introduction to Computing"},
                {"IT102", "Computer Programming 1"},
                {"IT103", "Mathematics in the Modern World"},
                {"IT104", "Communication Skills 1"},
                {"IT105", "Physical Education 1"}
            },
            { // Year 1, Semester 2
                {"IT106", "Computer Programming 2"},
                {"IT107", "Digital Literacy"},
                {"IT108", "Purposive Communication"},
                {"IT109", "Physical Education 2"},
                {"IT110", "Philippine History"}
            },
            { // Year 2, Semester 1
                {"IT201", "Web Development"},
                {"IT202", "Database Systems"},
                {"IT203", "Discrete Mathematics"},
                {"IT204", "Object-Oriented Programming"},
                {"IT205", "Physical Education 3"}
            },
            { // Year 2, Semester 2
                {"IT206", "Networking 1"},
                {"IT207", "Data Structures and Algorithms"},
                {"IT208", "Statistics for IT"},
                {"IT209", "Physical Education 4"},
                {"IT210", "Ethics"}
            },
            { // Year 3, Semester 1
                {"IT301", "Mobile Application Development"},
                {"IT302", "Systems Integration and Architecture"},
                {"IT303", "Software Engineering"},
                {"IT304", "Operating Systems"},
                {"IT305", "IT Elective 1"}
            },
            { // Year 3, Semester 2
                {"IT306", "IT Elective 2"},
                {"IT307", "Human Computer Interaction"},
                {"IT308", "Networking 2"},
                {"IT309", "Technopreneurship"},
                {"IT310", "Research Methods"}
            },
            { // Year 4, Semester 1
                {"IT401", "Capstone Project 1"},
                {"IT402", "IT Seminar and Field Trips"},
                {"IT403", "Professional Practice"},
                {"IT404", "Cloud Computing"},
                {"IT405", "IT Elective 3"}
            },
            { // Year 4, Semester 2
                {"IT406", "Capstone Project 2"},
                {"IT407", "Internship"},
                {"IT408", "Emerging Technologies"},
                {"IT409", "IT Elective 4"},
                {"IT410", "Project Management"}
            }
        });
        subjectData.put("BSBA", new String[][][] {
            { // Year 1, Semester 1
                {"BA101", "Principles of Management"},
                {"BA102", "Business Mathematics"},
                {"BA103", "Microeconomics"},
                {"BA104", "Communication Skills 1"},
                {"BA105", "Physical Education 1"}
            },
            { // Year 1, Semester 2
                {"BA106", "Financial Accounting"},
                {"BA107", "Macroeconomics"},
                {"BA108", "Purposive Communication"},
                {"BA109", "Physical Education 2"},
                {"BA110", "Philippine History"}
            },
            { // Year 2, Semester 1
                {"BA201", "Marketing Management"},
                {"BA202", "Business Communication"},
                {"BA203", "Business Law"},
                {"BA204", "Human Resource Management"},
                {"BA205", "Physical Education 3"}
            },
            { // Year 2, Semester 2
                {"BA206", "Management Accounting"},
                {"BA207", "Business Ethics"},
                {"BA208", "Statistics for Business"},
                {"BA209", "Physical Education 4"},
                {"BA210", "Entrepreneurship"}
            },
            { // Year 3, Semester 1
                {"BA301", "Financial Management"},
                {"BA302", "Operations Management"},
                {"BA303", "Organizational Behavior"},
                {"BA304", "Strategic Management"},
                {"BA305", "Business Research 1"}
            },
            { // Year 3, Semester 2
                {"BA306", "Business Research 2"},
                {"BA307", "International Business"},
                {"BA308", "Taxation"},
                {"BA309", "Management Information Systems"},
                {"BA310", "Corporate Social Responsibility"}
            },
            { // Year 4, Semester 1
                {"BA401", "Internship"},
                {"BA402", "Business Plan Development"},
                {"BA403", "Leadership and Teamwork"},
                {"BA404", "Business Analytics"},
                {"BA405", "Business Elective 1"}
            },
            { // Year 4, Semester 2
                {"BA406", "Business Elective 2"},
                {"BA407", "Business Elective 3"},
                {"BA408", "Business Elective 4"},
                {"BA409", "Internship 2"},
                {"BA410", "Comprehensive Exam"}
            }
        });
        subjectData.put("BSED", new String[][][] {
            { // Year 1, Semester 1
                {"ED101", "Foundations of Education"},
                {"ED102", "Child and Adolescent Development"},
                {"ED103", "Communication Skills 1"},
                {"ED104", "Physical Education 1"},
                {"ED105", "Philippine Literature"}
            },
            { // Year 1, Semester 2
                {"ED106", "Facilitating Learner-Centered Teaching"},
                {"ED107", "Technology for Teaching and Learning"},
                {"ED108", "Purposive Communication"},
                {"ED109", "Physical Education 2"},
                {"ED110", "Mathematics in the Modern World"}
            },
            { // Year 2, Semester 1
                {"ED201", "Curriculum Development"},
                {"ED202", "Assessment of Learning 1"},
                {"ED203", "Educational Psychology"},
                {"ED204", "Physical Education 3"},
                {"ED205", "Science, Technology, and Society"}
            },
            { // Year 2, Semester 2
                {"ED206", "Assessment of Learning 2"},
                {"ED207", "Educational Technology"},
                {"ED208", "Physical Education 4"},
                {"ED209", "Contemporary World"},
                {"ED210", "Readings in Philippine History"}
            },
            { // Year 3, Semester 1
                {"ED301", "The Teacher and the School Curriculum"},
                {"ED302", "Field Study 1"},
                {"ED303", "Research in Education 1"},
                {"ED304", "Assessment of Learning 3"},
                {"ED305", "Facilitating Learning"}
            },
            { // Year 3, Semester 2
                {"ED306", "Field Study 2"},
                {"ED307", "Research in Education 2"},
                {"ED308", "Trends and Issues in Education"},
                {"ED309", "Classroom Management"},
                {"ED310", "Educational Assessment"}
            },
            { // Year 4, Semester 1
                {"ED401", "Practice Teaching 1"},
                {"ED402", "Curriculum Implementation"},
                {"ED403", "Educational Leadership"},
                {"ED404", "Guidance and Counseling"},
                {"ED405", "Special Topics in Education"}
            },
            { // Year 4, Semester 2
                {"ED406", "Practice Teaching 2"},
                {"ED407", "Internship"},
                {"ED408", "Educational Technology 2"},
                {"ED409", "Capstone Project"},
                {"ED410", "Comprehensive Exam"}
            }
        });
        for (Program program : programs) {
            String acronym = program.getAcronym();
            String[][][] programSubjects = subjectData.get(acronym);
            if (programSubjects == null) continue;
            for (int y = 1; y <= 4; y++) {
                final int year = y;
                for (int s = 1; s <= 2; s++) {
                    final int sem = s;
                    int idx = (year - 1) * 2 + (sem - 1);
                    Course course = allCourses.stream().filter(c -> c.getProgram().equals(program) && c.getYear() == year && c.getSemester() == sem).findFirst().orElse(null);
                    if (course == null) continue;
                    if (idx < programSubjects.length) {
                        String[][] semSubjects = programSubjects[idx];
                        for (String[] subj : semSubjects) {
                            Subject subject = new Subject();
                            subject.setCode(subj[0]);
                            subject.setName(subj[1]);
                            subject.setUnits(3);
                            subject.setCourse(course);
                            subject.setProgram(program);
                            subject.setYearLevel(year);
                            subject.setSemester(sem);
                            allSubjects.add(subject);
                        }
                    }
                }
            }
        }
        allSubjects = subjectRepository.saveAll(allSubjects);

        // --- SUBJECT SECTIONS ---
        // Remove all existing subject sections and recreate
        subjectSectionRepository.deleteAll();
        List<SubjectSection> allSubjectSections = new ArrayList<>();
        // Define academic years for each year level (removed 2023-2024)
        String[] academicYears = {"2025-2026", "2026-2027", "2027-2028", "2028-2029"};
        // Define available rooms and more varied time slots (with multiple days)
        String[] rooms = {"Room 101", "Room 102", "Room 103", "Room 104", "Room 105", "Room 201", "Room 202", "Room 203", "Room 204", "Room 205"};
        String[] schedules = {
            "MWF 8-9:30", "MWF 9:30-11", "MWF 11-12:30", "MWF 1-2:30", "MWF 2:30-4",
            "TTh 8-9:30", "TTh 9:30-11", "TTh 11-12:30", "TTh 1-2:30", "TTh 2:30-4",
            "MWF 8-9:30 / TTh 10-11:30", "MWF 1-2:30 / TTh 2:30-4", "TTh 8-9:30 / MWF 11-12:30"
        };
        // Track used room+schedule pairs
        Set<String> usedRoomSchedule = new HashSet<>();
        int roomIdx = 0, schedIdx = 0;
        for (Subject subject : allSubjects) {
            int yearLevel = subject.getYearLevel();
            if (yearLevel < 1 || yearLevel > academicYears.length) continue;
            String academicYear = academicYears[yearLevel - 1];
            List<Section> matchingSections = allSections.stream()
                .filter(section -> 
                    section.getProgram().getId().equals(subject.getProgram().getId()) &&
                    Objects.equals(section.getYearLevel(), subject.getYearLevel())
                )
                .collect(Collectors.toList());
            for (Section section : matchingSections) {
                SubjectSection ss = new SubjectSection();
                ss.setSubject(subject);
                ss.setSection(section);
                ss.setAcademicYear(academicYear);
                ss.setSemester(subject.getSemester());
                // Assign a unique room+schedule pair with more variety
                String room, schedule, key;
                int attempts = 0;
                do {
                    room = rooms[roomIdx % rooms.length];
                    schedule = schedules[schedIdx % schedules.length];
                    key = room + "@" + schedule;
                    schedIdx++;
                    if (schedIdx % schedules.length == 0) roomIdx++;
                    attempts++;
                } while (usedRoomSchedule.contains(key) && attempts < rooms.length * schedules.length);
                usedRoomSchedule.add(key);
                ss.setRoom(room);
                ss.setSchedule(schedule);
                ss.setMaxStudents(40);
                allSubjectSections.add(ss);
            }
        }
        subjectSectionRepository.saveAll(allSubjectSections);

        // Only load faculty and students if they don't exist
        if (facultyRepository.count() == 0) {
            System.out.println("Loading faculty data...");
            // Get existing programs for faculty assignments
            programs = programRepository.findAll();
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
            // Assign teaching assignments (skip 2023-2024, use only academicYears[0] or later)
            String currentAcademicYear = academicYears[0];
            assignTeachingLoad(csProf1, programs.get(0), 1, 1, currentAcademicYear); // First year, first sem CS subjects
            assignTeachingLoad(csProf2, programs.get(0), 1, 2, currentAcademicYear); // First year, second sem CS subjects
            assignTeachingLoad(busProf1, programs.get(1), 1, 1, currentAcademicYear); // First year, first sem Business subjects
            assignTeachingLoad(busProf2, programs.get(1), 1, 2, currentAcademicYear); // First year, second sem Business subjects
            assignTeachingLoad(engProf1, programs.get(2), 1, 1, currentAcademicYear); // First year, first sem Engineering subjects
            assignTeachingLoad(engProf2, programs.get(2), 1, 2, currentAcademicYear); // First year, second sem Engineering subjects
            System.out.println("=== Faculty Data Loaded ===");
            System.out.println("Faculty: " + faculty.size());
            System.out.println("Faculty Programs: " + facultyProgramRepository.count());
            System.out.println("Teaching Assignments: " + teachingAssignmentRepository.count());
        } else {
            System.out.println("Faculty already exist. Skipping faculty creation.");
        }

        System.out.println("=== Database Loading Complete ===");
        System.out.println("Admin accounts available:");
        adminRepository.findAll().forEach(admin -> 
            System.out.println("  - " + admin.getEmail() + " (Password: Admin123!)"));
        System.out.println("Programs: " + programRepository.count());
        System.out.println("Subjects: " + subjectRepository.count());
        System.out.println("Courses: " + courseRepository.count());
        System.out.println("Sections: " + sectionRepository.count());
        System.out.println("Subject Sections: " + subjectSectionRepository.count());
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

    private Program createProgramWithFullCurriculum(String name, Map<Integer, Map<Integer, List<String>>> yearData, List<Section> sections, String academicYear) {
        Program program = new Program(name);
        String acronym = Arrays.stream(name.split(" ")).map(s -> String.valueOf(s.charAt(0))).collect(Collectors.joining());
        program.setAcronym(acronym);

        List<Course> programCourses = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, List<String>>> yearEntry : yearData.entrySet()) {
            int year = yearEntry.getKey();
            for (Map.Entry<Integer, List<String>> semesterEntry : yearEntry.getValue().entrySet()) {
                int semester = semesterEntry.getKey();
                String courseName = String.format("%s - Year %d, Semester %d", name, year, semester);
                Course course = new Course(courseName, year, semester, program);

                List<Subject> subjects = new ArrayList<>();
                for (String subjectName : semesterEntry.getValue()) {
                    Subject subject = new Subject(
                        generateSubjectCode(name, subjectName),
                        subjectName,
                        3, // Default units
                        course
                    );
                    subject.setProgram(program);
                    subject.setYearLevel(year);
                    subject.setSemester(semester);
                    
                    // Create SubjectSections for this subject for each section
                    List<SubjectSection> subjectSections = new ArrayList<>();
                    for (Section section : sections) {
                        subjectSections.add(new SubjectSection(subject, section, academicYear, semester));
                    }
                    subject.setSubjectSections(subjectSections);

                    subjects.add(subject);
                }
                course.setSubjects(subjects);
                programCourses.add(course);
            }
        }
        program.setCourses(programCourses);
        return program;
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
        List<Section> sections = sectionRepository.findAll(); // You may want to filter by program/year/semester
        
        for (Subject subject : subjects) {
            for (Section section : sections) {
                // Check if SubjectSection already exists for this combination
                Optional<SubjectSection> existingSubjectSection = subjectSectionRepository
                    .findBySubjectIdAndSectionIdAndAcademicYearAndSemester(
                        subject.getId(), section.getId(), academicYear, semester);
                
                SubjectSection subjectSection;
                if (existingSubjectSection.isPresent()) {
                    subjectSection = existingSubjectSection.get();
                } else {
                    // Create new SubjectSection
                    subjectSection = new SubjectSection();
                    subjectSection.setSubject(subject);
                    subjectSection.setSection(section);
                    subjectSection.setAcademicYear(academicYear);
                    subjectSection.setSemester(semester);
                    subjectSection.setSchedule("TBD"); // Default schedule
                    subjectSection.setRoom("TBD"); // Default room
                    subjectSection.setMaxStudents(30); // Default max students
                    subjectSection = subjectSectionRepository.save(subjectSection);
                }
                
                // Create TeachingAssignment
                TeachingAssignment ta = new TeachingAssignment();
                ta.setFaculty(faculty);
                ta.setSubjectSection(subjectSection);
                teachingAssignmentRepository.save(ta);
            }
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