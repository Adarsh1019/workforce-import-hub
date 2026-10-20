package com.workforce.service;

import com.workforce.dto.AuthDtos.LoginRequest;
import com.workforce.dto.AuthDtos.LoginResponse;
import com.workforce.entity.AppUser;
import com.workforce.repository.AppUserRepository;
import com.workforce.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest req) {
        AppUser user = users.findByUsername(req.username())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        String role = user.getRole().name();
        return new LoginResponse(jwt.generate(user.getUsername(), role), user.getUsername(), role);
    }
}
