package com.coshift.api.service;

import com.coshift.api.dto.VehiculeRequest;
import com.coshift.api.entity.EnergyType;
import com.coshift.api.entity.User;
import com.coshift.api.entity.Vehicule;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Véhicules déclarés par les membres.
 *
 * <p>Service simple, mais qui porte un contrôle de propriété : c'est le genre
 * de règle écrite à la main dans chaque méthode, où une seule omission ouvre
 * la ressource d'autrui. Les tests la verrouillent sur les trois opérations
 * qui la mettent en jeu.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehiculeService — véhicules")
class VehiculeServiceTest {

    @Mock private VehiculeRepository vehiculeRepository;
    @Mock private UserRepository userRepository;
    @Mock private Messages messages;

    @InjectMocks private VehiculeService service;

    private User proprietaire;
    private User tiers;
    private Vehicule voiture;

    private static final String PROPRIETAIRE = "proprietaire@coshift.be";
    private static final String TIERS = "tiers@coshift.be";

    @BeforeEach
    void preparer() {
        proprietaire = utilisateur(1L, PROPRIETAIRE);
        tiers = utilisateur(2L, TIERS);

        voiture = Vehicule.builder()
                .id(20L).uuid("vehicule-uuid")
                .brand("Renault").model("Clio")
                .licensePlate("1-ABC-123")
                .seats(4).energy(EnergyType.GASOLINE)
                .photoUrl("http://localhost:8080/photo.jpg")
                .owner(proprietaire)
                .build();

        when(userRepository.findByEmail(PROPRIETAIRE)).thenReturn(Optional.of(proprietaire));
        when(userRepository.findByEmail(TIERS)).thenReturn(Optional.of(tiers));
        when(vehiculeRepository.findByUuid("vehicule-uuid")).thenReturn(Optional.of(voiture));
        when(vehiculeRepository.save(any(Vehicule.class))).thenAnswer(i -> i.getArgument(0));
        when(messages.get(anyString())).thenReturn("message");
    }

    @Nested
    @DisplayName("Lister")
    class Lister {

        @Test
        @DisplayName("ne rend que les véhicules du membre connecté")
        void neRendQueLesSiens() {
            when(vehiculeRepository.findByOwnerId(1L)).thenReturn(List.of(voiture));

            var mes = service.getMyVehicules(PROPRIETAIRE);

            assertThat(mes).hasSize(1);
            assertThat(mes.get(0).getLicensePlate()).isEqualTo("1-ABC-123");
            verify(vehiculeRepository).findByOwnerId(1L);
        }

