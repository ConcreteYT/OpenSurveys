package com.opensurveys.dto;

import lombok.Data;

/** Body for DELETE /account — confirmUsername must match the signed-in username exactly. */
@Data
public class DeleteAccountRequest {
    private String confirmUsername;
}
