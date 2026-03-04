package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.section.CreateSectionRequest;
import ru.just.monolithmvp.dto.section.SectionDto;
import ru.just.monolithmvp.dto.section.UpdateSectionRequest;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.Course;
import ru.just.monolithmvp.model.Section;
import ru.just.monolithmvp.repository.CourseRepository;
import ru.just.monolithmvp.repository.SectionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionService {
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public SectionDto create(CreateSectionRequest request) {
        Section section = new Section();
        section.setTitle(request.title());
        section.setDescription(request.description());
        section.setPriority(request.priority());
        return toDto(sectionRepository.save(section));
    }

    @Transactional(readOnly = true)
    public List<SectionDto> getAll() {
        return sectionRepository.findAllByOrderByPriorityAscIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectionDto getById(Long sectionId) {
        return toDto(getSectionEntity(sectionId));
    }

    @Transactional
    public SectionDto update(Long sectionId, UpdateSectionRequest request) {
        Section section = getSectionEntity(sectionId);
        section.setTitle(request.title());
        section.setDescription(request.description());
        section.setPriority(request.priority());
        return toDto(sectionRepository.save(section));
    }

    @Transactional
    public void delete(Long sectionId) {
        Section section = getSectionEntity(sectionId);
        List<Course> courses = courseRepository.findBySectionId(sectionId);
        courses.forEach(course -> course.setSection(getDefaultSection()));
        if (!courses.isEmpty()) {
            courseRepository.saveAll(courses);
        }
        sectionRepository.delete(section);
    }

    @Transactional(readOnly = true)
    public Section getSectionEntity(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found: " + sectionId));
    }

    private SectionDto toDto(Section section) {
        return new SectionDto(
                section.getId(),
                section.getTitle(),
                section.getDescription(),
                section.getPriority()
        );
    }

    // Единственная секция, которая может быть с приоритетом -1 - это стандартная, которая создается в миграциях
    public Section getDefaultSection() {
        return sectionRepository.findByPriority(-1);
    }
}
