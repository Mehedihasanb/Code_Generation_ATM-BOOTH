package com.example.backend.controllers;

import com.example.backend.dtos.CurrentUserResponse;
import com.example.backend.dtos.LoginRequest;
import com.example.backend.dtos.LoginResponse;
import com.example.backend.dtos.RegisterRequest;
import com.example.backend.dtos.TokenResponse;
import com.example.backend.dtos.UserResponse;
import com.example.backend.entities.User;
import com.example.backend.mappers.CustomerMapper;
import com.example.backend.mappers.UserMapper;
import com.example.backend.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
public class AuthRestController {

    private final AuthService auth;
    private final UserMapper toUser;
    private final CustomerMapper toProfile;

    public AuthRestController(AuthService auth, UserMapper toUser, CustomerMapper toProfile) {
        this.auth = auth;
        this.toUser = toUser;
        this.toProfile = toProfile;
    }

    @PostMapping("/register")
    public UserResponse signUp(@Valid @RequestBody RegisterRequest body) {
        User created = auth.register(body);
        return toUser.toResponse(created);
    }

    @PostMapping("/login")
    public LoginResponse signIn(@Valid @RequestBody LoginRequest body) {
        User user = auth.login(body.email(), body.password());
        return loginResponseFor(user);
    }

    @GetMapping("/me")
    public CurrentUserResponse profile(@AuthenticationPrincipal User user) {
        return toProfile.toCurrentUser(user);
    }

    private LoginResponse loginResponseFor(User user) {
        String accessToken = auth.generateToken(user);
        long expirationSeconds = auth.getTokenExpirationMs() / 1000;
        TokenResponse token = new TokenResponse(accessToken, expirationSeconds, "Bearer");
        CurrentUserResponse userInfo = toProfile.toCurrentUser(user);
        return new LoginResponse(token, userInfo);
    }
}
