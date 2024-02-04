package ru.just.courses.model.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ru.just.courses.model.course.Course;
import ru.just.courses.model.user.User;
import ru.just.dtolib.audit.ChangeType;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_course_change_type_change_time", columnList = "course_id ASC, changeType ASC, changeTime DESC")})
@Entity
public class CourseChangeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @SequenceGenerator(name = "id_seq", sequenceName = "course_change_event_id_seq", allocationSize = 1)
    private Long id;
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    @Column(nullable = false)
    private ZonedDateTime changeTime = ZonedDateTime.now();
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
}
