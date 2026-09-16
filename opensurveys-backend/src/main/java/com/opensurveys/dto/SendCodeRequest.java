package com.opensurveys.dto;

import lombok.Data;

/** Body for POST /account/send-code. Purpose: EMAIL_CHANGE or PASSWORD_CHANGE. */
@Data
public class SendCodeRequest {
    private String purpose;
}
