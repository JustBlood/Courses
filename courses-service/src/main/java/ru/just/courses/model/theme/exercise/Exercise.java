package ru.just.courses.model.theme.exercise;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.theme.Theme;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "", allocationSize = 1)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "theme_id")
    private Theme theme;
    @Column(nullable = false)
    private Integer ordinalNumber;
}
