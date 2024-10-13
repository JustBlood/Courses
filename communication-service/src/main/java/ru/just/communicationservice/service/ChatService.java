package ru.just.communicationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.communicationservice.dto.ChatDto;
import ru.just.communicationservice.dto.CreateChatDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    public ChatDto createChat(CreateChatDto createChatDto) {
        // todo: логика создания чатов - обращение к userService для получения инфы о пользователях (для отображения в чате)
        return null;
    }

    public List<ChatDto> getUserChats(String userId) {
        //todo: получить чаты пользователя
        return null;
    }

    public ChatDto getChat(String chatId) {
        // получить чат по id
        return null;
    }
}
