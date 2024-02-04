package ru.just.courses.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ru.just.courses.model.course.Course;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_course_evaluation")
public class UserCourseEvaluation {
    @Id @ManyToOne @JoinColumn(name = "user_id")
    private User user;
    @Id @ManyToOne @JoinColumn(name = "course_id")
    private Course course;
    @Column(columnDefinition = "INTEGER CHECK(evaluation >= 0 AND evaluation <= 5)")
    private Integer evaluation;
}
