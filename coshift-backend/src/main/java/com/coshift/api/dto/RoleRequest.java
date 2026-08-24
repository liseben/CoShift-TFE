package com.coshift.api.dto;

import com.coshift.api.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Le rôle à attribuer à un membre. */
@Data
@Schema(name = "DemandeRole")
public class RoleRequest {

    @NotNull(message = "{validation.admin.role}")
    @Schema(description = "USER, ADMIN ou SUPER_ADMIN.", example = "ADMIN")
    private Role role;
}
