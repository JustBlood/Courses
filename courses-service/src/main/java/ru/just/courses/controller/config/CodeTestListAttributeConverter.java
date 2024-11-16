package ru.just.courses.controller.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.just.courses.model.theme.lesson.CodeLesson;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class CodeTestListAttributeConverter implements AttributeConverter<List<CodeLesson.CodeTest>, String> {
    private static final String SPLIT_CHAR = ";";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<CodeLesson.CodeTest> codeTests) {
        return codeTests.stream()
                .map(test -> {
                    try {
                        return objectMapper.writeValueAsString(test);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.joining(SPLIT_CHAR));
    }

    @Override
    public List<CodeLesson.CodeTest> convertToEntityAttribute(String string) {
        return Arrays.stream(string.split(SPLIT_CHAR))
                .map(test -> {
                    try {
                        return objectMapper.readValue(test, CodeLesson.CodeTest.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }
}
