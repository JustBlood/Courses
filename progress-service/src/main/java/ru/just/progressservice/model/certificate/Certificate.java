package ru.just.progressservice.model.certificate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "certificate",
        uniqueConstraints = {@UniqueConstraint(name = "certificate_user_course_uq",
                columnNames = {"user_id", "course_id"})})
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @SequenceGenerator(name = "id_seq", sequenceName = "certificate_id_seq", allocationSize = 1)
    private Long certificateId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long courseId;
    @Column(nullable = false)
    private String certificatePath;
}
