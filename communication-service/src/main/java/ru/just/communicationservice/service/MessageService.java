package ru.just.communicationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.just.communicationservice.dto.MessageDto;
import ru.just.communicationservice.dto.PageDto;
import ru.just.communicationservice.model.Message;
import ru.just.communicationservice.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public List<MessageDto> getMessagesForChat(String chatId, Pageable pageable) {
        // todo: вернуть сохарненные сообщения в чате
        return null;
    }

    public void saveMessage(UUID chatId, Long senderId, MessageDto messageDto) {
        Message message = new Message();
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setContent(messageDto.getContent());
        message.setSentAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    public Page<Message> loadMessages(UUID chatId, PageDto pageDto) {
        Pageable pageable = PageRequest.of(pageDto.getPage(), pageDto.getSize());
        return messageRepository.findByChatIdOrderBySentAtDesc(chatId, pageable);
    }
}
