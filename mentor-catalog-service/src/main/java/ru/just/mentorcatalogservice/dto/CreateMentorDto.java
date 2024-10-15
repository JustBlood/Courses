package ru.just.mentorcatalogservice.dto;

import lombok.Data;

import java.util.Set;

@Data
public class CreateMentorDto {
    private Long userId;
    private String shortAboutMe;
    private String longAboutMe;
    private Set<String> specializations;
}
