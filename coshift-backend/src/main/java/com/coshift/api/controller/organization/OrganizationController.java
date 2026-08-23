package com.coshift.api.controller.organization;

import com.coshift.api.dto.OrganizationDashboardResponse;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.service.Messages;
import com.coshift.api.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organisations", description = "Le cercle auquel appartient un membre, et ce qu'il y produit.")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepository userRepository;
    private final Messages messages;

    @Operation(
            summary = "Consulter les chiffres de mes organisations",
            description = """
                    Volume de trajets, places partagées, taux de remplissage et participation,
                    pour chaque organisation dont le membre connecté fait partie.

                    **Il n'y a pas de seuil d'anonymat ici**, contrairement au jeu de données
                    ouvert. La différence tient au lecteur : celui-ci est membre du cercle
                    qu'il consulte, et les trajets comptés sont ceux qu'il voit déjà un par un
                    dans la recherche. Masquer un agrégat dont le détail est à portée de clic
                    serait une précaution de façade.

                    Les définitions sont en revanche identiques à celles du jeu ouvert : deux
                    chiffres portant le même nom dans deux écrans doivent se calculer pareil.

                    Le bloc `nonMesure` énonce ce que le produit ne sait pas calculer — la
                    distance des trajets, donc les émissions évitées. Un trajet porte des
                    villes, pas une distance ; afficher une estimation serait le chiffre le
                    plus facile à produire de tout cet écran, et le seul que personne ne
                    songerait à vérifier.

                    Réponse vide si le membre n'appartient à aucune organisation active :
                    ce n'est pas une erreur, c'est le cas d'une adresse dont le domaine n'est
                    revendiqué par personne.""")
    @ApiResponse(responseCode = "200", description = "Un tableau de bord par organisation, par ordre alphabétique.")
    @GetMapping("/mine")
    public ResponseEntity<List<OrganizationDashboardResponse>> mesOrganisations(Authentication auth) {
        var membre = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
        return ResponseEntity.ok(organizationService.tableauDeBord(membre));
    }
}
