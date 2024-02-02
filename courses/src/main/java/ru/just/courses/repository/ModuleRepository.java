package ru.just.courses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.courses.model.Module;

public interface ModuleRepository extends JpaRepository<Module, Long> {
}
