package ru.just.courses.model.user;

import jakarta.persistence.*;

@Entity
@Table(name = "avatar_images")
public class AvatarImage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "avatar_image_id_seq", allocationSize = 1)
    private Long id;
    @Column
    private String contentType;
    @Column
    private String filename;
    @OneToOne
    private User user;
}
