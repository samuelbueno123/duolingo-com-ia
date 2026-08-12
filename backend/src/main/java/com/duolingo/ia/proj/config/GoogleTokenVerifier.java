// Bagulho para verificar o token do google que foi enviado para o back e saber se realmente é um ip_token válido ou não

package com.duolingo.ia.proj.config;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(
            @Value("${google.client-id}") String clientId)
            throws GeneralSecurityException, IOException {

        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(
                        Collections.singletonList(clientId)
                )
                .build();
    }

    public GoogleIdToken verify(String credential)
            throws GeneralSecurityException, IOException {

        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException(
                    "Google credential não informado."
            );
        }

        return verifier.verify(credential);
    }
}