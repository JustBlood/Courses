package ru.just.securityservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Set;

@Accessors(chain = true)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_id_seq")
    @SequenceGenerator(name = "role_id_seq", sequenceName = "role_id_seq", allocationSize = 1)
    @Column(name = "role_id")
    private Long roleId;
    @Column(nullable = false, unique = true)
    private String name;
    @ManyToMany
    @JoinTable(name = "user_role",
            joinColumns =
                @JoinColumn(name = "role_id", referencedColumnName = "role_id"),
            inverseJoinColumns =
                @JoinColumn(name = "user_id", referencedColumnName = "user_id"))
    private Set<User> users;
}
