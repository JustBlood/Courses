package ru.just.courses.model.user;

import jakarta.persistence.*;
import lombok.*;
import ru.just.courses.model.course.Course;

import java.util.Objects;
import java.util.Set;

@With
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
    @SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 1)
    private Long userId;
    @OneToMany(mappedBy = "author")
    private Set<Course> authoredCourses;
    @ManyToMany(mappedBy = "users")
    private Set<Course> courses;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    public void addCourse(Course course) {
        course.getUsers().add(this);
        courses.add(course);
    }
}
