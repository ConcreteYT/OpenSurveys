package com.opensurveys.dto;

import lombok.Data;

/**
 * Admin-only body for PUT /users/{id}.
 * Updates profile fields and role (USER / ADMIN). Password is never changed here.
 */
@Data
public class UpdateUserRequest {
    private String name;
    private String username;
    private String email;
    private String role;
}
