package com.example.auth1.service;

import com.example.auth1.model.Program;
import com.example.auth1.model.Section;
import com.example.auth1.repository.ProgramRepository;
import com.example.auth1.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ProgramRepository programRepository;

    @Autowired
    public SectionService(SectionRepository sectionRepository, ProgramRepository programRepository) {
        this.sectionRepository = sectionRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public void createSection(String sectionName, Long programId, Integer yearLevel) {
        // Optional: Check for duplicates
        if (sectionRepository.existsByNameAndProgramId(sectionName, programId)) {
            throw new RuntimeException("Section with name '" + sectionName + "' already exists for this program.");
        }
        
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found with ID: " + programId));
        
        Section section = new Section();
        section.setName(sectionName);
        section.setProgram(program);
        section.setYearLevel(yearLevel);
        
        sectionRepository.save(section);
    }

    @Transactional
    public Section updateSection(Long sectionId, String name, Long programId, Integer yearLevel) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found with id: " + sectionId));

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found with id: " + programId));

        // Check if a section with the new name already exists in the same program (and is not the current section)
        sectionRepository.findByNameAndProgramId(name, programId).ifPresent(existingSection -> {
            if (!existingSection.getId().equals(sectionId)) {
                throw new RuntimeException("A section with the name '" + name + "' already exists in this program.");
            }
        });

        section.setName(name);
        section.setProgram(program);
        section.setYearLevel(yearLevel);
        return sectionRepository.save(section);
    }

    @Cacheable("sectionList")
    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    @Cacheable("sectionListDTO")
    public java.util.List<com.example.auth1.model.SectionListDTO> getAllSectionListDTOs() {
        return sectionRepository.findAllForListView();
    }
} 