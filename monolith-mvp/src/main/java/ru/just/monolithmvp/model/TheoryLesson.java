package ru.just.monolithmvp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("THEORY")
@Getter
@Setter
@NoArgsConstructor
public class TheoryLesson extends Lesson {

    @Column(name = "theory_content", columnDefinition = "TEXT", nullable = false)
    private String content;
}
