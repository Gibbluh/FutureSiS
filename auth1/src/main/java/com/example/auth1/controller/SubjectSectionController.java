package com.example.auth1.controller;

import com.example.auth1.model.*;
import com.example.auth1.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.example.auth1.model.SubjectDTO;

@Controller
@RequestMapping("/admin")
public class SubjectSectionController {
    private static final Logger logger = LoggerFactory.getLogger(SubjectSectionController.class);

    private final SubjectSectionRepository subjectSectionRepository;
    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final GradeRepository gradeRepository;

    public SubjectSectionController(SubjectSectionRepository subjectSectionRepository,
                                   SubjectRepository subjectRepository,
                                   SectionRepository sectionRepository,
                                   TeachingAssignmentRepository teachingAssignmentRepository,
                                   FacultyRepository facultyRepository,
                                   ProgramRepository programRepository,
                                   GradeRepository gradeRepository) {
        this.subjectSectionRepository = subjectSectionRepository;
        this.subjectRepository = subjectRepository;
        this.sectionRepository = sectionRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.facultyRepository = facultyRepository;
        this.programRepository = programRepository;
        this.gradeRepository = gradeRepository;
    }

    @GetMapping("/subject-sections/{id}/assign-faculty")
    public String assignFacultyForm(@PathVariable Long id, Model model) {
        try {
            SubjectSection subjectSection = subjectSectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject Section not found"));
            
            List<Faculty> faculties = facultyRepository.findAll();
            
            model.addAttribute("subjectSection", subjectSection);
            model.addAttribute("faculties", faculties);
            
            return "admin/assign_faculty";
        } catch (Exception e) {
            logger.error("Error loading assign faculty form: {}", e.getMessage(), e);
            model.addAttribute("message", "Error loading assign faculty form");
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/subject-sections/{id}/assign-faculty")
    public String assignFaculty(@PathVariable Long id,
                               @RequestParam Long facultyId,
                               RedirectAttributes redirectAttributes) {
        try {
            SubjectSection subjectSection = subjectSectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject Section not found"));
            
            Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            // Check if assignment already exists
            if (teachingAssignmentRepository.existsByFacultyIdAndSubjectSectionId(facultyId, id)) {
                throw new RuntimeException("Faculty is already assigned to this subject section");
            }
            
            TeachingAssignment teachingAssignment = new TeachingAssignment(faculty, subjectSection);
            teachingAssignmentRepository.save(teachingAssignment);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Faculty assigned successfully to " + subjectSection.getSubject().getCode() + 
                " - " + subjectSection.getSection().getName());
            
            return "redirect:/admin/subject-sections";
        } catch (Exception e) {
            logger.error("Error assigning faculty: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error assigning faculty: " + e.getMessage());
            return "redirect:/admin/subject-sections/" + id + "/assign-faculty";
        }
    }

    @GetMapping("/subject-sections/{id}/grades")
    public String viewSectionGrades(@PathVariable Long id, Model model) {
        try {
            SubjectSection subjectSection = subjectSectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject Section not found"));
            
            List<Grade> grades = gradeRepository.findBySubjectSectionIdWithStudentDetails(id);
            
            model.addAttribute("subjectSection", subjectSection);
            model.addAttribute("grades", grades);
            
            return "admin/section_grades";
        } catch (Exception e) {
            logger.error("Error loading section grades: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading grades: " + e.getMessage());
            return "admin/section_grades";
        }
    }

    @PostMapping("/subject-sections/{sectionId}/remove-faculty/{facultyId}")
    public String removeFaculty(@PathVariable Long sectionId,
                              @PathVariable Long facultyId,
                              RedirectAttributes redirectAttributes) {
        try {
            SubjectSection subjectSection = subjectSectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Subject Section not found"));
            
            Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
            
            TeachingAssignment assignment = teachingAssignmentRepository
                .findByFacultyIdAndSubjectSectionId(facultyId, sectionId)
                .orElseThrow(() -> new RuntimeException("Teaching assignment not found"));
            
            teachingAssignmentRepository.delete(assignment);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Faculty " + faculty.getFirstName() + " " + faculty.getLastName() + 
                " removed from " + subjectSection.getSubject().getCode() + 
                " - " + subjectSection.getSection().getName());
            
            return "redirect:/admin/subject-sections";
        } catch (Exception e) {
            logger.error("Error removing faculty: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error removing faculty: " + e.getMessage());
            return "redirect:/admin/subject-sections/" + sectionId + "/assign-faculty";
        }
    }

    @PostMapping("/subject-sections/{id}/delete")
    public String deleteSubjectSection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SubjectSection subjectSection = subjectSectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject Section not found"));
            
            // Delete associated teaching assignments and grades
            teachingAssignmentRepository.deleteBySubjectSectionId(id);
            gradeRepository.deleteBySubjectSectionId(id);
            
            // Delete the subject section
            subjectSectionRepository.delete(subjectSection);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Subject Section deleted successfully: " + subjectSection.getSubject().getCode() + 
                " - " + subjectSection.getSection().getName());
            
            return "redirect:/admin/subject-sections";
        } catch (Exception e) {
            logger.error("Error deleting subject section: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting subject section: " + e.getMessage());
            return "redirect:/admin/subject-sections";
        }
    }

    // Add REST endpoint for dynamic subject filtering
    @GetMapping("/subjects/filter")
    @ResponseBody
    public List<SubjectDTO> filterSubjectsByProgramAndSemesterAndYearLevel(@RequestParam(required = false) Long programId,
                                                                          @RequestParam(required = false) Integer semester,
                                                                          @RequestParam(required = false) Integer yearLevel) {
        return subjectRepository.findByProgramIdAndSemesterAndYearLevel(programId, semester, yearLevel)
            .stream()
            .map(s -> new SubjectDTO(s.getId(), s.getCode(), s.getName(), s.getYearLevel(), s.getSemester()))
            .collect(Collectors.toList());
    }

    @GetMapping("/sections/by-program")
    @ResponseBody
    public List<Section> getSectionsByProgram(@RequestParam Long programId) {
        return sectionRepository.findByProgramId(programId);
    }
} 