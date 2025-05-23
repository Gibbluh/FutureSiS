package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AcademicController {

    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;

    public AcademicController(ProgramRepository programRepository,
                         CourseRepository courseRepository,
                         SubjectRepository subjectRepository) {
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
    }

    // Program Management
    @GetMapping("/programs")
    @Transactional(readOnly = true)
    public String managePrograms(Model model) {
        List<Program> programs = programRepository.findAll();
        programs.sort(Comparator.comparing(Program::getId));
        model.addAttribute("programs", programs);
        return "admin/manage_programs";
    }

    @GetMapping("/programs/add")
    public String showAddProgramPage(Model model) {
        model.addAttribute("program", new Program());
        return "admin/add_program";
    }

    @PostMapping("/programs/add")
    @Transactional
    public String addProgram(@RequestParam String name, RedirectAttributes redirectAttributes) {
        try {
            Program program = new Program(name);
            program = programRepository.save(program);
            
            for (int year = 1; year <= 4; year++) {
                for (int semester = 1; semester <= 2; semester++) {
                    Course course = new Course();
                    course.setYear(year);
                    course.setSemester(semester);
                    course.setProgram(program);
                    courseRepository.save(course);
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Program added successfully with 4-year course structure!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error adding program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    @GetMapping("/programs/edit/{id}")
    @Transactional(readOnly = true)
    public String showEditProgramPage(@PathVariable Long id, Model model) {
        try {
            Program program = programRepository.findByIdWithCourses(id)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            
            if (program.getCourses() != null) {
                program.setCourses(program.getCourses().stream()
                    .sorted(Comparator.comparingInt(Course::getYear)
                        .thenComparingInt(Course::getSemester))
                    .collect(Collectors.toList()));
            }
            
            model.addAttribute("program", program);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading program: " + e.getMessage());
        }
        return "admin/edit_program";
    }

    @PostMapping("/programs/edit/{id}")
    @Transactional
    public String editProgram(
            @PathVariable Long id,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program not found"));
            program.setName(name);
            programRepository.save(program);
            redirectAttributes.addFlashAttribute("successMessage", "Program updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    @PostMapping("/programs/delete/{id}")
    @Transactional
    public String deleteProgram(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            programRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Program deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting program: " + e.getMessage());
        }
        return "redirect:/admin/programs";
    }

    // Course Management
    @GetMapping("/programs/{programId}/courses")
    @Transactional(readOnly = true)
    public String viewProgramCourses(@PathVariable Long programId, Model model) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new RuntimeException("Program not found"));
        List<Course> courses = courseRepository.findByProgramIdOrderByYearAscSemesterAsc(programId);
        
        model.addAttribute("program", program);
        model.addAttribute("courses", courses);
        return "admin/program_courses";
    }

    // Subject Management 
    @GetMapping("/courses/{courseId}/subjects")
    @Transactional(readOnly = true)
    public String viewCourseSubjects(@PathVariable Long courseId, Model model) {
        try {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            List<Subject> subjects = subjectRepository.findByCourseId(courseId);
            
            model.addAttribute("course", course);
            model.addAttribute("subjects", subjects);
            return "admin/course_subjects";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading subjects: " + e.getMessage());
            return "redirect:/admin/programs";
        }
    }

    @GetMapping("/courses/{courseId}/subjects/add")
    public String showAddSubjectForm(@PathVariable Long courseId, Model model) {
        try {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            Subject subject = new Subject();
            subject.setCourse(course);
            
            model.addAttribute("course", course);
            model.addAttribute("subject", subject);
            return "admin/add_subject";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading form: " + e.getMessage());
            return "redirect:/admin/courses/" + courseId + "/subjects";
        }
    }

    @PostMapping("/courses/{courseId}/subjects/add")
    @Transactional
    public String addSubject(
            @PathVariable Long courseId,
            @ModelAttribute("subject") Subject subject,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            if (result.hasErrors()) {
                model.addAttribute("course", course);
                return "admin/add_subject";
            }
            
            subject.setCourse(course);
            subjectRepository.save(subject);
            redirectAttributes.addFlashAttribute("success", "Subject added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding subject: " + e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/subjects";
    }

    @PostMapping("/courses/{courseId}/subjects/delete/{subjectId}")
    @Transactional
    public String deleteSubject(
            @PathVariable Long courseId,
            @PathVariable Long subjectId,
            RedirectAttributes redirectAttributes) {
        try {
            subjectRepository.deleteById(subjectId);
            redirectAttributes.addFlashAttribute("success", "Subject deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting subject: " + e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/subjects";
    }
}