package com.opensurveys.dto;

import lombok.Data;

/** Body for PUT /account/profile — name is the joined first+last from the client. */
@Data
public class UpdateProfileRequest {
    private String name;
    private String username;
}
