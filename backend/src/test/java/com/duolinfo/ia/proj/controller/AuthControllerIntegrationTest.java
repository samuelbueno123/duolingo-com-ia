package com.duolinfo.ia.proj.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.controller.Auth.AuthController;
import com.duolinfo.ia.proj.dto.request.ForgotPasswordRequestDTO;
import com.duolinfo.ia.proj.dto.request.GoogleLoginRequest;
import com.duolinfo.ia.proj.dto.request.PasswordLoginRequest;
import com.duolinfo.ia.proj.dto.request.RefreshTokenRequestDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.service.AuthService;
import com.duolinfo.ia.proj.service.AuthService.LoginResult;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.PasswordResetService;
import com.duolinfo.ia.proj.service.UserService;

class AuthControllerIntegrationTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private JwtService jwtService;
    private UserService userService;
    private PasswordResetService passwordResetService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        jwtService = mock(JwtService.class);
        userService = mock(UserService.class);
        passwordResetService = mock(PasswordResetService.class);
        objectMapper = new ObjectMapper();

        AuthController controller = new AuthController(
            authService,
            jwtService,
            userService,
            false // secureRefreshCookie
        );
        ReflectionTestUtils.setField(controller, "passwordResetService", passwordResetService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_withValidCredentials_returns200AndSetsCookie() throws Exception {
        Student student = new Student();
        student.setId(10L);
        student.setEmail("student@example.com");
        student.setName("Student Name");

        LoginResult result = new LoginResult(student, "STUDENT");

        when(authService.loginWithPassword("student@example.com", "Password123!")).thenReturn(result);
        when(jwtService.generateToken(student, "STUDENT")).thenReturn("access-token-123");
        when(jwtService.generateRefreshToken(student)).thenReturn("refresh-token-456");
        when(jwtService.getRefreshTokenLifetimeSeconds()).thenReturn(604800L);
        when(authService.resolveRoles(student)).thenReturn(List.of("ROLE_STUDENT"));

        PasswordLoginRequest req = new PasswordLoginRequest("student@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", is("access-token-123")))
            .andExpect(jsonPath("$.user.email", is("student@example.com")))
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token-456")));
    }

    @Test
    void login_withBadCredentials_returns401() throws Exception {
        when(authService.loginWithPassword("bad@example.com", "wrong"))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha incorretos."));

        PasswordLoginRequest req = new PasswordLoginRequest("bad@example.com", "wrong");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLogin_withInvalidGoogleToken_returns401() throws Exception {
        when(authService.loginWithGoogle("invalid-google-token"))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do Google inválido."));

        GoogleLoginRequest req = new GoogleLoginRequest("invalid-google-token");

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withValidCookie_returnsNewTokens() throws Exception {
        Student student = new Student();
        student.setId(10L);
        student.setEmail("student@example.com");
        student.setName("Student Name");

        LoginResult result = new LoginResult(student, "STUDENT");

        when(authService.refreshSession("valid-refresh-token")).thenReturn(result);
        when(jwtService.generateToken(student, "STUDENT")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(student)).thenReturn("new-refresh-token");
        when(jwtService.getRefreshTokenLifetimeSeconds()).thenReturn(604800L);
        when(authService.resolveRoles(student)).thenReturn(List.of("ROLE_STUDENT"));

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refreshToken", "valid-refresh-token"))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", is("new-access-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new-refresh-token")));
    }

    @Test
    void refresh_withExpiredRefreshToken_returns401() throws Exception {
        when(authService.refreshSession("expired-token"))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada. Faça login novamente."));

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refreshToken", "expired-token"))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new Cookie("refreshToken", "active-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", is("Logout realizado com sucesso.")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")));
    }

    @Test
    void me_withAuthenticatedUser_returnsUserProfile() throws Exception {
        Student student = new Student();
        student.setId(10L);
        student.setEmail("me@example.com");
        student.setName("My Profile");

        when(userService.findByEmail("me@example.com")).thenReturn(student);
        when(authService.resolveProfileType(student)).thenReturn("STUDENT");
        when(authService.resolveRoles(student)).thenReturn(List.of("ROLE_STUDENT"));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "me@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get("/api/auth/me")
                .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email", is("me@example.com")))
            .andExpect(jsonPath("$.user.name", is("My Profile")))
            .andExpect(jsonPath("$.profileType", is("STUDENT")));
    }

    @Test
    void forgotPassword_returnsRecoveryInstructions() throws Exception {
        Map<String, Object> response = Map.of(
            "message", "Se o email estiver cadastrado, as instruções foram enviadas.",
            "email", "student@example.com"
        );

        when(passwordResetService.requestPasswordReset(any(ForgotPasswordRequestDTO.class))).thenReturn(response);

        ForgotPasswordRequestDTO req = new ForgotPasswordRequestDTO("student@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("student@example.com")));
    }
}
