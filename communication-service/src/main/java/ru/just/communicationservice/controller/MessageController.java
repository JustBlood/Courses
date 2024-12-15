package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.communicationservice.dto.PageDto;
import ru.just.communicationservice.dto.RestMessageDto;
import ru.just.communicationservice.service.MessageService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping("/{chatId}")
    public ResponseEntity<Page<RestMessageDto>> getMessagesForChat(@PathVariable UUID chatId, @RequestParam("page") int page, @RequestParam("size") int size) {
        return ResponseEntity.ok(messageService.loadMessages(chatId, new PageDto(page, size)));
    }
}
