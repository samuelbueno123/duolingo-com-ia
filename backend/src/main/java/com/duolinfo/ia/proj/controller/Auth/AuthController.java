// Controller que recebe os endpoints do login

package com.duolinfo.ia.proj.controller.Auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.config.GoogleTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthController(
            GoogleTokenVerifier googleTokenVerifier) {

        this.googleTokenVerifier = googleTokenVerifier;
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginGoogle(
            @RequestBody GoogleLoginRequest request) {

        try {

            if (request == null || request.getCredential() == null || request.getCredential().isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Credencial do Google não informada."
                        )
                );
            }

            // ==========================================
            // VALIDAR CREDENTIAL DO GOOGLE
            // ==========================================

            GoogleIdToken idToken =
                    googleTokenVerifier.verify(
                            request.getCredential()
                    );


            // ==========================================
            // TOKEN INVÁLIDO
            // ==========================================

            if (idToken == null) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(
                                Map.of(
                                        "success", false,
                                        "message",
                                        "Token do Google inválido."
                                )
                        );
            }


            // ==========================================
            // PAYLOAD DO TOKEN
            // ==========================================

            Payload payload =
                    idToken.getPayload();


            // ==========================================
            // DADOS DO GOOGLE
            // ==========================================

            String googleId =
                    payload.getSubject();

            String email =
                    payload.getEmail();

            String name =
                    (String) payload.get("name");

            String picture =
                    (String) payload.get("picture");

            String hostedDomain =
                    (String) payload.get("hd");


            // ==========================================
            // CRIAR RESPOSTA
            // ==========================================

            GooglePayload googlePayload =
                    new GooglePayload(
                            googleId,
                            email,
                            name,
                            picture,
                            hostedDomain
                    );


            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Login Google realizado com sucesso."
            );

            response.put(
                    "user",
                    googlePayload
            );


            return ResponseEntity.ok(response);

        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "Não foi possível validar o token do Google."
                            )
                    );
        }
    }
}
