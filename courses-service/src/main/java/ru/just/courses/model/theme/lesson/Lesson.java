package ru.just.courses.model.theme.lesson;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.Theme;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(
        name = "lesson",
        uniqueConstraints = {
                @UniqueConstraint(name = "theme_id_ordinal_number_uq", columnNames = {"theme_id", "ordinal_number"})
        }
)
public abstract class Lesson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lessonId;
    @ManyToOne @JoinColumn(name = "theme_id")
    private Theme theme;
    private Integer ordinalNumber;
    @Enumerated(value = EnumType.STRING)
    private LessonType type;
}
