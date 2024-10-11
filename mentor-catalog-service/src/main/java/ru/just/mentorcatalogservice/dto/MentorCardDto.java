package ru.just.mentorcatalogservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class MentorCardDto {
    private String username;
    private String firstName;
    private String lastName;
    private String shortAboutMe;
    private String longAboutMe;
    private List<String> specializations;
    private Long studentsCount;
    private String avatarUrl;
}