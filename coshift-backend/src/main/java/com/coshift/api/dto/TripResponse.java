package com.coshift.api.dto;

import com.coshift.api.entity.EnergyType;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TripResponse {

    private String uuid;
    private String departureCity;
    private String departureAddress;
    private String arrivalCity;
    private String arrivalAddress;
    private LocalDateTime departureTime;
    private int availableSeats;
    private BigDecimal pricePerSeat;
    private String description;
    private TripStatus status;
    private LocalDateTime createdAt;

    // Préférences
    private boolean acceptsLuggage;
    private boolean acceptsPets;
    private boolean musicAllowed;
    private boolean talkingAllowed;

    // Infos conducteur (aperçu)
    private DriverSummary driver;

    // Infos véhicule (aperçu)
    private VehicleSummary vehicule;

    @Data @Builder
    public static class DriverSummary {
        private String uuid;
        private String firstname;
        private String lastname;
        private String pictureUrl;
        private double averageRating;
        private int tripsCount;
    }

    @Data @Builder
    public static class VehicleSummary {
        private String brand;
        private String model;
        private int seats;
        private EnergyType energy;
        private String photoUrl;
    }

    public static TripResponse from(Trip t) {
        return TripResponse.builder()
                .uuid(t.getUuid())
                .departureCity(t.getDepartureCity())
                .departureAddress(t.getDepartureAddress())
                .arrivalCity(t.getArrivalCity())
                .arrivalAddress(t.getArrivalAddress())
                .departureTime(t.getDepartureTime())
                .availableSeats(t.getAvailableSeats())
                .pricePerSeat(t.getPricePerSeat())
                .description(t.getDescription())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .acceptsLuggage(t.isAcceptsLuggage())
                .acceptsPets(t.isAcceptsPets())
                .musicAllowed(t.isMusicAllowed())
                .talkingAllowed(t.isTalkingAllowed())
                .driver(DriverSummary.builder()
                        .uuid(t.getDriver().getUuid())
                        .firstname(t.getDriver().getFirstname())
                        .lastname(t.getDriver().getLastname())
                        .pictureUrl(t.getDriver().getPictureUrl())
                        .averageRating(t.getDriver().getAverageRating())
                        .tripsCount(t.getDriver().getTripsCount())
                        .build())
                .vehicule(VehicleSummary.builder()
                        .brand(t.getVehicule().getBrand())
                        .model(t.getVehicule().getModel())
                        .seats(t.getVehicule().getSeats())
                        .energy(t.getVehicule().getEnergy())
                        .photoUrl(t.getVehicule().getPhotoUrl())
                        .build())
                .build();
    }
}
