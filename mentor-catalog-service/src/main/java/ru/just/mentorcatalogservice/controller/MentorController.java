package ru.just.mentorcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.mentorcatalogservice.dto.CreateMentorDto;
import ru.just.mentorcatalogservice.dto.MentorDto;
import ru.just.mentorcatalogservice.dto.StudentDto;
import ru.just.mentorcatalogservice.service.MentorService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/mentors/internal")
public class MentorController {
    private final MentorService mentorService;

    // добавить ментора в систему
    @PostMapping
    public ResponseEntity<MentorDto> createMentor(@RequestBody CreateMentorDto createMentorDto) {
        return ResponseEntity.ok(mentorService.createMentor(createMentorDto));
    }
    // добавить студента ментору
    @PatchMapping("/{mentorId}/students/add")
    public ResponseEntity<Void> addStudentToMentor(@PathVariable Long mentorId, @RequestBody StudentDto studentDto) {
        mentorService.addStudentToMentor(mentorId, studentDto);
        return ResponseEntity.ok().build();
    }
    // учится ли студент у ментора?
    @GetMapping("/{mentorId}/students/{studentId}/is-student")
    public ResponseEntity<Boolean> isStudentHasMentor(@PathVariable Long mentorId, @PathVariable Long studentId) {
        return ResponseEntity.ok(mentorService.isStudentHasMentor(studentId, mentorId));
    }
    // является ли пользователь ментором?
    @GetMapping("/{userId}/is-mentor")
    public ResponseEntity<Boolean> isMentor(@PathVariable Long userId) {
        return ResponseEntity.ok(mentorService.isMentor(userId));
    }
}
