package com.coshift.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "{validation.email.requis}")
    @Email(message = "{validation.email.format}")
    private String email;

    @NotBlank(message = "{validation.code.requis}")
    @Pattern(regexp = "^[0-9]{6}$", message = "{validation.code.format}")
    private String code;
}
