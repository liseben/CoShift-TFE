package com.coshift.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    /**
     * Identifiant public du compte. Sans lui, le front ne peut comparer
     * l'utilisateur connecté au conducteur d'un trajet que sur le prénom et
     * le nom — deux homonymes seraient confondus.
     */
    private String uuid;

    private String firstname;
    private String lastname;
    private String email;
    private String pictureUrl;
    private String phoneNumber;
    private String role;
    private boolean emailVerified;
    private double averageRating;
    private int tripsCount;
}