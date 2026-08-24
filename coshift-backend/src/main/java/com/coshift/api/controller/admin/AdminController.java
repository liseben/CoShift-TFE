package com.coshift.api.controller.admin;

import com.coshift.api.dto.AdminMemberResponse;
import com.coshift.api.dto.AdminOverviewResponse;
import com.coshift.api.dto.SuspensionRequest;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.LoginAttemptService;
import com.coshift.api.service.AdminService;
import com.coshift.api.service.Messages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Console de supervision et de modération.
 *
 * <p>Tout ce contrôleur est fermé par un seul {@link PreAuthorize} de classe.
 * Le poser une fois vaut mieux que le répéter méthode par méthode : une
 * annotation oubliée sur une méthode ajoutée plus tard laisserait un point
 * d'entrée d'administration ouvert à tous, et rien ne le signalerait.</p>
 *
 * <p>La distinction entre les deux rôles est faite dans le service, pas ici :
 * elle porte sur le <em>périmètre</em> des données, pas sur l'accès au point
 * d'entrée. Un contrôleur ne sait pas quelles organisations sont les siennes.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Administration",
        description = "Supervision et modération. Réservé aux rôles ADMIN et SUPER_ADMIN.")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final Messages messages;

    @Operation(
            summary = "Vue de supervision",
            description = """
                    Membres, trajets et réservations du périmètre de l'administrateur connecté.

                    **Le périmètre dépend du rôle.** Un `SUPER_ADMIN` répond de la plateforme
                    et voit tout. Un `ADMIN` répond de ses organisations et ne voit qu'elles :
                    sans cette borne, donner un rôle d'administrateur à une entreprise cliente
                    lui ouvrirait les membres et les trajets de toutes les autres. Le champ
                    `portee` indique laquelle des deux s'applique.

                    Les comptes effacés au titre de l'article 17 sont exclus de tous les
                    comptages sauf du leur, qui n'apparaît qu'à la portée plateforme :
                    l'anonymisation vide le rattachement, si bien qu'un compte effacé
                    n'appartient plus à aucun cercle.""")
    @ApiResponse(responseCode = "200", description = "Chiffres du périmètre.")
    @GetMapping("/apercu")
    public ResponseEntity<AdminOverviewResponse> apercu(Authentication auth) {
        return ResponseEntity.ok(adminService.apercu(administrateur(auth)));
    }

    @Operation(
            summary = "Lister les membres",
            description = """
                    Membres du périmètre, du plus récemment inscrit au plus ancien — l'ordre
                    utile, puisque ce sont les arrivées récentes qui demandent une attention.

                    La recherche porte sur le prénom, le nom et l'adresse. Les comptes effacés
                    ne remontent jamais : ils n'ont plus ni nom ni adresse, et les modérer
                    n'aurait ni objet ni fondement.

                    Ni téléphone, ni adresse postale, ni photographie ne sont renvoyés. Un
                    administrateur modère des comportements, pas des personnes ; une console
                    qui affiche tout devient un annuaire complet le jour où elle est
                    compromise.""")
    @ApiResponse(responseCode = "200", description = "Page de membres.")
    @GetMapping("/membres")
    public ResponseEntity<Page<AdminMemberResponse>> membres(
            @Parameter(description = "Recherche libre sur prénom, nom ou adresse.", example = "moreau")
            @RequestParam(required = false) String q,
            @Parameter(description = "Numéro de page, à partir de 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page, bornée à 100.", example = "20")
            @RequestParam(defaultValue = "20") int taille,
            Authentication auth) {
        return ResponseEntity.ok(adminService.membres(administrateur(auth), q, page, taille));
    }

    @Operation(
            summary = "Suspendre un compte",
            description = """
                    Empêche la connexion sans rien effacer ni anonymiser : les trajets passés
                    continuent d'exister, parce qu'ils engagent aussi les autres participants.
                    La mesure se lève à tout moment.

                    **Réservé au `SUPER_ADMIN`.** Consulter n'engage rien ; suspendre engage la
                    plateforme vis-à-vis de la personne. La décision revient donc à celui qui
                    répond de la plateforme, non à celui qui répond d'une organisation — sans
                    quoi une entreprise cliente pourrait fermer le compte d'un employé à
                    travers un outil qui n'est pas le sien.

                    Le motif est obligatoire : une décision sans raison écrite ne peut plus
                    être expliquée trois mois plus tard, ni à la personne, ni à un juge.

                    Trois refus : se suspendre soi-même, suspendre un autre `SUPER_ADMIN`
                    — deux comptes de supervision qui se neutralisent laissent l'application
                    sans pilote —, et suspendre un compte déjà suspendu, ce qui écraserait la
                    date de la première mesure.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte suspendu."),
            @ApiResponse(responseCode = "400", description = "Motif absent, compte déjà suspendu, ou suspension de soi-même.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Réservé au SUPER_ADMIN, ou cible administratrice de plateforme.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Aucun compte pour cet identifiant.", content = @Content())
    })
    @PatchMapping("/membres/{uuid}/suspension")
    public ResponseEntity<AdminMemberResponse> suspendre(
            @Parameter(description = "Identifiant public du compte.") @PathVariable String uuid,
            @Valid @RequestBody SuspensionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                adminService.suspendre(administrateur(auth), uuid, request.getMotif()));
    }

    @Operation(
            summary = "Lever une suspension",
            description = """
                    Rend l'accès au compte. Le motif est effacé en même temps que la date :
                    le conserver laisserait sur un compte redevenu actif la trace d'une
                    accusation à laquelle rien ne correspond plus. L'événement, lui, reste au
                    journal de sécurité.

                    Réservé au `SUPER_ADMIN`, comme la suspension.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte réactivé."),
            @ApiResponse(responseCode = "400", description = "Le compte n'était pas suspendu.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Réservé au SUPER_ADMIN.", content = @Content())
    })
    @DeleteMapping("/membres/{uuid}/suspension")
    public ResponseEntity<AdminMemberResponse> reactiver(
            @Parameter(description = "Identifiant public du compte.") @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(adminService.reactiver(administrateur(auth), uuid));
    }

    @Operation(
            summary = "Changer le rôle d'un membre",
            description = """
                    Attribue `USER`, `ADMIN` ou `SUPER_ADMIN`.

                    **Pourquoi ce point d'entrée existe.** Le premier `SUPER_ADMIN` a été posé
                    par une migration — il faut bien que quelqu'un ouvre la porte de
                    l'intérieur. S'en tenir là obligerait à écrire une migration, donc à
                    redéployer, chaque fois qu'un client change d'interlocuteur.

                    **Réservé au `SUPER_ADMIN`** : distribuer des rôles est le pouvoir qui
                    contient tous les autres, puisqu'il permet de se les donner. Un
                    administrateur d'organisation qui pourrait nommer des administrateurs se
                    nommerait lui-même administrateur de plateforme.

                    Trois autres refus : on ne change pas son propre rôle — se rétrograder par
                    mégarde fermerait la porte derrière soi ; on ne rétrograde pas le dernier
                    administrateur de plateforme, pour la même raison vue de l'autre côté ; et
                    un compte dont l'adresse n'est pas confirmée n'obtient aucun rôle
                    d'administration, car donner un rôle à une adresse non prouvée revient à
                    le donner à qui la contrôle.

                    L'opération est consignée au journal de sécurité avec l'ancien rôle, le
                    nouveau et qui a décidé.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôle modifié."),
            @ApiResponse(responseCode = "400", description = "Rôle sur soi-même, dernier administrateur, ou compte non vérifié.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Réservé au SUPER_ADMIN.", content = @Content())
    })
    @PatchMapping("/membres/{uuid}/role")
    public ResponseEntity<AdminMemberResponse> changerRole(
            @Parameter(description = "Identifiant public du compte.") @PathVariable String uuid,
            @Valid @RequestBody com.coshift.api.dto.RoleRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                adminService.changerRole(administrateur(auth), uuid, request.getRole()));
    }

    @Operation(
            summary = "Freinages de connexion en cours",
            description = """
                    Couples adresse IP × compte actuellement bloqués par le freinage des
                    tentatives, du plus récent au plus ancien.

                    **C'est ce que la console montre à la place du journal de sécurité.** Le
                    journal est un fichier, écrit hors de portée de l'application et
                    volontairement : lui ouvrir un point d'entrée HTTP reviendrait à offrir,
                    derrière une seule authentification, la liste des adresses et des comptes
                    attaqués — exactement ce qu'un attaquant vient chercher. Les freinages en
                    cours répondent à la même question, « quelque chose est-il en train de se
                    passer ? », sans ouvrir cette porte, et ils sont actionnables tout de
                    suite.

                    La liste vit en mémoire et disparaît au redémarrage, ce qui est cohérent
                    avec ce qu'elle décrit : un blocage dure quinze minutes.

                    Réservé au `SUPER_ADMIN` : elle porte des adresses IP et des adresses
                    électroniques, y compris de comptes étrangers à toute organisation de
                    l'appelant.""")
    @ApiResponse(responseCode = "200", description = "Freinages en vigueur, éventuellement aucun.")
    @GetMapping("/blocages")
    public ResponseEntity<List<LoginAttemptService.Blocage>> blocages(Authentication auth) {
        return ResponseEntity.ok(adminService.blocages(administrateur(auth)));
    }

    private com.coshift.api.entity.User administrateur(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
    }
}
