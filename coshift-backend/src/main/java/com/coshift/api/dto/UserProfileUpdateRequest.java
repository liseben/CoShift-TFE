package com.coshift.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "{validation.prenom.requis}")
    private String firstname;

    @NotBlank(message = "{validation.nom.requis}")
    private String lastname;

    @NotBlank(message = "{validation.email.requis}")
    @Email(message = "{validation.email.format}")
    private String email;

    private String phoneNumber;
}
