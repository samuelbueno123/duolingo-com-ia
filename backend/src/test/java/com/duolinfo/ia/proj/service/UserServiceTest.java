package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.service.ProfilePictureStorageService;
import com.duolinfo.ia.proj.config.UserAuthCache;
import com.duolinfo.ia.proj.dto.request.DeleteAccountRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserPreferencesRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserRequestDTO;
import com.duolinfo.ia.proj.dto.response.UserPreferencesResponseDTO;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.UserRepository;

import jakarta.persistence.EntityManager;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private ProfilePictureStorageService profilePictureStorage;
    private UserAuthCache userAuthCache;
    private JdbcTemplate jdbcTemplate;
    private EntityManager entityManager;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        profilePictureStorage = mock(ProfilePictureStorageService.class);
        userAuthCache = mock(UserAuthCache.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        entityManager = mock(EntityManager.class);

        userService = new UserService(
            userRepository,
            passwordEncoder,
            profilePictureStorage
        );
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "jdbcTemplate", jdbcTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "entityManager", entityManager);
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "userAuthCache", userAuthCache);
    }

    @Test
    void updateProfile_updatesFieldsAndEncodesPasswordIfValid() {
        User user = new User();
        user.setId(10L);
        user.setName("Old Name");
        user.setEmail("old@example.com");
        user.setGender("MASCULINO");
        user.setPasswordHash(passwordEncoder.encode("oldPassword123"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRequestDTO dto = new UserRequestDTO(
            "New Name",
            "new@example.com",
            null,
            null,
            "oldPassword123",
            "newPassword456",
            "FEMININO"
        );

        User updated = userService.updateProfile(10L, dto);

        assertEquals("New Name", updated.getName());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("FEMININO", updated.getGender());
        assertTrue(passwordEncoder.matches("newPassword456", updated.getPasswordHash()));
    }

    @Test
    void updateProfile_withWrongCurrentPassword_throwsBadRequest() {
        User user = new User();
        user.setId(10L);
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("correctPassword"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserRequestDTO dto = new UserRequestDTO(
            "Name",
            "test@example.com",
            null,
            null,
            "wrongPassword",
            "newPassword",
            null
        );

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userService.updateProfile(10L, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("A senha atual informada está incorreta"));
    }

    @Test
    void updateProfile_googleAccountCannotChangeEmail_throwsBadRequest() {
        User user = new User();
        user.setId(10L);
        user.setEmail("googleuser@gmail.com");
        user.setGoogleId("google-sub-12345");

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserRequestDTO dto = new UserRequestDTO(
            "Name",
            "another@gmail.com",
            null,
            null,
            null,
            null,
            null
        );

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userService.updateProfile(10L, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("O email de uma conta Google não pode ser alterado"));
    }

    @Test
    void deleteOwnAccount_withSuperAdmin_throwsConflict() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@duolingo.com");
        admin.setAdmin(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userService.deleteOwnAccount(1L, new DeleteAccountRequestDTO("password", null))
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("admin@duolingo.com) não pode ser excluída"));
    }

    @Test
    void deleteOwnAccount_withIncorrectPassword_throwsBadRequest() {
        User user = new User();
        user.setId(20L);
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("mypassword"));

        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userService.deleteOwnAccount(20L, new DeleteAccountRequestDTO("badpass", null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Informe sua senha atual correta"));
    }

    @Test
    void deleteOwnAccount_googleAccountWithoutPassword_validatesConfirmationText() {
        User googleUser = new User();
        googleUser.setId(30L);
        googleUser.setEmail("google@example.com");
        googleUser.setPasswordHash(null);
        googleUser.setGoogleId("gid-999");

        when(userRepository.findById(30L)).thenReturn(Optional.of(googleUser));

        // Invalid confirmation text throws
        assertThrows(
            ResponseStatusException.class,
            () -> userService.deleteOwnAccount(30L, new DeleteAccountRequestDTO(null, "WRONG"))
        );

        // Valid confirmation text "EXCLUIR" succeeds
        userService.deleteOwnAccount(30L, new DeleteAccountRequestDTO(null, "EXCLUIR"));
        verify(userRepository).delete(googleUser);
    }

    @Test
    void deleteById_cascadesCleanupAcrossAllTablesIncludingSrsAndAi() {
        User user = new User();
        user.setId(50L);
        user.setEmail("student@example.com");

        when(userRepository.findById(50L)).thenReturn(Optional.of(user));

        userService.deleteById(50L);

        // Verify valid cleanup statements executed on jdbcTemplate and auth cache evicted
        verify(jdbcTemplate, times(30)).update(anyString(), eq(50L));
        verify(userAuthCache).evict(50L);
        verify(userAuthCache).evictByEmail("student@example.com");
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(userRepository).delete(user);
    }

    @Test
    void preferences_normalizesLanguagesAndDailyGoalMinutes() {
        assertEquals("pt", UserService.normalizeUiLanguage("pt-BR"));
        assertEquals("en", UserService.normalizeUiLanguage("English"));
        assertEquals("es", UserService.normalizeUiLanguage("espanhol"));
        assertEquals("system", UserService.normalizeThemeMode(null));
        assertEquals("dark", UserService.normalizeThemeMode("escuro"));
        assertEquals("light", UserService.normalizeThemeMode("claro"));

        assertEquals(15, UserService.normalizeDailyGoalMinutes(null));
        assertEquals(30, UserService.normalizeDailyGoalMinutes(30));

        assertThrows(ResponseStatusException.class, () -> UserService.normalizeDailyGoalMinutes(4));
        assertThrows(ResponseStatusException.class, () -> UserService.normalizeDailyGoalMinutes(181));
    }
}
