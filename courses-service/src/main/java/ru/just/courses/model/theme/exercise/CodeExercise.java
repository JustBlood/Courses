package ru.just.courses.model.theme.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import ru.just.courses.controller.config.CodeTestListAttributeConverter;

import java.util.List;

@Entity
public class CodeExercise extends Exercise {
    @Convert(converter = CodeTestListAttributeConverter.class)
    @Column(name = "code_tests", nullable = false)
    private List<CodeTest> codeTests;
    private String checkingCode;

    public record CodeTest(String input, String output) { }
}
