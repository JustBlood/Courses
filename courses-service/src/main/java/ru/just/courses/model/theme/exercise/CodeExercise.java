package ru.just.courses.model.theme.exercise;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.controller.config.CodeTestListAttributeConverter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"theme_id", "ordinal_number"})})
public class CodeExercise extends Exercise {
    @Convert(converter = CodeTestListAttributeConverter.class)
    @Column(name = "code_tests", nullable = false)
    private List<CodeTest> codeTests;

    public record CodeTest(String input, String output) { }

    public CodeExercise(Integer ordinalNumber, List<CodeTest> codeTests) {
        super(null, null, ordinalNumber);
        this.codeTests = codeTests;
    }
}
