package ru.just.mentorcatalogservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.just.mentorcatalogservice.dto.CreateMentorDto;
import ru.just.mentorcatalogservice.dto.MentorCardDto;
import ru.just.mentorcatalogservice.dto.MentorDto;
import ru.just.mentorcatalogservice.dto.StudentDto;
import ru.just.mentorcatalogservice.dto.mapper.MentorMapper;
import ru.just.mentorcatalogservice.model.Mentor;
import ru.just.mentorcatalogservice.repository.MentorRepository;

@Service
@RequiredArgsConstructor
public class MentorService {
    private final MentorRepository mentorRepository;
    private final MentorMapper mentorMapper;

    public MentorDto createMentor(CreateMentorDto createMentorDto) {
        return mentorRepository.createMentor(createMentorDto);
    }

    public void addStudentToMentor(Long mentorId, StudentDto studentDto) {
        mentorRepository.addStudentToMentor(mentorId, studentDto);
    }

    public Boolean isMentor(Long userId) {
        return mentorRepository.isMentorExists(userId);
    }

    public Page<MentorCardDto> getMentorsCardsBySpecializationPart(String specialization, Pageable pageable) {
        Page<Mentor> mentors = mentorRepository.findMentorsBySpecialization(specialization, pageable);
        final Page<MentorCardDto> cards = mentors.map(mentorMapper::toCardDto);
        // todo: заполнить карточки данными с сервиса пользователей
        return cards;
    }

    public boolean isStudentHasMentor(Long studentId, Long mentorId) {
        return mentorRepository.isStudentHasMentor(studentId, mentorId);
    }
}
