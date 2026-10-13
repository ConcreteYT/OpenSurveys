package com.opensurveys.dto;

import lombok.Data;

@Data
public class VerifyEmailRequest {
    private String username;
    private String code;
}
