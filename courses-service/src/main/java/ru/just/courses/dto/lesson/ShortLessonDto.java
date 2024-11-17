package ru.just.courses.dto.lesson;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.just.courses.model.theme.lesson.Lesson;
import ru.just.dtolib.base.Dto;

@EqualsAndHashCode(callSuper = true)
@Data
public class ShortLessonDto extends Dto<Lesson> {
    private Long lessonId;
    private Integer ordinalNumber;

    @Override
    public ShortLessonDto fromEntity(Lesson entity) {
        this.lessonId = entity.getLessonId();
        this.ordinalNumber = entity.getOrdinalNumber();
        return this;
    }

    @Override
    public Lesson toEntity() {
        throw new UnsupportedOperationException();
    }
}
