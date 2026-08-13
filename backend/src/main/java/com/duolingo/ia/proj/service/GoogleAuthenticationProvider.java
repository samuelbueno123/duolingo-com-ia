package com.duolingo.ia.proj.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;

import com.duolingo.ia.proj.config.GoogleTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

/**
 * Componente especializado em autenticação Google.
 * Microsoft e GitHub poderão ter componentes equivalentes no futuro.
 */
@Service
public class GoogleAuthenticationProvider {

    private final GoogleTokenVerifier tokenVerifier;

    public GoogleAuthenticationProvider(GoogleTokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    public GoogleUserData authenticate(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new InvalidGoogleCredentialException("Credencial do Google não informada.");
        }

        try {
            GoogleIdToken idToken = tokenVerifier.verify(credential);
            if (idToken == null) {
                throw new InvalidGoogleCredentialException("Token do Google inválido.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new InvalidGoogleCredentialException("O token do Google não contém e-mail.");
            }

            return new GoogleUserData(
                    payload.getSubject(),
                    email,
                    (String) payload.get("name"),
                    (String) payload.get("picture"),
                    (String) payload.get("hd"));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new InvalidGoogleCredentialException("Não foi possível validar o token do Google.");
        }
    }
}
