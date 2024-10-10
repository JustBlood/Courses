package ru.just.mentorcatalogservice.dto;

import lombok.Data;

import java.util.Set;

@Data
public class MentorDto {
    private Long userId;
    private Set<String> specializations;
    private String shortAboutMe;
    private String longAboutMe;
    private Long studentsCount;
    private String avatarUrl;
}
