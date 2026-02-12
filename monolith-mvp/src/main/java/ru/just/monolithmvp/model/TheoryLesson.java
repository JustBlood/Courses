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

    @Enumerated(EnumType.STRING)
    @Column(name = "theory_content_type", nullable = false)
    private TheoryContentType contentType;

    @Column(name = "theory_content", length = 20000, nullable = false)
    private String content;

    @PrePersist
    @PreUpdate
    private void syncLessonType() {
        setLessonType(LessonType.THEORY);
    }
}
