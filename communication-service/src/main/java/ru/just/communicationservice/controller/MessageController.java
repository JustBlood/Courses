package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.communicationservice.dto.MessageDto;
import ru.just.communicationservice.service.MessageService;

import java.awt.print.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    // Получить сообщения в чате с пагинацией
    @GetMapping("/{chatId}")
    public ResponseEntity<List<MessageDto>> getMessagesForChat(@PathVariable String chatId, Pageable pageable) {
        return ResponseEntity.ok(messageService.getMessagesForChat(chatId, pageable));
    }
}
