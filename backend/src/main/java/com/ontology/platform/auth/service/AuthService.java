package com.ontology.platform.auth.service;

import com.ontology.platform.auth.entity.User;
import com.ontology.platform.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // In production, inject UserMapper and query database
    // For now, a simplified structure

    public String login(String username, String password) {
        // TODO: Replace with actual DB query
        // User user = userMapper.selectOne(
        //     new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        // Placeholder: generate token for any valid user
        if ("admin".equals(username) && passwordEncoder.matches(password,
                passwordEncoder.encode("admin123"))) {
            return jwtTokenProvider.generateToken("1", username, "ADMIN");
        }

        throw new IllegalArgumentException("Invalid credentials");
    }

    public String getTokenFromUser(User user) {
        return jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), user.getRole());
    }
}