        @Test
        @DisplayName("rend une liste vide plutôt qu'une erreur")
        void listeVideSansErreur() {
            when(vehiculeRepository.findByOwnerId(anyLong())).thenReturn(List.of());

            assertThat(service.getMyVehicules(PROPRIETAIRE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Ajouter")
    class Ajouter {

        @Test
        @DisplayName("rattache le véhicule au membre connecté")
        void rattacheAuMembre() {
            service.addVehicule(PROPRIETAIRE, demande("2-XYZ-789"));

            ArgumentCaptor<Vehicule> capture = ArgumentCaptor.forClass(Vehicule.class);
            verify(vehiculeRepository).save(capture.capture());
            assertThat(capture.getValue().getOwner()).isEqualTo(proprietaire);
        }

        @ParameterizedTest(name = "« {0} » est enregistrée en majuscules et sans espaces")
        @ValueSource(strings = {"  1-abc-123  ", "1-Abc-123", "1-ABC-123 "})
        @DisplayName("normalise la plaque d'immatriculation")
        void normaliseLaPlaque(String saisie) {
            // La plaque sert de clé d'unicité en base : « 1-abc-123 » et
            // « 1-ABC-123 » désignent le même véhicule et ne doivent pas pouvoir
            // coexister.
            service.addVehicule(PROPRIETAIRE, demande(saisie));

            ArgumentCaptor<Vehicule> capture = ArgumentCaptor.forClass(Vehicule.class);
            verify(vehiculeRepository).save(capture.capture());
            assertThat(capture.getValue().getLicensePlate()).isEqualTo("1-ABC-123");
        }

        @Test
        @DisplayName("refuse un membre inconnu")
        void refuseUnMembreInconnu() {
            when(userRepository.findByEmail("fantome@coshift.be")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addVehicule("fantome@coshift.be", demande("3-DEF-456")))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(vehiculeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Modifier")
    class Modifier {

        @Test
        @DisplayName("applique les nouvelles valeurs")
        void appliqueLesNouvellesValeurs() {
            VehiculeRequest r = demande("9-ZZZ-999");
            r.setBrand("Peugeot");
            r.setModel("208");
            r.setSeats(5);
            r.setEnergy(EnergyType.ELECTRIC);

            var reponse = service.updateVehicule(PROPRIETAIRE, "vehicule-uuid", r);

            assertThat(reponse.getBrand()).isEqualTo("Peugeot");
            assertThat(reponse.getSeats()).isEqualTo(5);
            assertThat(reponse.getEnergy()).isEqualTo(EnergyType.ELECTRIC);
            assertThat(reponse.getLicensePlate()).isEqualTo("9-ZZZ-999");
        }

        @Test
        @DisplayName("conserve la photo existante si aucune n'est fournie")
        void conserveLaPhotoExistante() {
            // Une modification qui n'aborde pas la photo ne doit pas l'effacer :
            // c'est le défaut classique d'un PUT appliqué sans discernement.
            VehiculeRequest r = demande("1-ABC-123");
            r.setPhotoUrl(null);

            service.updateVehicule(PROPRIETAIRE, "vehicule-uuid", r);

            assertThat(voiture.getPhotoUrl()).isEqualTo("http://localhost:8080/photo.jpg");
        }

        @Test
        @DisplayName("refuse de modifier le véhicule d'autrui")
        void refuseLeVehiculeDautrui() {
            assertThatThrownBy(() ->
                    service.updateVehicule(TIERS, "vehicule-uuid", demande("1-ABC-123")))
                    .isInstanceOf(UnauthorizedException.class);

            verify(vehiculeRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse un véhicule inconnu")
        void refuseUnVehiculeInconnu() {
            when(vehiculeRepository.findByUuid("inexistant")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updateVehicule(PROPRIETAIRE, "inexistant", demande("1-ABC-123")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Supprimer")
    class Supprimer {

        @Test
        @DisplayName("supprime le véhicule du membre connecté")
        void supprimeLeSien() {
            service.deleteVehicule(PROPRIETAIRE, "vehicule-uuid");

            verify(vehiculeRepository).delete(voiture);
        }

        @Test
        @DisplayName("refuse de supprimer le véhicule d'autrui")
        void refuseCeluiDautrui() {
            assertThatThrownBy(() -> service.deleteVehicule(TIERS, "vehicule-uuid"))
                    .isInstanceOf(UnauthorizedException.class);

            verify(vehiculeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("refuse un véhicule inconnu")
        void refuseUnVehiculeInconnu() {
            when(vehiculeRepository.findByUuid("inexistant")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteVehicule(PROPRIETAIRE, "inexistant"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(vehiculeRepository, never()).delete(any());
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private VehiculeRequest demande(String plaque) {
        VehiculeRequest r = new VehiculeRequest();
        r.setBrand("Renault");
        r.setModel("Clio");
        r.setLicensePlate(plaque);
        r.setSeats(4);
        r.setEnergy(EnergyType.GASOLINE);
        r.setPhotoUrl("http://localhost:8080/photo.jpg");
        return r;
    }

    private User utilisateur(Long id, String courriel) {
        return User.builder()
                .id(id).uuid("uuid-" + id)
                .email(courriel)
                .firstname("Prenom" + id).lastname("Nom" + id)
                .password("peu-importe")
                .emailVerified(true)
                .build();
    }
}
