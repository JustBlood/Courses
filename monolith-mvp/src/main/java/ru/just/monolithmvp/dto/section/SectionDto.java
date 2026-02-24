package ru.just.monolithmvp.dto.section;

public record SectionDto(
        Long id,
        String title,
        String description,
        Integer priority
) {
}
