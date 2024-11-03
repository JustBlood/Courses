package ru.just.courses.model.theme;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.just.courses.model.Module;
import ru.just.courses.model.audit.ThemeChangeEvent;
import ru.just.courses.model.theme.content.ThemeContent;
import ru.just.courses.model.theme.exercise.Exercise;

import java.util.List;
import java.util.Set;

@Getter
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "theme",
        uniqueConstraints = {@UniqueConstraint(name = "theme_order_in_module_uq",
                columnNames = {"module_id", "ordinal_number"})})
public class Theme {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "theme_id_seq")
    @SequenceGenerator(name = "theme_id_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 2048)
    private String description;
    @ManyToOne @JoinColumn(name = "module_id")
    private Module module; //todo: index
    @Column(name = "ordinal_number", nullable = false)
    private Integer ordinalNumber;
    @Column(nullable = false) @Enumerated(value = EnumType.STRING)
    private ContentType contentType;
    @OneToMany
    private Set<ThemeContent> themeContents;
    @OneToMany
    private Set<Exercise> exercises;
    @OneToMany(mappedBy = "theme")
    private List<ThemeChangeEvent> themeChangeEvents;
}
