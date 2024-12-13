package ru.just.mentorcatalogservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.just.mentorcatalogservice.dto.CreateMentorDto;
import ru.just.mentorcatalogservice.dto.MentorDto;
import ru.just.mentorcatalogservice.dto.StudentDto;
import ru.just.mentorcatalogservice.dto.mapper.MentorMapper;
import ru.just.mentorcatalogservice.dto.user.UserDto;
import ru.just.mentorcatalogservice.model.Mentor;
import ru.just.mentorcatalogservice.repository.MentorRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorService {
    private final MentorRepository mentorRepository;
    private final MentorMapper mentorMapper;
    private final RestTemplate restTemplate;

    @Value("${service-discovery.users-service.name}")
    private String usersServiceName;

    public MentorDto createMentor(CreateMentorDto createMentorDto) {
        if (getUsersInfoFromUsersServices(createMentorDto.getUserId()) != null) {
            throw new NoSuchElementException("Пользователя с предоствленным id не существует.");
        }
        final Mentor mentor = mentorRepository.createMentor(createMentorDto);
        return mentorMapper.toDto(mentor);
    }

    public void addStudentToMentor(Long mentorId, StudentDto studentDto) {
        mentorRepository.addStudentToMentor(mentorId, studentDto);
    }

    public Boolean isMentor(Long userId) {
        return mentorRepository.isMentorExists(userId);
    }

    public Page<MentorDto> getMentorsCardsBySpecializationPart(List<String> specialization, Pageable pageable) {
        Page<Mentor> mentors = mentorRepository.findMentorsBySpecialization(specialization, pageable);
        return mentors.map(mentorMapper::toDto);
    }

    public boolean isStudentHasMentor(Long studentId, Long mentorId) {
        return mentorRepository.isStudentHasMentor(studentId, mentorId);
    }
    private UserDto getUsersInfoFromUsersServices(Long userId) {
        final String uriTemplate = String.format("http://%s/api/v1/users/internal/%s", usersServiceName, userId);
        final HttpHeaders headers = buildHeaders();
        final RequestEntity<Void> requestEntity = RequestEntity.get(uriTemplate)
                .headers(headers)
                .build();
        try {
            return restTemplate.exchange(requestEntity, UserDto.class).getBody();
        } catch (RestClientException e) {
            log.error("Error while requesting user service get users info by ids", e);
            throw new IllegalStateException();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
