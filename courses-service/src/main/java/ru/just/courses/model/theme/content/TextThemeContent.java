package ru.just.courses.model.theme.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Clob;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "theme_text_content")
public class TextThemeContent extends ThemeContent {
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private Clob text;

    public TextThemeContent(Clob text, Long themeId, Integer ordinalNumber) {
        super(themeId, ordinalNumber);
        this.text = text;
    }
}
