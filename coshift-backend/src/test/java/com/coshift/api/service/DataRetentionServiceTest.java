package com.coshift.api.service;

import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import com.coshift.api.entity.User;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Limitation de la durée de conservation — RGPD, article 5.1.e.
 *
 * <p>Une politique de confidentialité peut annoncer n'importe quelle durée. Ce
 * qui la rend vraie, c'est un mécanisme qui l'applique sans intervention — et
 * ce mécanisme tourne à trois heures et demie du matin, sans personne pour
 * regarder. S'il se trompe, il se trompe en silence, dans les deux sens
 * possibles : conserver ce qui devait disparaître, ou détruire ce qui devait
 * rester.</p>
 *
 * <p>Les tests ci-dessous couvrent les deux sens.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataRetentionService — durées de conservation")
class DataRetentionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TripRepository tripRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private SecurityAuditService audit;

    @InjectMocks private DataRetentionService service;

    @BeforeEach
    void preparer() {
        // Les seuils viennent normalement de application.properties via @Value ;
        // sans contexte Spring, on les pose à la main.
        ReflectionTestUtils.setField(service, "joursAvantPurgeInscription", 30);
        ReflectionTestUtils.setField(service, "moisAvantAnonymisationTrajet", 24);

        when(userRepository.findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(any()))
                .thenReturn(List.of());
        when(userRepository.findWithExpiredCodes(any())).thenReturn(List.of());
        when(tripRepository.findAnonymisables(any())).thenReturn(List.of());
        when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(anyLong())).thenReturn(List.of());
        when(bookingRepository.findByPassengerIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        when(bookingRepository.findByTripIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
    }

    // ══════════════ 1. Inscriptions jamais confirmées ═══════════════════════════

    @Nested
    @DisplayName("Inscriptions abandonnées")
    class Inscriptions {

        @Test
        @DisplayName("supprime une inscription jamais confirmée et sans historique")
        void supprimeUneInscriptionAbandonnee() {
            // Une adresse dont personne n'a prouvé qu'elle lui appartenait n'est
            // pas un compte : la garder revient à conserver la donnée d'une
            // personne qui n'a peut-être rien demandé.
            User abandonnee = utilisateur(1L, "jamais.confirme@coshift.be");
            when(userRepository.findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(any()))
                    .thenReturn(List.of(abandonnee));

            service.appliquerLesDurees();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<User>> capture = ArgumentCaptor.forClass(List.class);
            verify(userRepository).deleteAll(capture.capture());
            assertThat(capture.getValue()).containsExactly(abandonnee);
        }

        @Test
        @DisplayName("consigne la purge au journal de sécurité")
        void consigneLaPurge() {
            User abandonnee = utilisateur(1L, "jamais.confirme@coshift.be");
            when(userRepository.findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(any()))
                    .thenReturn(List.of(abandonnee));

            service.appliquerLesDurees();

            verify(audit).consigner(eq(SecurityAuditService.Evenement.COMPTE_PURGE),
                    eq(abandonnee.getUuid()), anyString(), anyString());
        }

        @Test
        @DisplayName("épargne une inscription qui a laissé des trajets")
        void epargneUnCompteAvecHistorique() {
            // Supprimer la ligne casserait les clés étrangères, et détruirait au
            // passage l'historique d'un tiers qui n'a rien demandé.
            User avecTrajet = utilisateur(1L, "a.des.trajets@coshift.be");
            when(userRepository.findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(any()))
                    .thenReturn(List.of(avecTrajet));
            when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(1L))
                    .thenReturn(List.of(trajet(LocalDateTime.now().minusYears(1))));

            service.appliquerLesDurees();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<User>> capture = ArgumentCaptor.forClass(List.class);
            verify(userRepository).deleteAll(capture.capture());
            assertThat(capture.getValue()).isEmpty();
        }

        @Test
        @DisplayName("épargne une inscription qui a laissé des réservations")
        void epargneUnCompteAvecReservations() {
            User avecResa = utilisateur(1L, "a.reserve@coshift.be");
            when(userRepository.findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(any()))
                    .thenReturn(List.of(avecResa));
            when(bookingRepository.findByPassengerIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(reservation(trajet(LocalDateTime.now().minusYears(1)))));

            service.appliquerLesDurees();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<User>> capture = ArgumentCaptor.forClass(List.class);
            verify(userRepository).deleteAll(capture.capture());
            assertThat(capture.getValue()).isEmpty();
        }

        @Test
        @DisplayName("détache le compte de son organisation avant de le supprimer")
        void detacheAvantDeSupprimer() {
            // La table de liaison porte une clé étrangère vers l'utilisateur :
            // le rattachement doit tomber avant la ligne.
            User abandonnee = utilisateur(1L, "jamais.confirme@coshift.be");
            abandonnee.getOrganizations().add(
                    com.coshift.api.entity.Organization.builder().id(9L).name("Solvantis").build());
            when(userRepository.findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(any()))
                    .thenReturn(List.of(abandonnee));

            service.appliquerLesDurees();

            assertThat(abandonnee.getOrganizations()).isEmpty();
        }
    }

    // ══════════════ 2. Codes expirés ════════════════════════════════════════════

    @Nested
    @DisplayName("Codes expirés")
    class Codes {

        @Test
        @DisplayName("efface un code de vérification périmé")
        void effaceUnCodeDeVerificationPerime() {
            User u = utilisateur(1L, "membre@coshift.be");
            u.setVerificationCode("123456");
            u.setVerificationCodeExpiry(LocalDateTime.now().minusHours(2));
            when(userRepository.findWithExpiredCodes(any())).thenReturn(List.of(u));

            service.appliquerLesDurees();

            assertThat(u.getVerificationCode()).isNull();
            assertThat(u.getVerificationCodeExpiry()).isNull();
        }

        @Test
        @DisplayName("efface un code de réinitialisation périmé")
        void effaceUnCodeDeReinitialisationPerime() {
            // Un code de réinitialisation qui traîne est un mot de passe
            // secondaire oublié.
            User u = utilisateur(1L, "membre@coshift.be");
            u.setPasswordResetCode("654321");
            u.setPasswordResetExpiry(LocalDateTime.now().minusHours(2));
            when(userRepository.findWithExpiredCodes(any())).thenReturn(List.of(u));

            service.appliquerLesDurees();

            assertThat(u.getPasswordResetCode()).isNull();
            assertThat(u.getPasswordResetExpiry()).isNull();
        }

        @Test
        @DisplayName("laisse intact un code encore valable")
        void laisseIntactUnCodeValable() {
            // La requête peut ramener un compte pour l'un de ses deux codes ;
            // l'autre, encore valable, ne doit pas être emporté avec.
            User u = utilisateur(1L, "membre@coshift.be");
            u.setVerificationCode("111111");
            u.setVerificationCodeExpiry(LocalDateTime.now().plusHours(1));
            u.setPasswordResetCode("222222");
            u.setPasswordResetExpiry(LocalDateTime.now().minusHours(1));
            when(userRepository.findWithExpiredCodes(any())).thenReturn(List.of(u));

            service.appliquerLesDurees();

            assertThat(u.getVerificationCode()).isEqualTo("111111");
            assertThat(u.getPasswordResetCode()).isNull();
        }
    }

    // ══════════════ 3. Trajets et réservations anciens ══════════════════════════

    @Nested
    @DisplayName("Trajets de plus de vingt-quatre mois")
    class TrajetsAnciens {

        @Test
        @DisplayName("retire les adresses précises et la description")
        void retireLesAdressesPrecises() {
            // Une adresse exacte désigne un domicile ou un lieu de travail ; une
            // description libre peut contenir n'importe quoi — un prénom, un
            // numéro, un point de rendez-vous.
            Trip ancien = trajet(LocalDateTime.now().minusYears(3));
            when(tripRepository.findAnonymisables(any())).thenReturn(new ArrayList<>(List.of(ancien)));

            service.appliquerLesDurees();

            assertThat(ancien.getDepartureAddress()).isNull();
            assertThat(ancien.getArrivalAddress()).isNull();
            assertThat(ancien.getDescription()).isNull();
        }

        @Test
        @DisplayName("conserve les villes et la date pour les statistiques")
        void conserveLesVillesEtLaDate() {
            // C'est exactement ce qu'alimentent les données ouvertes : une ville
            // de départ, une ville d'arrivée, un mois. Rien de tout cela ne
            // désigne quelqu'un.
            Trip ancien = trajet(LocalDateTime.now().minusYears(3));
            when(tripRepository.findAnonymisables(any())).thenReturn(new ArrayList<>(List.of(ancien)));

            service.appliquerLesDurees();

            assertThat(ancien.getDepartureCity()).isEqualTo("Namur");
            assertThat(ancien.getArrivalCity()).isEqualTo("Bruxelles");
            assertThat(ancien.getDepartureTime()).isNotNull();
            assertThat(ancien.getStatus()).isEqualTo(TripStatus.COMPLETED);
        }

        @Test
        @DisplayName("efface les motifs de refus des réservations rattachées")
        void effaceLesMotifs() {
            // Le motif d'un refus est rédigé par un humain à propos d'un autre :
            // c'est la donnée la plus sensible de la table.
            Trip ancien = trajet(LocalDateTime.now().minusYears(3));
            Booking resa = reservation(ancien);
            resa.setStatusReason("Ne s'est pas présenté au rendez-vous");
            when(tripRepository.findAnonymisables(any())).thenReturn(new ArrayList<>(List.of(ancien)));
            when(bookingRepository.findByTripIdOrderByCreatedAtDesc(10L))
                    .thenReturn(new ArrayList<>(List.of(resa)));

            service.appliquerLesDurees();

            assertThat(resa.getStatusReason()).isNull();
            // Le statut reste : il alimente les comptages agrégés.
            assertThat(resa.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Passage à vide")
    class PassageAVide {

        @Test
        @DisplayName("ne touche à rien quand aucune donnée n'a atteint son terme")
        void neToucheARien() {
            service.appliquerLesDurees();

            verify(tripRepository).saveAll(List.of());
            verify(userRepository, never()).save(any());
            verify(audit, never()).consigner(any(), anyString(), anyString(), anyString());
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private Trip trajet(LocalDateTime depart) {
        return Trip.builder()
                .id(10L).uuid("trajet-uuid")
                .departureCity("Namur").departureAddress("Place d'Armes 1")
                .arrivalCity("Bruxelles").arrivalAddress("Rue de la Loi 16")
                .departureTime(depart)
                .availableSeats(3).pricePerSeat(new BigDecimal("5.00"))
                .description("Départ ponctuel, merci d'être à l'heure")
                .status(TripStatus.COMPLETED)
                .build();
    }

    private Booking reservation(Trip trajet) {
        return Booking.builder()
                .id(100L).uuid("resa-uuid")
                .trip(trajet)
                .seatsBooked(1).totalPrice(new BigDecimal("5.00"))
                .status(BookingStatus.COMPLETED)
                .build();
    }

    private User utilisateur(Long id, String courriel) {
        return User.builder()
                .id(id).uuid("uuid-" + id)
                .email(courriel)
                .firstname("Prenom" + id).lastname("Nom" + id)
                .password("peu-importe")
                .emailVerified(false)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build();
    }
}
