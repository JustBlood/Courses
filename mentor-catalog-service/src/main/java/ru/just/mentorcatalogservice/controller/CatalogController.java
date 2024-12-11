package ru.just.mentorcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.just.mentorcatalogservice.dto.MentorCardDto;
import ru.just.mentorcatalogservice.service.CardService;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/mentors/catalog")
@RestController
public class CatalogController {
    private final CardService cardService;

    // получить информацию о менторах для страницы карточек
    // инфа выдается вообще вся, но отображаться будет не фулл
    @GetMapping
    public ResponseEntity<Page<MentorCardDto>> getMentorCards(@RequestParam(required = false) List<String> specialization, Pageable pageable) {
        if (specialization == null) specialization = List.of("");
        return ResponseEntity.ok(cardService.getMentorCardsPage(specialization, pageable));
    }
}
