package com.coshift.api.dto;

import com.coshift.api.entity.Role;
import com.coshift.api.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Un membre, tel que la console de modération le montre.
 *
 * <h2>Ce qui n'y figure pas, et pourquoi</h2>
 *
 * <p>Ni téléphone, ni adresse postale, ni photographie. Un administrateur
 * modère des comportements, pas des personnes : il a besoin de savoir qui est
 * inscrit, si l'adresse est confirmée, si le compte est suspendu et depuis
 * quand. Le reste relève du droit d'accès de la personne elle-même, pas du
 * travail de modération — et une console qui affiche tout devient, le jour où
 * elle est compromise, un annuaire complet.</p>
 *
 * <p>L'adresse électronique y est, parce qu'elle est l'identifiant du compte :
 * on ne peut ni le chercher ni en parler sans elle.</p>
 */
@Builder
@Schema(name = "MembreAdmin", description = "Vue d'un membre pour la modération.")
public record AdminMemberResponse(

        @Schema(example = "0b2-0001-0000-4000-8000-000000000001") String uuid,
        @Schema(example = "Fanny") String firstname,
        @Schema(example = "Moreau") String lastname,
        @Schema(example = "fanny.moreau@solvantis.be") String email,
        @Schema(example = "USER") Role role,

        @Schema(description = "L'adresse a été confirmée par un code.", example = "true")
        boolean emailVerified,

        @Schema(description = "Organisations dont le membre fait partie.")
        List<String> organisations,

        @Schema(description = "Trajets publiés et honorés, tels que le profil les affiche.", example = "3")
        int tripsCount,

        @Schema(description = "Moyenne des avis reçus, 0 si aucun.", example = "4.3")
        double averageRating,

        @Schema(example = "2025-01-04T12:30:00") LocalDateTime createdAt,

        @Schema(description = "Instant de suspension, ou null si le compte est actif.")
        LocalDateTime suspendedAt,

        @Schema(description = "Motif de la suspension, tel qu'écrit par l'administrateur.")
        String suspensionReason
) {

    public static AdminMemberResponse from(User u) {
        return AdminMemberResponse.builder()
                .uuid(u.getUuid())
                .firstname(u.getFirstname())
                .lastname(u.getLastname())
                .email(u.getEmail())
                .role(u.getRole())
                .emailVerified(u.isEmailVerified())
                .organisations(u.getOrganizations() == null ? List.of() :
                        u.getOrganizations().stream()
                                .map(o -> o.getName())
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList())
                .tripsCount(u.getTripsCount())
                .averageRating(u.getAverageRating())
                .createdAt(u.getCreatedAt())
                .suspendedAt(u.getSuspendedAt())
                .suspensionReason(u.getSuspensionReason())
                .build();
    }
}
