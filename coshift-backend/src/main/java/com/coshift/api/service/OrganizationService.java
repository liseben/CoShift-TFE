package com.coshift.api.service;

import com.coshift.api.entity.Organization;
import com.coshift.api.entity.User;
import com.coshift.api.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
