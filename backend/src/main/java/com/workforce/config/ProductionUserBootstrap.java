package com.workforce.config;

import com.workforce.entity.AppUser;
import com.workforce.entity.Role;
import com.workforce.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates initial production accounts from temporary Render secrets when explicitly enabled. */
@Component
@Profile("production")
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class ProductionUserBootstrap implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String hrUsername;
    private final String hrPassword;

    public ProductionUserBootstrap(
            AppUserRepository users,
            PasswordEncoder encoder,
            @Value("${app.bootstrap.admin-username:}") String adminUsername,
            @Value("${app.bootstrap.admin-password:}") String adminPassword,
            @Value("${app.bootstrap.hr-username:}") String hrUsername,
            @Value("${app.bootstrap.hr-password:}") String hrPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.hrUsername = hrUsername;
        this.hrPassword = hrPassword;
    }

    @Override
    public void run(String... args) {
        createIfAbsent(adminUsername, adminPassword, Role.ADMIN, "admin");
        if (hrUsername.isBlank() != hrPassword.isBlank()) {
            throw new IllegalStateException("Both bootstrap HR username and password must be provided together.");
        }
        if (!hrUsername.isBlank()) createIfAbsent(hrUsername, hrPassword, Role.HR, "HR");
    }

    private void createIfAbsent(String username, String password, Role role, String accountType) {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Bootstrap " + accountType + " username and password are required.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("Bootstrap " + accountType + " password must be at least 12 characters.");
        }
        if (!users.existsByUsername(username)) {
            users.save(new AppUser(username, encoder.encode(password), role));
        }
    }
}