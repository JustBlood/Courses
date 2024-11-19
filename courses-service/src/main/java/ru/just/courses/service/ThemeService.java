package ru.just.courses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.courses.controller.exception.MethodNotAllowedException;
import ru.just.courses.dto.CreateThemeDto;
import ru.just.courses.dto.ThemeDto;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.repository.ThemeRepository;
import ru.just.courses.repository.exception.EntityNotFoundException;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ThemeService {
    private final ThemeRepository themeRepository;
    private final ModuleService moduleService;
    private final ThreadLocalTokenService tokenService;

    public Optional<ThemeDto> findThemeById(Long themeId) {
        return themeRepository.findById(themeId).map(new ThemeDto()::fromEntity);
    }

    public ThemeDto createTheme(CreateThemeDto dto) {
        moduleService.checkCourseAuthor(dto.getModuleId());

        final Theme theme = themeRepository.save(dto.toEntity());
        return new ThemeDto().fromEntity(theme);
    }

    public void deleteById(Long themeId) {
        checkCourseAuthor(themeId);
        themeRepository.deleteById(themeId);
    }

    public void checkCourseAuthor(Long themeId) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new EntityNotFoundException("Theme with specified id not found"));
        if (!theme.getModule().getCourse().getAuthorId().equals(tokenService.getUserId())) {
            throw new MethodNotAllowedException("You are not a course author");
        }
    }

    public Optional<List<ThemeDto>> getThemesByCourseId(Long courseId) {
        return themeRepository.findAllByModule_Course_Id(courseId)
                .map(l -> l.stream().map(t -> new ThemeDto().fromEntity(t)).collect(Collectors.toList()));
    }
}
