package com.duolinfo.ia.proj.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.DeleteAccountRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserPreferencesRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserRequestDTO;
import com.duolinfo.ia.proj.dto.response.AuthResponseDTO;
import com.duolinfo.ia.proj.dto.response.UserPreferencesResponseDTO;
import com.duolinfo.ia.proj.dto.response.UserResponseDTO;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.mapper.RequestMapper;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(
    name = "Users",
    description = "Gerenciamento de usuários"
)
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final boolean secureRefreshCookie;

    @Autowired(required = false)
    private AccessControlService accessControlService;

    public UserController(
            UserService userService,
            JwtService jwtService,
            @Value("${server.servlet.session.cookie.secure:false}")
            boolean secureRefreshCookie) {

        this.userService = userService;
        this.jwtService = jwtService;
        this.secureRefreshCookie = secureRefreshCookie;
    }

    // ==========================================
    // CREATE
    // ==========================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cria um usuário",
        description = """
            Cria um novo usuário e retorna um JWT.
            A senha será criptografada antes de ser armazenada.
            """
    )
    public AuthResponseDTO create(
            @Valid @RequestBody UserRequestDTO request,
            HttpServletResponse response) {

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A senha é obrigatória para criar uma conta com email.");
        }

        var newUser = userService.create(RequestMapper.toNewUser(request));

        String token = jwtService.generateToken(
            newUser,
            "USER"
        );
        String refreshToken = jwtService.generateRefreshToken(newUser);
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(
                "refreshToken", refreshToken)
            .httpOnly(true)
            .secure(secureRefreshCookie)
            .sameSite(secureRefreshCookie ? "None" : "Lax")
            .path("/api/auth")
            .maxAge(Duration.ofSeconds(jwtService.getRefreshTokenLifetimeSeconds()))
            .build()
            .toString());

        return new AuthResponseDTO(
            true,
            "Usuário criado com sucesso.",
            token,
            refreshToken,
            DtoMapper.toUserResponse(newUser),
            "USER",
            List.of("USER")
        );
    }

    // ==========================================
    // PREFERENCES & SELF-MANAGEMENT
    // ==========================================

    @GetMapping("/me/preferences")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retorna as preferências do usuário autenticado")
    public UserPreferencesResponseDTO getMyPreferences() {
        User current = requireCurrentUser();
        return userService.getPreferences(current.getId());
    }

    @PutMapping("/me/preferences")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza as preferências do usuário autenticado")
    public UserPreferencesResponseDTO updateMyPreferences(
            @Valid @RequestBody UserPreferencesRequestDTO request) {
        User current = requireCurrentUser();
        return userService.updatePreferences(current.getId(), request);
    }

    @DeleteMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Exclui a própria conta do usuário autenticado")
    public Map<String, Object> deleteMyAccount(
            @RequestBody(required = false) DeleteAccountRequestDTO request,
            HttpServletResponse response) {
        User current = requireCurrentUser();
        userService.deleteOwnAccount(current.getId(), request);
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(secureRefreshCookie)
            .sameSite(secureRefreshCookie ? "None" : "Lax")
            .path("/api/auth")
            .maxAge(Duration.ZERO)
            .build()
            .toString());
        return Map.of(
            "success", true,
            "message", "Conta excluída com sucesso."
        );
    }

    // ==========================================
    // READ
    // ==========================================

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Lista todos os usuários",
        description = "Retorna todos os usuários cadastrados."
    )
    public List<UserResponseDTO> listAll() {

        return userService.findAll()
            .stream()
            .map(DtoMapper::toUserResponse)
            .toList();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuário por ID"
    )
    public UserResponseDTO findById(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanReadUser(id);
        }
        return DtoMapper.toUserResponse(userService.findById(id));
    }

    @GetMapping("/google/{googleId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuário por Google ID"
    )
    public UserResponseDTO findByGoogleId(
            @PathVariable String googleId) {
        User target = requireUser(userService.findByGoogleId(googleId));
        if (accessControlService != null) {
            accessControlService.assertCanReadUser(target.getId());
        }
        return DtoMapper.toUserResponse(target);
    }

    @GetMapping("/email/{email}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuário por email"
    )
    public UserResponseDTO findByEmail(
            @PathVariable String email) {
        User target = requireUser(userService.findByEmail(email));
        if (accessControlService != null) {
            accessControlService.assertCanReadUser(target.getId());
        }
        return DtoMapper.toUserResponse(target);
    }

    @GetMapping("/name/{name}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuários por nome"
    )
    public List<UserResponseDTO> findByName(
            @PathVariable String name) {

        return userService.findByName(name)
            .stream()
            .map(DtoMapper::toUserResponse)
            .toList();
    }

    // ==========================================
    // UPDATE
    // ==========================================

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Atualiza usuário",
        description = """
            Atualiza os dados básicos do usuário.

            Caso uma nova senha seja informada,
            ela será novamente criptografada.
            """
    )
    public void update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertSelfOrSuperAdmin(id);
        }
        userService.updateProfile(id, request);
    }

    @PatchMapping("/{id}/profile-picture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Atualiza somente a foto de perfil")
    public void updateProfilePicture(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertSelfOrSuperAdmin(id);
        }
        if (request.profilePicture() == null || request.profilePicture().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A foto de perfil não pode estar vazia."
            );
        }

        userService.updateProfilePicture(id, request.profilePicture());
    }

    // ==========================================
    // DELETE
    // ==========================================

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Exclui usuário"
    )
    public void deleteById(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertSelfOrSuperAdmin(id);
        }
        userService.deleteById(id);
    }

    private User requireCurrentUser() {
        if (accessControlService == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        return accessControlService.getCurrentUserOrThrow();
    }

    private static User requireUser(User user) {
        if (user == null) {
            throw new ResourceNotFoundException("Usuário não encontrado.");
        }
        return user;
    }
}
