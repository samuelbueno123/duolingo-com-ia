package com.duolinfo.ia.proj.controller.Auth;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.config.GoogleTokenVerifier;
import com.duolinfo.ia.proj.dto.response.AuthResponseDTO;
import com.duolinfo.ia.proj.dto.response.AuthenticatedUserResponseDTO;
import com.duolinfo.ia.proj.dto.response.ApiErrorResponse;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.AuthService;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.StudentService;
import com.duolinfo.ia.proj.service.TeacherService;
import com.duolinfo.ia.proj.service.UserService;
import com.duolinfo.ia.proj.dto.request.GoogleLoginRequest;
import com.duolinfo.ia.proj.dto.request.PasswordLoginRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
@CrossOrigin(
    origins = "*",
    allowedHeaders = "*"
)
@Tag(
    name = "Auth",
    description = "Autenticação e gerenciamento da sessão"
)
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthService authService;

    /*
     * Usado apenas para o caso de criação/definição
     * de senha dentro do fluxo legado.
     */
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            GoogleTokenVerifier googleTokenVerifier,
            StudentService studentService,
            TeacherService teacherService,
            UserService userService,
            JwtService jwtService,
            AuthService authService,
            PasswordEncoder passwordEncoder) {

        this.googleTokenVerifier = googleTokenVerifier;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.userService = userService;
        this.jwtService = jwtService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================
    // LOGIN GOOGLE
    // =========================================================

    @PostMapping("/google")
    @Operation(
        summary = "Login com Google",
        description = """
            Recebe a credential do Google Identity Services,
            valida o token, cria ou atualiza o usuário e emite
            um JWT da aplicação.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login realizado com sucesso",
            content = @Content(
                schema = @Schema(
                    implementation = AuthResponseDTO.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Credential não informada"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token Google inválido"
        )
    })
    public ResponseEntity<?> loginGoogle(
            @Valid @RequestBody GoogleLoginRequest request) {

        try {

            if (request == null
                    || request.getCredential() == null
                    || request.getCredential().isBlank()) {

                return ResponseEntity
                    .badRequest()
                    .body(
                        createError(
                            HttpStatus.BAD_REQUEST,
                            "Credencial do Google não informada.",
                            "/api/auth/google"
                        )
                    );
            }

            GoogleIdToken idToken =
                googleTokenVerifier.verify(
                    request.getCredential()
                );

            if (idToken == null) {

                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                        createError(
                            HttpStatus.UNAUTHORIZED,
                            "Token do Google inválido.",
                            "/api/auth/google"
                        )
                    );
            }

            Payload payload =
                idToken.getPayload();

            if (!Boolean.TRUE.equals(
                    payload.getEmailVerified())) {

                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                        createError(
                            HttpStatus.UNAUTHORIZED,
                            "O Google não confirmou este endereço de email.",
                            "/api/auth/google"
                        )
                    );
            }

            String googleId =
                payload.getSubject();

            String email =
                payload.getEmail();

            String name =
                (String) payload.get("name");

            String picture =
                (String) payload.get("picture");

            /*
             * O email vindo do Google deve ser normalizado.
             */
            if (email != null) {
                email = email.trim().toLowerCase();
            }

            /*
             * Se por algum motivo o Google não fornecer nome,
             * usamos uma identificação mínima.
             */
            if (name == null || name.isBlank()) {
                name = email;
            }

            User user =
                userService.findByEmail(email);

            if (user == null) {

                user = new User();

                user.setEmail(email);
                user.setName(name);
                user.setGoogleId(googleId);
                user.setProfilePicture(picture);

                /*
                 * Nenhuma senha é definida para login Google.
                 */
                user.setPasswordHash(null);

                user =
                    userService.create(user);

            } else {

                boolean changed = false;

                if (user.getGoogleId() == null
                        || !user.getGoogleId().equals(googleId)) {

                    user.setGoogleId(googleId);
                    changed = true;
                }

                if (picture != null
                        && !picture.equals(
                            user.getProfilePicture())) {

                    user.setProfilePicture(picture);
                    changed = true;
                }

                if ((user.getName() == null
                        || user.getName().isBlank())
                        && name != null
                        && !name.isBlank()) {

                    user.setName(name);
                    changed = true;
                }

                if (changed) {
                    user =
                        userService.update(user);
                }
            }

            String profileType =
                resolveProfileType(
                    googleId,
                    email
                );

            String token =
                jwtService.generateToken(
                    user,
                    profileType
                );

            AuthResponseDTO response =
                new AuthResponseDTO(
                    true,
                    "Login Google realizado com sucesso.",
                    token,
                    DtoMapper.toUserResponse(user),
                    profileType
                );

            return ResponseEntity.ok(response);

        } catch (
                GeneralSecurityException
                | IOException
                | IllegalArgumentException exception) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    createError(
                        HttpStatus.UNAUTHORIZED,
                        "Não foi possível validar o token do Google.",
                        "/api/auth/google"
                    )
                );
        }
    }

    // =========================================================
    // LOGIN EMAIL + SENHA
    // =========================================================

    @PostMapping("/login")
    @Operation(
        summary = "Login com email e senha",
        description = """
            Autentica um usuário utilizando email e senha
            e retorna o JWT da aplicação.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login realizado com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Email ou senha inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciais inválidas"
        )
    })
    public ResponseEntity<?> loginWithPassword(
            @Valid @RequestBody PasswordLoginRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            return ResponseEntity
                .badRequest()
                .body(
                    createError(
                        HttpStatus.BAD_REQUEST,
                        "Email e senha são obrigatórios.",
                        "/api/auth/login"
                    )
                );
        }

        String email =
            request.getEmail()
                .trim()
                .toLowerCase();

        String rawPassword =
            request.getPassword();

        User user =
            userService.findByEmail(email);

        /*
         * Primeiro acesso usando email/senha.
         */
        if (user == null) {

            String defaultName =
                email.contains("@")
                    ? email.substring(
                        0,
                        email.indexOf("@")
                    )
                    : email;

            if (!defaultName.isEmpty()) {

                defaultName =
                    Character.toUpperCase(
                        defaultName.charAt(0)
                    )
                    + defaultName.substring(1);
            }

            user = new User();

            user.setEmail(email);
            user.setName(defaultName);

            /*
             * IMPORTANTE:
             * enviamos a senha pura para o UserService.
             *
             * O UserService é quem gera o hash.
             */
            user.setPasswordHash(rawPassword);

            user =
                userService.create(user);

        } else if (
                user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()) {

            /*
             * Usuário criado anteriormente apenas com Google.
             *
             * Aqui mantive o comportamento que você já tinha:
             * permite definir uma senha.
             *
             * IMPORTANTE:
             * a senha é enviada pura ao UserService para que
             * ele faça UMA única criptografia.
             */
            user.setPasswordHash(rawPassword);

            user =
                userService.update(user);

        } else {

            /*
             * Usuário já possui senha.
             * AuthService utiliza BCrypt.matches().
             */
            try {

                user =
                    authService.authenticate(
                        email,
                        rawPassword
                    );

            } catch (ResponseStatusException exception) {

                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                        createError(
                            HttpStatus.UNAUTHORIZED,
                            "Credenciais inválidas.",
                            "/api/auth/login"
                        )
                    );
            }
        }

        String googleId =
            user.getGoogleId() != null
                ? user.getGoogleId()
                : "";

        String profileType =
            resolveProfileType(
                googleId,
                email
            );

        String token =
            jwtService.generateToken(
                user,
                profileType
            );

        AuthResponseDTO response =
            new AuthResponseDTO(
                true,
                "Login realizado com sucesso.",
                token,
                DtoMapper.toUserResponse(user),
                profileType
            );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // USUÁRIO AUTENTICADO
    // =========================================================

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Retorna o usuário autenticado",
        description = """
            Retorna os dados públicos do usuário associado
            ao JWT atualmente autenticado.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuário autenticado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token ausente ou inválido"
        )
    })
    public ResponseEntity<?> me(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    createError(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário não autenticado.",
                        "/api/auth/me"
                    )
                );
        }

        User user =
            findAuthenticatedUser(
                authentication
            );

        if (user == null) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    createError(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não foi encontrado.",
                        "/api/auth/me"
                    )
                );
        }

        String googleId =
            user.getGoogleId() != null
                ? user.getGoogleId()
                : "";

        String profileType =
            resolveProfileType(
                googleId,
                user.getEmail()
            );

        AuthenticatedUserResponseDTO response =
            new AuthenticatedUserResponseDTO(
                true,
                true,
                DtoMapper.toUserResponse(user),
                profileType
            );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Encerra a sessão",
        description = """
            Como a autenticação utiliza JWT stateless,
            o servidor não precisa destruir uma sessão.
            O frontend deve remover o JWT armazenado.
            """
    )
    public ResponseEntity<?> logout() {

        return ResponseEntity.ok(
            java.util.Map.of(
                "success", true,
                "message", "Logout realizado com sucesso."
            )
        );
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private User findAuthenticatedUser(
            Authentication authentication) {

        Object principal =
            authentication.getPrincipal();

        /*
         * Caso seu filtro JWT já coloque a própria entidade User
         * no Authentication.
         */
        if (principal instanceof User user) {
            return user;
        }

        /*
         * Caso use UserDetails, normalmente authentication.getName()
         * corresponde ao username/email.
         */
        String username =
            authentication.getName();

        if (username == null
                || username.isBlank()) {
            return null;
        }

        return userService.findByEmail(
            username
        );
    }

    private String resolveProfileType(
            String googleId,
            String email) {

        /*
         * Evita consultas desnecessárias com googleId vazio.
         */
        if (googleId != null
                && !googleId.isBlank()) {

            if (teacherService.findByGoogleId(
                    googleId) != null) {

                return "TEACHER";
            }

            if (studentService.findByGoogleId(
                    googleId) != null) {

                return "STUDENT";
            }
        }

        if (email != null
                && !email.isBlank()) {

            if (teacherService.findByEmail(
                    email) != null) {

                return "TEACHER";
            }

            if (studentService.findByEmail(
                    email) != null) {

                return "STUDENT";
            }
        }

        return "USER";
    }

    private ApiErrorResponse createError(
            HttpStatus status,
            String message,
            String path) {

        return new ApiErrorResponse(
            java.time.LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path
        );
    }
}