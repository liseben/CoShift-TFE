package com.coshift.api.dto;

import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Représentation d'une réservation renvoyée au client.
 *
 * <p>Sert les deux points de vue : celui du passager qui consulte ses
 * réservations (F30) et celui du conducteur qui consulte les demandes reçues
 * sur ses trajets (F19). Le bloc {@code passenger} n'est donc renseigné que
 * pour le conducteur, et {@code trip} porte le rappel du trajet concerné.</p>
 *
 * <p>Conformément à F13bis, le numéro de téléphone du passager n'est exposé
 * qu'une fois la réservation confirmée : tant que la demande est en attente,
 * rien ne doit permettre de contourner la plateforme.</p>
 */
@Data
@Builder
public class BookingResponse {

    private String uuid;
    private int seatsBooked;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private String statusReason;
    private LocalDateTime createdAt;

    /**
     * Date à laquelle le passager a confirmé que le trajet avait eu lieu (F21),
     * ou {@code null} tant qu'il ne l'a pas fait. Le client s'en sert pour
     * n'afficher le bouton de confirmation que là où il a un sens.
     */
    private LocalDateTime completedAt;

    private TripSummary trip;
    private PassengerSummary passenger;

    @Data
    @Builder
    public static class TripSummary {
        private String uuid;
        private String departureCity;
        private String departureAddress;
        private String arrivalCity;
        private String arrivalAddress;
        private LocalDateTime departureTime;
        private BigDecimal pricePerSeat;
        private int availableSeats;
        private String driverFirstname;
        private String driverLastname;
        private String driverPictureUrl;
        private double driverAverageRating;
        /** Renseigné uniquement si la réservation est confirmée (F13bis). */
        private String driverPhoneNumber;
        private String vehiculeBrand;
        private String vehiculeModel;
    }

    @Data
    @Builder
    public static class PassengerSummary {
        private String uuid;
        private String firstname;
        private String lastname;
        private String pictureUrl;
        private double averageRating;
        private int tripsCount;
        /** Renseigné uniquement si la réservation est confirmée (F13bis). */
        private String phoneNumber;
    }

    /** Vue passager : le trajet réservé et son conducteur (F30). */
    public static BookingResponse forPassenger(Booking booking) {
        var trip = booking.getTrip();
        var driver = trip.getDriver();
        boolean confirmed = booking.getStatus() == BookingStatus.CONFIRMED;

        return baseOf(booking)
                .trip(TripSummary.builder()
                        .uuid(trip.getUuid())
                        .departureCity(trip.getDepartureCity())
                        .departureAddress(trip.getDepartureAddress())
                        .arrivalCity(trip.getArrivalCity())
                        .arrivalAddress(trip.getArrivalAddress())
                        .departureTime(trip.getDepartureTime())
                        .pricePerSeat(trip.getPricePerSeat())
                        .availableSeats(trip.getAvailableSeats())
                        .driverFirstname(driver.getFirstname())
                        .driverLastname(driver.getLastname())
                        .driverPictureUrl(driver.getPictureUrl())
                        .driverAverageRating(driver.getAverageRating())
                        .driverPhoneNumber(confirmed ? driver.getPhoneNumber() : null)
                        .vehiculeBrand(trip.getVehicule().getBrand())
                        .vehiculeModel(trip.getVehicule().getModel())
                        .build())
                .build();
    }

    /** Vue conducteur : qui a demandé une place sur mon trajet (F19). */
    public static BookingResponse forDriver(Booking booking) {
        var passenger = booking.getPassenger();
        boolean confirmed = booking.getStatus() == BookingStatus.CONFIRMED;

        return baseOf(booking)
                .passenger(PassengerSummary.builder()
                        .uuid(passenger.getUuid())
                        .firstname(passenger.getFirstname())
                        .lastname(passenger.getLastname())
                        .pictureUrl(passenger.getPictureUrl())
                        .averageRating(passenger.getAverageRating())
                        .tripsCount(passenger.getTripsCount())
                        .phoneNumber(confirmed ? passenger.getPhoneNumber() : null)
                        .build())
                .build();
    }

    private static BookingResponseBuilder baseOf(Booking booking) {
        return BookingResponse.builder()
                .uuid(booking.getUuid())
                .seatsBooked(booking.getSeatsBooked())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .statusReason(booking.getStatusReason())
                .createdAt(booking.getCreatedAt())
                .completedAt(booking.getCompletedAt());
    }
}
