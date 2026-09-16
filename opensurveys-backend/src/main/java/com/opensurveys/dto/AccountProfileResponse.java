package com.opensurveys.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Profile payload returned by GET /account/me and account update endpoints. */
@Data
@NoArgsConstructor
public class AccountProfileResponse {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String role;
    private boolean hasPassword;
    /** Present when username changed and a fresh JWT was issued. */
    private String token;

    public AccountProfileResponse(
            Long id,
            String name,
            String username,
            String email,
            String role,
            boolean hasPassword
    ) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
        this.hasPassword = hasPassword;
    }
}
