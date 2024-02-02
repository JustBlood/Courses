package ru.just.courses.model.course;

import jakarta.persistence.*;
import lombok.*;
import ru.just.courses.model.Module;
import ru.just.courses.model.audit.CourseChangeEvent;
import ru.just.courses.model.user.User;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

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
    @ManyToOne
    private User author; // todo: index
    @Column(nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();
    private Integer completionTimeInHours; // todo: index
    @Transient
    private Double rating; // todo: index
    @OneToMany(mappedBy = "course", orphanRemoval = true,  cascade = CascadeType.ALL)
    private List<Module> modules;
    @ManyToMany
    private Set<User> users;
    @OneToMany(mappedBy = "course")
    private List<CourseChangeEvent> courseChangeEvents;

    public void addLesson(Module module) {
        module.setCourse(this);
        this.modules.add(module);
    }
}
