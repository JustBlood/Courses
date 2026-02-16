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
        if (getContentType() == TheoryContentType.VIDEO_URL) {
            setLessonType(LessonType.THEORY_VIDEO);
        } else if (getContentType() == TheoryContentType.PDF_FILE) {
            setLessonType(LessonType.THEORY_PDF);
        } else {
            setLessonType(LessonType.THEORY_TEXT);
        }
    }
}
