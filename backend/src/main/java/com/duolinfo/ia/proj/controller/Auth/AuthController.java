package com.duolinfo.ia.proj.controller.Auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.config.GoogleTokenVerifier;
import com.duolinfo.ia.proj.service.StudentService;
import com.duolinfo.ia.proj.service.TeacherService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Auth",
        description = "Autenticação via Google e gerenciamento da sessão do usuário"
)
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            GoogleTokenVerifier googleTokenVerifier,
            StudentService studentService,
            TeacherService teacherService,
            SecurityContextRepository securityContextRepository) {

        this.googleTokenVerifier = googleTokenVerifier;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/google")
    @Operation(
            summary = "Realiza login com Google",
            description = "Valida o token do Google, cria a sessão autenticada e retorna os dados do usuário e o tipo de perfil."
    )
    public ResponseEntity<?> loginGoogle(
            @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            if (request == null
                    || request.getCredential() == null
                    || request.getCredential().isBlank()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Credencial do Google não informada."
                        )
                );
            }

            GoogleIdToken idToken =
                    googleTokenVerifier.verify(request.getCredential());

            if (idToken == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(
                                Map.of(
                                        "success", false,
                                        "message", "Token do Google inválido."
                                )
                        );
            }

            Payload payload = idToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(
                                Map.of(
                                        "success", false,
                                        "message",
                                        "O Google não confirmou este endereço de email."
                                )
                        );
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String hostedDomain = (String) payload.get("hd");

            GooglePayload googlePayload = new GooglePayload(
                    googleId,
                    email,
                    name,
                    picture,
                    hostedDomain
            );

            String profileType = resolveProfileType(googleId, email);

            Authentication authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            googlePayload,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + profileType
                                    )
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("success", true);
            response.put(
                    "message",
                    "Login Google realizado com sucesso."
            );
            response.put("user", googlePayload);
            response.put("profileType", profileType);

            return ResponseEntity.ok(response);

        } catch (GeneralSecurityException
                 | IOException
                 | IllegalArgumentException exception) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Não foi possível validar o token do Google."
                            )
                    );
        }
    }

    @GetMapping("/me")
    @Operation(
            summary = "Retorna o usuário autenticado",
            description = "Informa se existe uma sessão autenticada e retorna o principal e o perfil atual."
    )
    public ResponseEntity<?> me(Authentication authentication) {
        return ResponseEntity.ok(
                authenticatedResponse(authentication)
        );
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Encerra a sessão",
            description = "Invalida a sessão HTTP atual e limpa o contexto de segurança."
    )
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Logout realizado com sucesso."
                )
        );
    }

    private Map<String, Object> authenticatedResponse(
            Authentication authentication) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "authenticated",
                authentication != null && authentication.isAuthenticated()
        );
        response.put(
                "user",
                authentication != null
                        ? authentication.getPrincipal()
                        : null
        );
        response.put(
                "profileType",
                authentication == null
                        ? null
                        : getProfileType(authentication)
        );

        return response;
    }

    private String getProfileType(Authentication authentication) {
        return authentication
                .getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority
                                .getAuthority()
                                .replaceFirst("^ROLE_", "")
                )
                .orElse("USER");
    }

    private String resolveProfileType(
            String googleId,
            String email) {

        if (teacherService.findByGoogleId(googleId) != null
                || (email != null
                && teacherService.findByEmail(email) != null)) {

            return "TEACHER";
        }

        if (studentService.findByGoogleId(googleId) != null
                || (email != null
                && studentService.findByEmail(email) != null)) {

            return "STUDENT";
        }

        return "USER";
    }
}