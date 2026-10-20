package com.workforce.config;

import com.workforce.entity.AppUser;
import com.workforce.entity.Role;
import com.workforce.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the demo accounts admin/12345 and hr/12345 on first start. Disable with app.seed-users=false. */
@Component
@ConditionalOnProperty(name = "app.seed-users", havingValue = "true")
public class UserSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserSeeder(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        create("admin", "12345", Role.ADMIN);
        create("hr", "12345", Role.HR);
    }

    private void create(String username, String password, Role role) {
        if (!users.existsByUsername(username)) users.save(new AppUser(username, encoder.encode(password), role));
    }
}
