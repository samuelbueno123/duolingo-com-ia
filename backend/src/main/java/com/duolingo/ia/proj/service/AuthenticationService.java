package com.duolingo.ia.proj.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.duolingo.ia.proj.entities.User;
import com.duolingo.ia.proj.requesters.GoogleLoginRequest;
import com.duolingo.ia.proj.requesters.GooglePayload;

/**
 * Fachada principal do caso de uso de autenticação.
 * Orquestra os componentes especializados e não depende da camada HTTP.
 */
@Service
public class AuthenticationService {

    private final GoogleAuthenticationProvider googleAuthenticationProvider;
    private final UserService userService;

    public AuthenticationService(
            GoogleAuthenticationProvider googleAuthenticationProvider,
            UserService userService) {
        this.googleAuthenticationProvider = googleAuthenticationProvider;
        this.userService = userService;
    }

    public Map<String, Object> loginWithGoogle(GoogleLoginRequest request) {
        String credential = request == null ? null : request.getCredential();
        GoogleUserData googleUser = googleAuthenticationProvider.authenticate(credential);
        User user = userService.createOrUpdateFromGoogle(googleUser);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Login Google realizado com sucesso.");
        response.put("user", new GooglePayload(
                user.getGoogleId(),
                user.getEmail(),
                user.getName(),
                user.getProfilePicture(),
                googleUser.hostedDomain()));
        return response;
    }
}
