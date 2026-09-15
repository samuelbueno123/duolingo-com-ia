package com.duolinfo.ia.proj;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.duolinfo.ia.proj.config.GoogleTokenVerifier;
import com.duolinfo.ia.proj.controller.Auth.GooglePayload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import jakarta.servlet.http.HttpSession;

@SpringBootTest(
        classes = ProjApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:duolingo-test;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "spring.h2.console.enabled=false"
        }
)
class SecurityAndAuthIntegrationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    private MockMvc mockMvc;

    @BeforeEach
    void configureMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldBlockHomeWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Autenticação necessária."));
    }

    @Test
    void shouldBlockStudentControllerWithoutAuthentication()
            throws Exception {

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldBlockTeacherControllerWithoutAuthentication()
            throws Exception {

        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldNotExposeLegacyUserPaths() throws Exception {
        GooglePayload user = new GooglePayload(
                "legacy-user-google-id",
                "legacy@example.com",
                "Usuário Legado",
                null,
                null
        );

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        mockMvc.perform(get("/User/students").with(authentication(authentication)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/User/teachers").with(authentication(authentication)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/User").with(authentication(authentication)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectGoogleLoginWithoutCredential()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Credencial do Google não informada."));
    }

    @Test
    void shouldLoginWithGoogleAndCreateSession()
            throws Exception {

        GoogleIdToken googleIdToken = createGoogleIdToken();

        when(googleTokenVerifier.verify("valid-google-token"))
                .thenReturn(googleIdToken);

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "credential": "valid-google-token"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.googleId")
                        .value("google-user-123"))
                .andExpect(jsonPath("$.user.email")
                        .value("marcus@example.com"))
                .andExpect(jsonPath("$.user.name")
                        .value("Marcus Vinicius"))
                .andExpect(jsonPath("$.profileType")
                        .value("USER"))
                .andReturn();

        HttpSession session =
                loginResult.getRequest().getSession(false);

        if (session == null) {
            throw new AssertionError(
                    "O login deveria ter criado uma sessão."
            );
        }

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(
                                        (org.springframework.mock.web.MockHttpSession)
                                                session
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email")
                        .value("marcus@example.com"))
                .andExpect(jsonPath("$.profileType")
                        .value("USER"));

        mockMvc.perform(
                        get("/api/home")
                                .session(
                                        (org.springframework.mock.web.MockHttpSession)
                                                session
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.name")
                        .value("Marcus Vinicius"))
                .andExpect(jsonPath("$.profileType")
                        .value("USER"))
                .andExpect(jsonPath("$.profileCompleted")
                        .value(false));
    }

    @Test
    void shouldAllowAuthenticatedStudentToAccessHome()
            throws Exception {

        GooglePayload student = new GooglePayload(
                "student-google-id",
                "student@example.com",
                "Estudante Teste",
                null,
                null
        );

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        student,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_STUDENT"
                                )
                        )
                );

        mockMvc.perform(
                        get("/api/home")
                                .with(authentication(authentication))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.email")
                        .value("student@example.com"))
                .andExpect(jsonPath("$.profileType")
                        .value("STUDENT"))
                .andExpect(jsonPath("$.profileCompleted")
                        .value(true));
    }

    @Test
    void shouldAllowAuthenticatedTeacherToAccessHome()
            throws Exception {

        GooglePayload teacher = new GooglePayload(
                "teacher-google-id",
                "teacher@example.com",
                "Professor Teste",
                null,
                null
        );

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        teacher,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_TEACHER"
                                )
                        )
                );

        mockMvc.perform(
                        get("/api/home")
                                .with(authentication(authentication))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileType")
                        .value("TEACHER"))
                .andExpect(jsonPath("$.profileCompleted")
                        .value(true));
    }

    @Test
    void shouldLogoutAuthenticatedUser() throws Exception {
        GoogleIdToken googleIdToken = createGoogleIdToken();

        when(googleTokenVerifier.verify("valid-google-token"))
                .thenReturn(googleIdToken);

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "credential": "valid-google-token"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andReturn();

        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession)
                        loginResult
                                .getRequest()
                                .getSession(false);

        mockMvc.perform(
                        post("/api/auth/logout")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Logout realizado com sucesso."));
    }

    private GoogleIdToken createGoogleIdToken() {
        Payload payload = new Payload();

        payload.setSubject("google-user-123");
        payload.setEmail("marcus@example.com");
        payload.setEmailVerified(true);
        payload.set("name", "Marcus Vinicius");
        payload.set("picture", "https://example.com/profile.png");

        GoogleIdToken googleIdToken =
                mock(GoogleIdToken.class);

        when(googleIdToken.getPayload())
                .thenReturn(payload);

        return googleIdToken;
    }
}