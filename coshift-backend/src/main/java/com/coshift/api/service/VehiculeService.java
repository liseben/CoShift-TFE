package com.coshift.api.service;

import com.coshift.api.dto.VehiculeRequest;
import com.coshift.api.dto.VehiculeResponse;
import com.coshift.api.entity.User;
import com.coshift.api.entity.Vehicule;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final UserRepository userRepository;

    public List<VehiculeResponse> getMyVehicules(String email) {
        User owner = findUser(email);
        return vehiculeRepository.findByOwnerId(owner.getId())
                .stream()
                .map(VehiculeResponse::from)
                .toList();
    }

    @Transactional
    public VehiculeResponse addVehicule(String email, VehiculeRequest request) {
        User owner = findUser(email);

        // Unicité de la plaque d'immatriculation déjà garantie par la contrainte DB
        Vehicule vehicule = Vehicule.builder()
                .brand(request.getBrand())
                .model(request.getModel())
                .licensePlate(request.getLicensePlate().toUpperCase().trim())
                .seats(request.getSeats())
                .energy(request.getEnergy())
                .photoUrl(request.getPhotoUrl())
                .owner(owner)
                .build();

        return VehiculeResponse.from(vehiculeRepository.save(vehicule));
    }

    @Transactional
    public VehiculeResponse updateVehicule(String email, String uuid, VehiculeRequest request) {
        Vehicule vehicule = findOwnedVehicule(email, uuid);

        vehicule.setBrand(request.getBrand());
        vehicule.setModel(request.getModel());
        vehicule.setLicensePlate(request.getLicensePlate().toUpperCase().trim());
        vehicule.setSeats(request.getSeats());
        vehicule.setEnergy(request.getEnergy());
        if (request.getPhotoUrl() != null) {
            vehicule.setPhotoUrl(request.getPhotoUrl());
        }

        return VehiculeResponse.from(vehiculeRepository.save(vehicule));
    }

    @Transactional
    public void deleteVehicule(String email, String uuid) {
        Vehicule vehicule = findOwnedVehicule(email, uuid);
        vehiculeRepository.delete(vehicule);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private Vehicule findOwnedVehicule(String email, String uuid) {
        User owner = findUser(email);
        Vehicule vehicule = vehiculeRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable."));
        if (!vehicule.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas propriétaire de ce véhicule.");
        }
        return vehicule;
    }
}
