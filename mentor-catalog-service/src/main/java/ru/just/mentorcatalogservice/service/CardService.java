package ru.just.mentorcatalogservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.just.mentorcatalogservice.dto.MentorCardDto;
import ru.just.mentorcatalogservice.dto.MentorDto;
import ru.just.mentorcatalogservice.dto.user.UserDto;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {
    private final MentorService mentorService;
    private final RestTemplate restTemplate;
    private final ThreadLocalTokenService tokenService;

    @Value("${service-discovery.users-service.name}")
    private String usersServiceName;


    public Page<MentorCardDto> getMentorCardsPage(List<String> specialization, Pageable pageable) {
        // todo: запрос в БД для инфы о менторе
        // todo: запрос в Пользователей на данные пользователя
        final Page<MentorDto> mentors = mentorService.getMentorsCardsBySpecializationPart(specialization, pageable);

        Map<Long, UserDto> userInfoById = getUsersInfoFromUsersServices(mentors.stream().map(MentorDto::getUserId).collect(Collectors.toList()));
        return buildMentorCardDtos(mentors, userInfoById);
    }

    private Page<MentorCardDto> buildMentorCardDtos(Page<MentorDto> mentors, Map<Long, UserDto> userInfoById) {
        final List<MentorCardDto> cards = mentors.stream()
                .filter(mentor -> userInfoById.containsKey(mentor.getUserId()))
                .map(mentor -> {
                    MentorCardDto cardDto = new MentorCardDto();
                    final UserDto currentUserDto = userInfoById.get(mentor.getUserId());
                    cardDto.setFirstName(currentUserDto.getFirstName());
                    cardDto.setLastName(currentUserDto.getLastName());
                    cardDto.setUsername(currentUserDto.getUsername());
                    cardDto.setAvatarUrl(currentUserDto.getPhotoUrl());
                    cardDto.setSpecializations(mentor.getSpecializations());
                    cardDto.setShortAboutMe(mentor.getShortAboutMe());
                    cardDto.setLongAboutMe(mentor.getLongAboutMe());
                    cardDto.setStudentsCount(mentor.getStudentsCount());
                    cardDto.setUserId(mentor.getUserId());

                    return cardDto;
                }).collect(Collectors.toList());
        return new PageImpl<>(cards, mentors.getPageable(), cards.size());
    }

    private Map<Long,UserDto> getUsersInfoFromUsersServices(List<Long> userIds) {
        final String uriTemplate = String.format("http://%s/api/v1/users?ids=%s",
                usersServiceName,
                userIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","))
        );
        final HttpHeaders headers = buildHeaders();
        final RequestEntity<Void> requestEntity = RequestEntity.get(uriTemplate)
                .headers(headers)
                .build();
        try {
            final List<UserDto> userDtos = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<List<UserDto>>() {}).getBody();
            if (userDtos == null) {
                return Collections.emptyMap();
            }
            return userDtos.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
        } catch (RestClientException e) {
            log.error("Error while requesting user service get users info by ids", e);
            throw new IllegalStateException();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, tokenService.getDecodedToken().getToken());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
