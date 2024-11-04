package ru.just.courses.model.course;

import jakarta.persistence.*;
import lombok.*;
import ru.just.courses.model.Module;
import ru.just.courses.model.audit.CourseChangeEvent;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@With
@Entity
@Table(name = "course")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @SequenceGenerator(name = "id_seq", sequenceName = "course_id_seq", allocationSize = 1)
    private Long id;
    private String title;
    @Column(length = 2048)
    private String description;
    @Column(nullable = false)
    private Long authorId; // todo: index
    @Column(nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();
    private Integer completionTimeInHours; // todo: index
    private Boolean completionPercentForCertificate;
    @OneToMany(mappedBy = "course", orphanRemoval = true,  cascade = CascadeType.ALL)
    private List<Module> modules;
    @OneToMany(mappedBy = "course")
    private List<CourseChangeEvent> courseChangeEvents;
}
