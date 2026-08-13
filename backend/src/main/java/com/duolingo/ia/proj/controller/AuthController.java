package com.duolingo.ia.proj.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolingo.ia.proj.requesters.GoogleLoginRequest;
import com.duolingo.ia.proj.service.AuthenticationService;
import com.duolingo.ia.proj.service.InvalidGoogleCredentialException;

/** Camada HTTP do login. Não contém regra de autenticação ou acesso ao banco. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginGoogle(@RequestBody GoogleLoginRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.loginWithGoogle(request));
        } catch (InvalidGoogleCredentialException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }
}
