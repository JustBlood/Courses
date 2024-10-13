package ru.just.communicationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.communicationservice.dto.MessageDto;

import java.awt.print.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    public List<MessageDto> getMessagesForChat(String chatId, Pageable pageable) {
        // todo: вернуть сохарненные сообщения в чате
        return null;
    }
}
