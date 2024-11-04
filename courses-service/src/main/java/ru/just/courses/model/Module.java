package ru.just.courses.model;

import jakarta.persistence.*;
import lombok.*;
import ru.just.courses.model.audit.ModuleChangeEvent;
import ru.just.courses.model.course.Course;
import ru.just.courses.model.theme.Theme;

import java.util.List;

@With
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "module", uniqueConstraints = {@UniqueConstraint(columnNames = {"course_id", "ordinal_number"})})
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @SequenceGenerator(name = "id_seq", sequenceName = "module_id_seq", allocationSize = 1)
    private Long id;
    @Column
    private String title;
    @Column(nullable = false, length = 2048)
    private String description;
    @ManyToOne(optional = false)
    private Course course; //todo: index
    @Column(name = "ordinal_number", nullable = false)
    private Integer ordinalNumber;
    @OneToMany(mappedBy = "module")
    private List<Theme> themes;
    @OneToMany(mappedBy = "module")
    private List<ModuleChangeEvent> moduleChangeEvents;
}
