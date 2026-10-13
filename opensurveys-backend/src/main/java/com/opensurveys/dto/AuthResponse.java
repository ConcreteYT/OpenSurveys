package com.opensurveys.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;
    private String role;
    private String username;
    private boolean verificationRequired;

    public AuthResponse(String role, String username) {
        this.role = role;
        this.username = username;
    }

    public static AuthResponse verificationRequired(String username) {
        AuthResponse response = new AuthResponse();
        response.setUsername(username);
        response.setVerificationRequired(true);
        return response;
    }
}
