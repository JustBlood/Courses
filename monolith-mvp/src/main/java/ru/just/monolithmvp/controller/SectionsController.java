package ru.just.monolithmvp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CreateCourseRequest;
import ru.just.monolithmvp.dto.section.CreateSectionRequest;
import ru.just.monolithmvp.dto.section.SectionDto;
import ru.just.monolithmvp.dto.section.UpdateSectionRequest;
import ru.just.monolithmvp.service.CourseService;
import ru.just.monolithmvp.service.SectionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SectionsController {
    private final SectionService sectionService;
    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<SectionDto> create(@Valid @RequestBody CreateSectionRequest request) {
        return new ResponseEntity<>(sectionService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SectionDto>> getAll() {
        return ResponseEntity.ok(sectionService.getAll());
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<SectionDto> getById(@PathVariable Long sectionId) {
        return ResponseEntity.ok(sectionService.getById(sectionId));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<SectionDto> update(@PathVariable Long sectionId,
                                             @Valid @RequestBody UpdateSectionRequest request) {
        return ResponseEntity.ok(sectionService.update(sectionId, request));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long sectionId) {
        sectionService.delete(sectionId);
        return ResponseEntity.ok(new ApiResponse("Section deleted"));
    }

    @PostMapping("/{sectionId}/courses")
    public ResponseEntity<CourseDto> createCourseInSection(@PathVariable Long sectionId,
                                                           @Valid @RequestBody CreateCourseRequest request) {
        return new ResponseEntity<>(courseService.createCourseInSection(sectionId, request), HttpStatus.CREATED);
    }
}
