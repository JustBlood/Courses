package ru.just.communicationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.just.communicationservice.dto.MessageDto;
import ru.just.communicationservice.dto.PageDto;
import ru.just.communicationservice.dto.RestMessageDto;
import ru.just.communicationservice.model.Message;
import ru.just.communicationservice.repository.MessageRepository;
import ru.just.communicationservice.service.integration.MediaIntegrationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final MediaIntegrationService mediaIntegrationService;

    public List<MessageDto> getMessagesForChat(String chatId, Pageable pageable) {
        // todo: вернуть сохарненные сообщения в чате
        return null;
    }

    public void saveMessage(UUID chatId, Long senderId, Message.MessageBody body) {
        Message message = new Message();
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setContent(body);
        message.setMessageType(body.getType());
        message.setSentAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    public Page<RestMessageDto> loadMessages(UUID chatId, PageDto pageDto) {
        Pageable pageable = PageRequest.of(pageDto.getPage(), pageDto.getSize());
        final Page<Message> messages = messageRepository.findByChatIdOrderBySentAtDesc(chatId, pageable);
        final List<RestMessageDto> restMessageDtos = messages.stream().map(m -> {
                    switch (m.getMessageType()) {
                        case ATTACHMENT -> {
                            final Message.AttachmentMessageBody content = (Message.AttachmentMessageBody) m.getContent();
                            String fileUrl = mediaIntegrationService.getPresignedUrlForAttachment(content.getFileId());
                            return new RestMessageDto(m, Map.of("fileUrl", fileUrl));
                        }
                        case SIMPLE_MESSAGE -> {
                            final Message.SimpleMessageBody content = (Message.SimpleMessageBody) m.getContent();
                            return new RestMessageDto(m, Map.of("message", content.getMessage()));
                        }
                        case null, default -> throw new IllegalStateException();
                    }
                })
                .collect(Collectors.toList());
        return new PageImpl<>(restMessageDtos, messages.getPageable(), messages.getTotalElements());
    }
}
