package com.RTGpoll.dto;

import lombok.Data;

// Request body for AuthController#login (POST /auth/login).
@Data
public class LoginRequest {
    private String username;
    private String password;
}
