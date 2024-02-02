package ru.just.courses.model.theme;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.just.courses.model.Module;
import ru.just.courses.model.audit.ThemeChangeEvent;

import java.util.List;

@Getter
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "theme",
        uniqueConstraints = {@UniqueConstraint(name = "theme_order_in_module_uq",
                columnNames = {"module_id", "theme_order"})})
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
    @Column(name = "theme_order", nullable = false)
    private Integer themeOrder;
    @Column(nullable = false) @Enumerated(value = EnumType.STRING)
    private ContentType contentType;
    @OneToMany(mappedBy = "theme")
    private List<ThemeChangeEvent> themeChangeEvents;
}
