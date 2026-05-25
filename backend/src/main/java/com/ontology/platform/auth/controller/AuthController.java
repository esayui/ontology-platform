package com.ontology.platform.auth.controller;

import com.ontology.platform.common.Result;
import com.ontology.platform.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        String token = authService.login(username, password);
        return Result.ok(Map.of(
                "token", token,
                "username", username,
                "tokenType", "Bearer"
        ));
    }
}
