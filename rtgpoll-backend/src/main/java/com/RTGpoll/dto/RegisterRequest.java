package com.RTGpoll.dto;

import lombok.Data;

// Request body for AuthController#register (POST /auth/register).
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String name;
}
