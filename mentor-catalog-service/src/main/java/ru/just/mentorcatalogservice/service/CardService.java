package ru.just.mentorcatalogservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.just.mentorcatalogservice.dto.MentorCardDto;

@Service
@RequiredArgsConstructor
public class CardService {
    private final MentorService mentorService;

    public Page<MentorCardDto> getMentorCardsPage(String specialization, Pageable pageable) {
        // todo: запрос в БД для инфы о менторе
        // todo: запрос в Пользователей на данные пользователя
        Page<MentorCardDto> mentorDtos = mentorService.getMentorsCardsBySpecializationPart(specialization, pageable);
        return mentorDtos;
    }
}
