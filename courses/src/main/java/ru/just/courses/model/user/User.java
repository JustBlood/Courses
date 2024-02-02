package ru.just.courses.model.user;

import jakarta.persistence.*;
import lombok.*;
import ru.just.courses.model.audit.UserChangeEvent;
import ru.just.courses.model.course.Course;

import java.time.LocalDate;
import java.util.List;
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
    private Long id;
    @Column(nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    private String firstName;
    private String lastName;
    private String mail;
    private String phone;
    @OneToOne(mappedBy = "user")
    @JoinColumn(name = "avatar_id")
    private AvatarImage avatar;
    private LocalDate registrationDate;
    @Column(nullable = false)
    private Boolean isAdmin;
    @OneToMany(mappedBy = "user")
    private List<UserChangeEvent> userChangeEvents;
    @OneToMany(mappedBy = "author")
    private Set<Course> authoredCourses;
    @ManyToMany(mappedBy = "users")
    private Set<Course> courses;
    @OneToMany(mappedBy = "user")
    private List<SocialMediaLink> socialMediaLinks;
    @ManyToMany
    private Set<Achievement> achievements;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void addCourse(Course course) {
        course.getUsers().add(this);
        courses.add(course);
    }
}
