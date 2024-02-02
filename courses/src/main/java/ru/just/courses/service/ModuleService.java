package ru.just.courses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.courses.dto.CreateModuleDto;
import ru.just.courses.dto.ModuleDto;
import ru.just.courses.repository.ModuleRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ModuleService {
    private final ModuleRepository moduleRepository;

    public Optional<ModuleDto> getModuleById(Long moduleId) {
        return moduleRepository.findById(moduleId).map(new ModuleDto()::fromEntity);
    }

    public ModuleDto createModule(CreateModuleDto createModuleDto) {
        return new ModuleDto().fromEntity(moduleRepository.save(createModuleDto.toEntity()));
    }

    public void deleteModuleById(Long moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new NoSuchElementException("module with specified id doesn't exists");
        }
        moduleRepository.deleteById(moduleId);
    }
}
