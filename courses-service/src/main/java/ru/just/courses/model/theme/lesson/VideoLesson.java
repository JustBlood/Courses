package ru.just.courses.model.theme.lesson;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"theme_id", "ordinal_number"})})
public class VideoLesson extends Lesson {
    private String videoUrl;

    public VideoLesson(Integer ordinalNumber) {
        super(null, null, ordinalNumber, LessonType.VIDEO);
    }
}
