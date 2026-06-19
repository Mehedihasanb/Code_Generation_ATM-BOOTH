package com.example.backend.dtos;

public record LoginResponse(
        TokenResponse token,
        CurrentUserResponse user
) {}
