package ru.just.mentorcatalogservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.just.mentorcatalogservice.dto.CreateMentorDto;
import ru.just.mentorcatalogservice.dto.MentorCardDto;
import ru.just.mentorcatalogservice.dto.MentorDto;
import ru.just.mentorcatalogservice.model.Mentor;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface MentorMapper {
    @Mapping(target = "studentsCount", expression = "java(mentor.getStudents.size())")
    MentorDto toDto(Mentor mentor);
    Mentor toEntity(CreateMentorDto mentor);
    @Mapping(target = "studentsCount", expression = "java(mentor.getStudents.size())")

    MentorCardDto toCardDto(Mentor mentor);
}
