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
        if (properties.username() == null || properties.username().isBlank()) {
            return;
        }

        userRepository.findByUsername(properties.username()).orElseGet(() -> {
            AppUser admin = new AppUser();
            admin.setUsername(properties.username());
            admin.setPasswordHash(passwordEncoder.encode(properties.password()));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            return userRepository.save(admin);
        });
    }
}
