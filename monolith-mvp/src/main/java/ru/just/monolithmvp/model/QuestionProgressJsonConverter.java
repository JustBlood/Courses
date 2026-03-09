package ru.just.monolithmvp.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class QuestionProgressJsonConverter implements AttributeConverter<List<QuestionProgress>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<QuestionProgress> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize question progress json", e);
        }
    }

    @Override
    public List<QuestionProgress> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData.trim(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize question progress json", e);
        }
    }
}
