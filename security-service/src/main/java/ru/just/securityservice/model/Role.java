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
@Table(name = "roles")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_id_seq")
    @SequenceGenerator(name = "roles_id_seq", sequenceName = "roles_id_seq", allocationSize = 1)
    private Long roleId;
    @Column(nullable = false, unique = true)
    private String name;
    @ManyToMany
    private Set<User> userPrincipal;
}
