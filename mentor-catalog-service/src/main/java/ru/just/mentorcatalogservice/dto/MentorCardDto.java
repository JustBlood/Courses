package ru.just.mentorcatalogservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class MentorCardDto {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String shortAboutMe;
    private String longAboutMe;
    private List<String> specializations;
    private Integer studentsCount;
    private String avatarUrl;
}
