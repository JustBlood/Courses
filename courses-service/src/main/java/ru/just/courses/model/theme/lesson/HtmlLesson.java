package ru.just.courses.model.theme.lesson;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Clob;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"theme_id", "ordinal_number"})})
public class HtmlLesson extends Lesson {
    private Clob html;

    public HtmlLesson(Integer ordinalNumber) {
        super(null, null, ordinalNumber, LessonType.HTML);
        this.html = null;
    }
}
