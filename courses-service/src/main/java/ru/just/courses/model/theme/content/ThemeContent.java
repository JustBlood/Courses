package ru.just.courses.model.theme.content;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ThemeContent {
    @Id @JoinColumn(name = "theme_id")
    private Long themeId;
    private Integer ordinalNumber;
}
