package com.coshift.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    private String firstname;
    private String lastname;
    // Si tu ajoutes un numéro de téléphone ou une bio plus tard dans ton entité User, 
    // c'est ici qu'il faudra rajouter les champs !
}