package com.RTGpoll.dto;

import lombok.Data;

/**
 * Body for PUT /account/password.
 * Exactly one of currentPassword or code must successfully verify the user.
 * Google-only accounts (no password) must use the code path.
 */
@Data
public class UpdatePasswordRequest {
    private String newPassword;
    private String currentPassword;
    private String code;
}
