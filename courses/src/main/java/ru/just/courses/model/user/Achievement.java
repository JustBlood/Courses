package ru.just.courses.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "achievement")
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "achievement_id_seq", allocationSize = 1)
    private Long id;
    private String message;
    @ManyToMany(mappedBy = "achievements")
    private Set<User> users;
}
