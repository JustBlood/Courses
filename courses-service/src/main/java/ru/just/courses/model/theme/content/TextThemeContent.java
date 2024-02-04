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
    public TextThemeContent(Clob text, Long themeId) {
        super(themeId);
        this.text = text;
    }

    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private Clob text;
}
