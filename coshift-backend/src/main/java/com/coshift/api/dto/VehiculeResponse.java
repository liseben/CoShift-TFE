package com.coshift.api.dto;

import com.coshift.api.entity.EnergyType;
import com.coshift.api.entity.Vehicule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VehiculeResponse {

    private String uuid;
    private String brand;
    private String model;
    private String licensePlate;
    private int seats;
    private EnergyType energy;
    private String photoUrl;
    private LocalDateTime createdAt;

    public static VehiculeResponse from(Vehicule v) {
        return VehiculeResponse.builder()
                .uuid(v.getUuid())
                .brand(v.getBrand())
                .model(v.getModel())
                .licensePlate(v.getLicensePlate())
                .seats(v.getSeats())
                .energy(v.getEnergy())
                .photoUrl(v.getPhotoUrl())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
