package com.opensurveys.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

// Response body returned by both AuthController#register and #login. `token` is the JWT
// (produced by JwtUtil#generateToken) that the client must send back as
// `Authorization: Bearer <token>` on POST /forms. `role` lets the UI gate admin pages
// (backend still enforces ROLE_ADMIN on protected endpoints).
@Data
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private String username;

    public AuthResponse(String token, String role, String username) {
        this.token = token;
        this.role = role;
        this.username = username;
    }
}
