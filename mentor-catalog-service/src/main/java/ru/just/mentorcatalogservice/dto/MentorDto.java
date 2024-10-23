package ru.just.mentorcatalogservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class MentorDto {
    private Long userId;
    private List<String> specializations;
    private String shortAboutMe;
    private String longAboutMe;
    private Integer studentsCount;
}
