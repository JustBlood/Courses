package ru.just.courses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.courses.controller.exception.MethodNotAllowedException;
import ru.just.courses.dto.CreateModuleDto;
import ru.just.courses.dto.ModuleDto;
import ru.just.courses.model.Module;
import ru.just.courses.repository.ModuleRepository;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ModuleService {
    private final ModuleRepository moduleRepository;
    private final ThreadLocalTokenService tokenService;

    public Optional<ModuleDto> getModuleById(Long moduleId) {
        return moduleRepository.findById(moduleId).map(new ModuleDto()::fromEntity);
    }

    public ModuleDto createModule(CreateModuleDto createModuleDto) {
        return new ModuleDto().fromEntity(moduleRepository.save(createModuleDto.toEntity()));
    }

    public void deleteModuleById(Long moduleId) {
        final Optional<Module> moduleOptional = moduleRepository.findById(moduleId);
        if (moduleOptional.isEmpty()) {
            throw new NoSuchElementException("module with specified id doesn't exists");
        }
        if (!moduleOptional.get().getCourse().getAuthorId().equals(tokenService.getUserId())) {
            throw new MethodNotAllowedException("You are not a course author");
        }
        moduleRepository.deleteById(moduleId);
    }
}
