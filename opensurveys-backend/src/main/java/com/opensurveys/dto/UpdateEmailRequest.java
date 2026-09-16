package com.opensurveys.dto;

import lombok.Data;

/**
 * Body for PUT /account/email.
 * Exactly one of currentPassword or code must successfully verify the user.
 */
@Data
public class UpdateEmailRequest {
    private String email;
    private String currentPassword;
    private String code;
}
