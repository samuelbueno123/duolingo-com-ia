package com.duolinfo.ia.proj.controller.Auth;

import java.util.Map;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.GoogleLoginRequest;
import com.duolinfo.ia.proj.dto.request.PasswordLoginRequest;
import com.duolinfo.ia.proj.dto.request.RefreshTokenRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserRequestDTO;
import com.duolinfo.ia.proj.dto.response.AuthResponseDTO;
import com.duolinfo.ia.proj.dto.response.AuthenticatedUserResponseDTO;
import com.duolinfo.ia.proj.dto.response.UserResponseDTO;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.AuthService;
import com.duolinfo.ia.proj.service.AuthService.LoginResult;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Auth", description = "Autenticação e gerenciamento da sessão")
public class AuthController {

    private static final String GOOGLE_LOGIN_MESSAGE = "Login Google realizado com sucesso.";
    private static final String PASSWORD_LOGIN_MESSAGE = "Login realizado com sucesso.";

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;
    private final boolean secureRefreshCookie;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            UserService userService,
            @Value("${server.servlet.session.cookie.secure:false}")
            boolean secureRefreshCookie) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.secureRefreshCookie = secureRefreshCookie;
    }

    @PostMapping("/google")
    @Operation(summary = "Login com Google")
    public AuthResponseDTO loginGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Credencial do Google não informada."
            );
        }
        return createAuthResponse(
            authService.loginWithGoogle(request.getCredential()),
            GOOGLE_LOGIN_MESSAGE,
            response
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Login com email e senha")
    public AuthResponseDTO loginWithPassword(
            @Valid @RequestBody PasswordLoginRequest request,
            HttpServletResponse response) {
        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Email e senha são obrigatórios."
            );
        }
        return createAuthResponse(
            authService.loginWithPassword(request.getEmail(), request.getPassword()),
            PASSWORD_LOGIN_MESSAGE,
            response
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova a sessão usando o token de renovação")
    public AuthResponseDTO refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshCookie,
            @RequestBody(required = false) RefreshTokenRequestDTO request,
            HttpServletResponse response) {
        String refreshToken = refreshCookie != null && !refreshCookie.isBlank()
            ? refreshCookie
            : request == null ? null : request.refreshToken();
        return createAuthResponse(
            authService.refreshSession(refreshToken),
            "Sessão renovada com sucesso.",
            response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retorna o usuário autenticado")
    public AuthenticatedUserResponseDTO me(Authentication authentication) {
        User user = authenticatedUser(authentication);
        return new AuthenticatedUserResponseDTO(
            true,
            true,
            DtoMapper.toUserResponse(user),
            authService.resolveProfileType(user),
            authService.resolveRoles(user)
        );
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Encerra a sessão")
    public Map<String, Object> logout(HttpServletResponse response) {
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
            "message", "Logout realizado com sucesso."
        );
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza os dados do próprio usuário autenticado")
    public UserResponseDTO updateMe(
            Authentication authentication,
            @Valid @RequestBody UserRequestDTO request) {
        User user = authenticatedUser(authentication);
        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Os dados para atualização não foram informados."
            );
        }
        return DtoMapper.toUserResponse(
            userService.updateProfile(user.getId(), request)
        );
    }

    private AuthResponseDTO createAuthResponse(
            LoginResult result,
            String message,
            HttpServletResponse response) {
        String token = jwtService.generateToken(result.user(), result.profileType());
        String refreshToken = jwtService.generateRefreshToken(result.user());
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
            message,
            token,
            refreshToken,
            DtoMapper.toUserResponse(result.user()),
            result.profileType(),
            authService.resolveRoles(result.user())
        );
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuário não autenticado."
            );
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        User user = userService.findByEmail(authentication.getName());
        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuário autenticado não foi encontrado."
            );
        }
        return user;
    }
}
