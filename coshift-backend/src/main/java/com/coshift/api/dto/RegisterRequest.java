package com.coshift.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstname;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastname;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    /**
     * Acceptation des conditions générales et de la politique de confidentialité.
     *
     * <h2>Pourquoi deux contraintes et non une</h2>
     *
     * <p>{@link AssertTrue} seul ne suffit pas : la spécification Bean
     * Validation considère qu'un élément {@code null} satisfait la contrainte.
     * Un client qui omettrait le champ créerait donc un compte réputé avoir
     * accepté — exactement ce que l'on cherche à empêcher. Le défaut a été
     * constaté en appelant le point d'entrée sans le champ : le compte était
     * créé, en 200.</p>
     *
     * <p>{@link NotNull} ferme cette porte, {@link AssertTrue} rejette le refus
     * explicite. Il faut les deux.</p>
     *
     * <h2>Pourquoi l'acceptation doit être un acte distinct</h2>
     *
     * <p>L'article VI.83, 21° du Code de droit économique répute abusive la
     * clause qui constate de manière irréfragable l'adhésion du consommateur à
     * des conditions dont il n'a pas eu connaissance avant la conclusion du
     * contrat. Une acceptation déduite du seul fait de s'inscrire ne vaudrait
     * donc rien.</p>
     */
    @NotNull(message = "Vous devez accepter les conditions générales et la politique de confidentialité.")
    @AssertTrue(message = "Vous devez accepter les conditions générales et la politique de confidentialité.")
    private Boolean acceptedTerms;
}