package ru.just.monolithmvp.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.just.monolithmvp.config.properties.BootstrapAdminProperties;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.Role;
import ru.just.monolithmvp.repository.AppUserRepository;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    @Override
    public void run(String... args) {
        if (properties.email() == null || properties.email().isBlank()) {
            return;
        }

        userRepository.findByEmail(properties.email()).orElseGet(() -> {
            AppUser admin = new AppUser();
            admin.setFullName(properties.fullName() == null || properties.fullName().isBlank() ? "System Admin" : properties.fullName());
            admin.setEmail(properties.email().isBlank() ? "admin@local" : properties.email());
            admin.setPasswordHash(passwordEncoder.encode(properties.password()));
            admin.setRole(Role.ADMIN);
            admin.setActivation(true);
            admin.setEnabled(true);
            return userRepository.save(admin);
        });
    }
}
