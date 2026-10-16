package com.duolinfo.ia.proj.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.config.GoogleTokenVerifier;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final UserService userService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ProfilePictureStorageService profilePictureStorage;
    private final Set<String> adminEmails;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GoogleTokenVerifier googleTokenVerifier,
            JwtService jwtService,
            UserService userService,
            StudentService studentService,
            TeacherService teacherService,
            ProfilePictureStorageService profilePictureStorage,
            @Value("${app.admin-emails:}") String configuredAdminEmails) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
        this.userService = userService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.profilePictureStorage = profilePictureStorage;
        this.adminEmails = Arrays.stream(configuredAdminEmails.split(","))
            .map(AuthService::normalizeEmail)
            .filter(email -> email != null)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public LoginResult loginWithPassword(String email, String rawPassword) {
        User user = authenticate(email, rawPassword);
        ensureAdminTeacherProfile(user);
        return new LoginResult(user, resolveProfileType(user));
    }

    @Transactional
    public LoginResult loginWithGoogle(String credential) {
        GoogleIdToken idToken = verifyGoogleCredential(credential);
        if (idToken == null) {
            throw unauthorized("Token do Google inválido.");
        }

        Payload payload = idToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw unauthorized("O Google não confirmou este endereço de email.");
        }

        String googleId = payload.getSubject();
        String email = normalizeEmail(payload.getEmail());
        if (googleId == null || googleId.isBlank() || email == null) {
            throw unauthorized("O Google não retornou uma conta válida.");
        }

        String name = stringClaim(payload, "name");
        if (name == null || name.isBlank()) {
            name = email;
        }
        String picture = stringClaim(payload, "picture");

        User user = findOrCreateGoogleUser(googleId, email, name, picture);
        ensureAdminTeacherProfile(user);
        return new LoginResult(user, resolveProfileType(user));
    }

    @Transactional
    public LoginResult refreshSession(String refreshToken) {
        if (refreshToken == null || !jwtService.isRefreshTokenValid(refreshToken)) {
            throw unauthorized("Sessão expirada. Entre novamente.");
        }

        Long userId = jwtService.getUserIdFromToken(refreshToken).orElse(null);
        if (userId == null) {
            throw unauthorized("Token de renovação inválido.");
        }

        User user = userService.findById(userId);
        ensureAdminTeacherProfile(user);
        return new LoginResult(user, resolveProfileType(user));
    }

    public User authenticate(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        User user = normalizedEmail == null
            ? null
            : userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> userRepository.findByEmail(normalizedEmail).orElse(null));

        if (user == null) {
            throw unauthorized("Credenciais inválidas. Email ou senha incorretos.");
        }

        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            if (user.getGoogleId() != null && !user.getGoogleId().isBlank()) {
                throw unauthorized(
                    "Esta conta foi criada via Google. Por favor, entre usando o botão do Google."
                );
            }
            throw unauthorized("Credenciais inválidas. Senha não cadastrada.");
        }

        if (passwordEncoder.matches(rawPassword, passwordHash)) {
            return user;
        }

        if (rawPassword.equals(passwordHash)) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            return userRepository.save(user);
        }

        throw unauthorized("Credenciais inválidas. Email ou senha incorretos.");
    }

    public String resolveProfileType(User user) {
        return resolveRoles(user).get(0);
    }

    public List<String> resolveRoles(User user) {
        if (user == null || user.getId() == null) {
            return List.of("USER");
        }

        Set<String> roles = new LinkedHashSet<>();
        boolean admin = user.isAdmin() || isConfiguredAdmin(user);
        if (admin) {
            roles.add("ADMIN");
        }
        if (admin || teacherService.isApproved(user.getId())) {
            roles.add("TEACHER");
        }
        if (studentService.existsById(user.getId())) {
            roles.add("STUDENT");
        }
        if (roles.isEmpty()) {
            roles.add("USER");
        }
        return List.copyOf(roles);
    }

    private void ensureAdminTeacherProfile(User user) {
        if (user.isAdmin() || isConfiguredAdmin(user)) {
            teacherService.ensureAdminTeacherProfile(user.getId());
        }
    }

    public boolean isConfiguredAdmin(User user) {
        return Boolean.TRUE.equals(user.getGoogleEmailVerified())
            && user.getGoogleId() != null
            && !user.getGoogleId().isBlank()
            && adminEmails.contains(normalizeEmail(user.getEmail()));
    }

    private GoogleIdToken verifyGoogleCredential(String credential) {
        try {
            return googleTokenVerifier.verify(credential);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Não foi possível validar o token do Google.",
                exception
            );
        }
    }

    private User findOrCreateGoogleUser(
            String googleId,
            String email,
            String name,
            String picture) {
        User user = userService.findByGoogleId(googleId);
        if (user == null) {
            user = userService.findByEmail(email);
        }

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setGoogleId(googleId);
            user.setGoogleEmailVerified(true);
            user.setProfilePicture(profilePictureStorage.importGooglePicture(picture));
            return userService.create(user);
        }

        boolean changed = false;
        boolean googleAccountLinked = !googleId.equals(user.getGoogleId());
        if (googleAccountLinked) {
            user.setGoogleId(googleId);
            changed = true;
        }
        if (!email.equals(normalizeEmail(user.getEmail()))) {
            user.setEmail(email);
            changed = true;
        }
        if (!Boolean.TRUE.equals(user.getGoogleEmailVerified())) {
            user.setGoogleEmailVerified(true);
            changed = true;
        }
        if ((user.getName() == null || user.getName().isBlank()) && !name.isBlank()) {
            user.setName(name);
            changed = true;
        }

        User updatedUser = changed ? userService.update(user) : user;
        boolean pictureNeedsImport = googleAccountLinked
            || user.getProfilePicture() == null
            || user.getProfilePicture().isBlank()
            || profilePictureStorage.isGooglePictureUrl(user.getProfilePicture());
        if (pictureNeedsImport && picture != null && !picture.isBlank()) {
            String storedPicture = profilePictureStorage.importGooglePicture(picture);
            if (storedPicture != null) {
                updatedUser = userService.updateProfilePicture(
                    updatedUser.getId(), storedPicture);
            }
        }
        return updatedUser;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String stringClaim(Payload payload, String claimName) {
        Object claim = payload.get(claimName);
        return claim instanceof String value ? value : null;
    }

    private static ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    public record LoginResult(User user, String profileType) {
    }
}
