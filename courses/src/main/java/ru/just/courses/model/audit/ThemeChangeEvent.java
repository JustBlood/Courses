package ru.just.courses.model.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.user.User;
import ru.just.dtolib.audit.ChangeType;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_theme_change_type_change_time", columnList = "theme_id ASC, changeType ASC, changeTime DESC")})
@Entity
public class ThemeChangeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @SequenceGenerator(name = "id_seq", sequenceName = "theme_change_event_id_seq", allocationSize = 1)
    private Long id;
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    @Column(nullable = false)
    private LocalDateTime changeTime = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "theme_id")
    private Theme theme;
}
