package com.coshift.api.controller.auth;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.LoginRequest;
import com.coshift.api.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // C'est l'URL que j'ai mise dans ton LoginPage.tsx !
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }
    
    // (Bonus pour plus tard : on ajoutera la méthode /register ici)
}