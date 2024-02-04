package ru.just.courses.model.course;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
public class CourseWithRating {
    @Id @OneToOne @JoinColumn(name = "course_id")
    private Course course;
    private Double averageRating;
}
