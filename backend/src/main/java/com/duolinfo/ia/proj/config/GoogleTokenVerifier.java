package com.duolinfo.ia.proj.config;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

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
            @Value("${GOOGLE_CLIENT_ID}") String clientId)
            throws GeneralSecurityException, IOException {

        List<String> audienceList = Arrays.stream(clientId.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(audienceList)
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