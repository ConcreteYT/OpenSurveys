package com.opensurveys.dto;

import lombok.Data;

// Request body for AuthController#googleLogin (POST /auth/google).
// `idToken` is the credential JWT from Google Identity Services on the frontend.
@Data
public class GoogleLoginRequest {
    private String idToken;
}
