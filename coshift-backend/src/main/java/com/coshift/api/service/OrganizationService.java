package com.coshift.api.service;

import com.coshift.api.dto.OrganizationDashboardResponse;
import com.coshift.api.entity.Organization;
import com.coshift.api.entity.User;
import com.coshift.api.repository.OrganizationRepository;
import com.coshift.api.repository.OrganizationStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Rattachement des personnes et des trajets à une organisation.
 *
 * <h2>Ce que cette classe décide</h2>
 *
 * <p>CoShift ne met pas en relation des inconnus : il s'adresse à des gens qui
 * partagent déjà un employeur, un campus ou un événement. Le cercle fermé n'est
 * pas une restriction ajoutée au produit, c'est sa condition de fonctionnement.
 * Encore faut-il que le logiciel sache à quel cercle chacun appartient : c'est
 * ce que cette classe établit, à partir du domaine de l'adresse
 * professionnelle.</p>
 *
 * <h2>Pourquoi le domaine, et pas une invitation</h2>
 *
 * <p>Une invitation supposerait un administrateur par organisation, un écran
 * pour l'émettre et un autre pour l'accepter — et surtout, elle ferait dépendre
 * l'arrivée d'un nouveau membre du geste de quelqu'un d'autre. Le domaine est
 * déjà vérifié par ailleurs : l'inscription n'aboutit qu'après confirmation de
 * l'adresse. Recevoir un code à {@code @solvantis.be}, c'est prouver qu'on lit
 * le courrier de Solvantis.</p>
 *
 * <h2>Ce que cette classe ne décide pas</h2>
 *
 * <p>Elle ne crée jamais d'organisation. Un domaine inconnu ne donne pas lieu à
 * un rattachement : la personne existe, elle n'appartient simplement à aucun
 * cercle. Créer une organisation à la volée reviendrait à laisser n'importe
 * quelle adresse fonder un espace, et à faire du premier venu le voisin de tous
 * les suivants.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationStatsRepository statsRepository;
    private final Messages messages;

    /**
     * Domaine d'une adresse de courriel, en minuscules.
     *
     * <p>Renvoie {@code null} plutôt que de lever pour une adresse absente ou
     * malformée : cette méthode répond à « d'où vient cette personne », et
     * « d'aucune organisation connue » est une réponse valable. La validation
     * du format de l'adresse relève du DTO d'inscription, en amont.</p>
     */
    public String domaine(String email) {
        if (email == null) return null;
        int arobase = email.lastIndexOf('@');
        if (arobase < 0 || arobase == email.length() - 1) return null;
        return email.substring(arobase + 1).trim().toLowerCase();
    }

    /** Organisation active revendiquant le domaine de cette adresse, s'il en existe une. */
    public Optional<Organization> organisationDuDomaine(String email) {
        String domaine = domaine(email);
        if (domaine == null || domaine.isEmpty()) return Optional.empty();
        return organizationRepository.findByEmailDomainIgnoreCaseAndActiveTrue(domaine);
    }

    /**
     * Rattache la personne à l'organisation de son domaine, si elle existe.
     *
     * <p>N'écrit pas en base : l'appelant enregistre l'utilisateur, et le
     * rattachement part avec lui dans la même transaction. Séparer les deux
     * écritures ouvrirait une fenêtre où un compte existe sans son cercle.</p>
     *
     * @return l'organisation rejointe, ou {@link Optional#empty()} si le
     *         domaine n'est revendiqué par aucune organisation active
     */
    public Optional<Organization> rattacher(User user) {
        Optional<Organization> organisation = organisationDuDomaine(user.getEmail());
        organisation.ifPresent(o -> {
            user.getOrganizations().add(o);
            log.info("Compte {} rattaché à l'organisation {}", user.getEmail(), o.getSlug());
        });
        return organisation;
    }

    /**
     * Organisation « d'origine » de la personne : celle de son domaine, à
     * condition qu'elle en soit effectivement membre.
     *
     * <p>La double condition n'est pas une précaution de style. Le domaine dit
     * d'où vient quelqu'un ; l'appartenance dit ce qui lui est ouvert. Elles se
     * séparent dès qu'un membre est retiré d'une organisation sans changer
     * d'adresse — et c'est alors l'appartenance qui fait foi.</p>
     *
     * <p>À défaut, et si la personne n'appartient qu'à une seule organisation,
     * c'est celle-là. Au-delà, aucune ne s'impose : c'est au conducteur de
     * choisir, et à l'interface de le lui demander.</p>
     */
    public Optional<Organization> organisationParDefaut(User user) {
        Set<Organization> siennes = user.getOrganizations();
        if (siennes == null || siennes.isEmpty()) return Optional.empty();

        String domaine = domaine(user.getEmail());
        if (domaine != null) {
            Optional<Organization> origine = siennes.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .filter(o -> domaine.equalsIgnoreCase(o.getEmailDomain()))
                    .findFirst();
            if (origine.isPresent()) return origine;
        }

        List<Organization> actives = siennes.stream()
                .filter(o -> Boolean.TRUE.equals(o.getActive()))
                .toList();
        return actives.size() == 1 ? Optional.of(actives.get(0)) : Optional.empty();
    }

    /**
     * Identifiants des organisations actives de la personne.
     *
     * <p>Sert de filtre de visibilité. La liste peut être vide : quelqu'un qui
     * n'appartient à aucune organisation ne voit que les trajets qui n'en ont
     * pas non plus. C'est une conséquence assumée du modèle, pas un oubli.</p>
     */
    public List<Long> identifiantsDesOrganisations(User user) {
        if (user.getOrganizations() == null) return List.of();
        return user.getOrganizations().stream()
                .filter(o -> Boolean.TRUE.equals(o.getActive()))
                .map(Organization::getId)
                .toList();
    }

    /** Vrai si la personne appartient à l'organisation portée par un trajet. */
    public boolean partageLeCercle(User user, Organization organisationDuTrajet) {
        /* Un trajet sans organisation est visible de tous : son conducteur
           n'appartenait à aucun cercle au moment de la publication, il n'y a
           donc personne à qui le réserver. */
        if (organisationDuTrajet == null) return true;
        return identifiantsDesOrganisations(user).contains(organisationDuTrajet.getId());
    }
    // ───────────────────────── Tableau de bord d'organisation ───────────────────

    /**
     * Chiffres de mobilité des organisations dont la personne est membre.
     *
     * <p>Réservé aux membres, et c'est ce qui autorise l'absence de seuil
     * d'anonymat : les trajets comptés ici sont ceux que le lecteur voit déjà un
     * par un dans la recherche. Masquer un agrégat dont le détail est à portée
     * de clic serait une précaution de façade.</p>
     *
     * <p><strong>L'organisation d'origine vient en premier</strong>, les autres
     * ensuite par ordre alphabétique. Cet ordre n'est pas cosmétique : le
     * formulaire de publication s'en sert pour présélectionner le cercle du
     * trajet. Une liste purement alphabétique y mettait en tête une
     * organisation que le serveur n'aurait pas retenue, si bien que l'écran
     * annonçait autre chose que ce qu'il faisait.</p>
     *
     * <p>Une organisation désactivée n'y figure pas : son contrat a pris fin,
     * ses chiffres ne sont plus les siens.</p>
     */
    public List<OrganizationDashboardResponse> tableauDeBord(User membre) {
        if (membre.getOrganizations() == null) return List.of();

        Long origine = organisationParDefaut(membre).map(Organization::getId).orElse(null);

        return membre.getOrganizations().stream()
                .filter(o -> Boolean.TRUE.equals(o.getActive()))
                .sorted(Comparator
                        .comparing((Organization o) -> !o.getId().equals(origine))
                        .thenComparing(Organization::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::chiffresDe)
                .toList();
    }

    private OrganizationDashboardResponse chiffresDe(Organization o) {
        long partagees = statsRepository.compterPlacesPartagees(o.getId());
        long restantes = statsRepository.compterPlacesRestantes(o.getId());

        return OrganizationDashboardResponse.builder()
                .uuid(o.getUuid())
                .name(o.getName())
                .slug(o.getSlug())
                .logoUrl(o.getLogoUrl())
                .volumes(OrganizationDashboardResponse.Volumes.builder()
                        .trajetsPublies(statsRepository.compterTrajets(o.getId()))
                        .trajetsAnnules(statsRepository.compterTrajetsAnnules(o.getId()))
                        .trajetsRealises(statsRepository.compterTrajetsRealises(o.getId()))
                        .placesPartagees(partagees)
                        .placesRestantes(restantes)
                        .tauxRemplissage(tauxRemplissage(partagees, restantes))
                        .build())
                .participation(OrganizationDashboardResponse.Participation.builder()
                        .membres(statsRepository.compterMembres(o.getId()))
                        .conducteurs(statsRepository.compterConducteurs(o.getId()))
                        .passagers(statsRepository.compterPassagers(o.getId()))
                        .build())
                .parMois(statsRepository.volumeParMois(o.getId()).stream()
                        .map(ligne -> OrganizationDashboardResponse.Mois.builder()
                                .mois(String.valueOf(ligne[0]))
                                .trajets(((Number) ligne[1]).longValue())
                                .placesPartagees(((Number) ligne[2]).longValue())
                                .build())
                        .toList())
                .nonMesure(OrganizationDashboardResponse.NonMesure.builder()
                        .distanceParcourue(true)
                        .emissionsEvitees(true)
                        .motif(messages.get("organisation.nonMesure"))
                        .build())
                .build();
    }

    /**
     * Part des places proposées qui ont trouvé preneur.
     *
     * <p>Le dénominateur est le total des places offertes — occupées plus
     * libres — et non le nombre de trajets : une voiture de cinq places à moitié
     * vide et une de deux places pleine ne se valent pas.</p>
     *
     * <p>Aucune place offerte donne zéro, pas une division par zéro ni un tiret.
     * Une organisation sans trajet a bien un taux de remplissage nul.</p>
     */
    private BigDecimal tauxRemplissage(long partagees, long restantes) {
        long offertes = partagees + restantes;
        if (offertes == 0) return BigDecimal.ZERO.setScale(1);
        return BigDecimal.valueOf(partagees * 100.0 / offertes).setScale(1, RoundingMode.HALF_UP);
    }
}
