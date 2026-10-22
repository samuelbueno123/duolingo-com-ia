package com.duolinfo.ia.proj.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.DeleteAccountRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserPreferencesRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserRequestDTO;
import com.duolinfo.ia.proj.dto.response.UserPreferencesResponseDTO;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfilePictureStorageService profilePictureStorage;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private EntityManager entityManager;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProfilePictureStorageService profilePictureStorage) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profilePictureStorage = profilePictureStorage;
    }

    @Transactional
    public User create(User user) {
        String email = normalizeEmail(user.getEmail());
        if (email == null) {
            throw badRequest("O email é obrigatório.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw badRequest("O nome é obrigatório.");
        }
        ensureEmailAvailable(email, null);

        user.setName(user.getName().trim());
        user.setEmail(email);
        user.setGender(User.normalizeGender(user.getGender()));
        encodePassword(user);

        String storedPicture = profilePictureStorage.store(user.getProfilePicture());
        user.setProfilePicture(storedPicture);
        try {
            User savedUser = userRepository.save(user);
            schedulePictureCleanup(null, storedPicture);
            return savedUser;
        } catch (RuntimeException exception) {
            profilePictureStorage.delete(storedPicture);
            throw exception;
        }
    }

    @Transactional
    public User update(User user) {
        return update(user, null, false);
    }

    private User update(
            User user,
            String pictureBeforeUpdate,
            boolean pictureBeforeUpdateProvided) {
        if (user.getId() == null) {
            throw badRequest("O identificador do usuário é obrigatório.");
        }

        User existingUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuário não encontrado."));

        if (user.getName() != null && !user.getName().isBlank()) {
            existingUser.setName(user.getName().trim());
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String email = normalizeEmail(user.getEmail());
            if (existingUser.isSuperAdmin() && !"admin@duolingo.com".equalsIgnoreCase(email)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O email da conta geral de administrador (admin@duolingo.com) não pode ser alterado.");
            }
            ensureEmailAvailable(email, existingUser.getId());
            existingUser.setEmail(email);
        }

        if (user.getGender() != null && !user.getGender().isBlank()) {
            existingUser.setGender(User.normalizeGender(user.getGender()));
        }

        if (user.getGoogleId() != null && !user.getGoogleId().isBlank()) {
            existingUser.setGoogleId(user.getGoogleId().trim());
        }
        if (Boolean.TRUE.equals(user.getGoogleEmailVerified())) {
            existingUser.setGoogleEmailVerified(true);
        }
        if (existingUser.isSuperAdmin()) {
            existingUser.setAdmin(true);
        } else {
            existingUser.setAdmin(user.isAdmin());
        }

        if (user.getUiLanguage() != null) {
            existingUser.setUiLanguage(normalizeUiLanguage(user.getUiLanguage()));
        }
        if (user.getThemeMode() != null) {
            existingUser.setThemeMode(normalizeThemeMode(user.getThemeMode()));
        }
        if (user.getDailyGoalMinutes() != null) {
            existingUser.setDailyGoalMinutes(normalizeDailyGoalMinutes(user.getDailyGoalMinutes()));
        }
        if (user.getNotifDailyReminders() != null) {
            existingUser.setNotifDailyReminders(user.getNotifDailyReminders());
        }
        if (user.getNotifAchievements() != null) {
            existingUser.setNotifAchievements(user.getNotifAchievements());
        }
        if (user.getNotifRanking() != null) {
            existingUser.setNotifRanking(user.getNotifRanking());
        }
        if (user.getNotifClassUpdates() != null) {
            existingUser.setNotifClassUpdates(user.getNotifClassUpdates());
        }

        String previousPicture = pictureBeforeUpdateProvided
            ? pictureBeforeUpdate
            : existingUser.getProfilePicture();
        String requestedPicture = user.getProfilePicture();
        if (requestedPicture != null) {
            existingUser.setProfilePicture(profilePictureStorage.store(requestedPicture));
        } else if (profilePictureStorage.isInlinePicture(previousPicture)) {
            existingUser.setProfilePicture(profilePictureStorage.store(previousPicture));
        }

        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            if (!isEncodedPassword(user.getPasswordHash())) {
                existingUser.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            }
        }

        return saveWithPictureCleanup(existingUser, previousPicture);
    }

    @Transactional
    public User updateProfile(Long userId, UserRequestDTO request) {
        if (request == null) {
            throw badRequest("Os dados do usuário não foram informados.");
        }

        User user = findById(userId);
        String previousPicture = user.getProfilePicture();
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.gender() != null && !request.gender().isBlank()) {
            user.setGender(User.normalizeGender(request.gender()));
        }

        if (request.email() != null && !request.email().isBlank()) {
            String email = normalizeEmail(request.email());
            boolean googleAccount = user.getGoogleId() != null
                && !user.getGoogleId().isBlank();
            if (googleAccount && !email.equals(normalizeEmail(user.getEmail()))) {
                throw badRequest("O email de uma conta Google não pode ser alterado por aqui.");
            }
            user.setEmail(email);
        }

        if (request.profilePicture() != null) {
            if (request.profilePicture().isBlank()) {
                throw badRequest("A foto de perfil não pode estar vazia.");
            }
            user.setProfilePicture(request.profilePicture());
        }

        if (request.password() != null && !request.password().isBlank()) {
            String currentPassword = request.currentPassword();
            String currentHash = user.getPasswordHash();
            if (currentHash != null && !currentHash.isBlank()) {
                if (currentPassword == null
                        || currentPassword.isBlank()
                        || !passwordEncoder.matches(currentPassword, currentHash)) {
                    throw badRequest("A senha atual informada está incorreta.");
                }
            }
            user.setPasswordHash(request.password());
        }

        return update(user, previousPicture, true);
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponseDTO getPreferences(Long userId) {
        User user = findById(userId);
        return toPreferencesResponse(user);
    }

    @Transactional
    public UserPreferencesResponseDTO updatePreferences(Long userId, UserPreferencesRequestDTO dto) {
        if (dto == null) {
            throw badRequest("As preferências não foram informadas.");
        }
        User user = findById(userId);
        if (dto.uiLanguage() != null && !dto.uiLanguage().isBlank()) {
            user.setUiLanguage(normalizeUiLanguage(dto.uiLanguage()));
        }
        if (dto.themeMode() != null && !dto.themeMode().isBlank()) {
            user.setThemeMode(normalizeThemeMode(dto.themeMode()));
        }
        if (dto.dailyGoalMinutes() != null) {
            user.setDailyGoalMinutes(normalizeDailyGoalMinutes(dto.dailyGoalMinutes()));
        }
        if (dto.notifDailyReminders() != null) {
            user.setNotifDailyReminders(dto.notifDailyReminders());
        }
        if (dto.notifAchievements() != null) {
            user.setNotifAchievements(dto.notifAchievements());
        }
        if (dto.notifRanking() != null) {
            user.setNotifRanking(dto.notifRanking());
        }
        if (dto.notifClassUpdates() != null) {
            user.setNotifClassUpdates(dto.notifClassUpdates());
        }
        User saved = userRepository.save(user);
        return toPreferencesResponse(saved);
    }

    public UserPreferencesResponseDTO toPreferencesResponse(User user) {
        boolean externalConfigured = smtpHost != null && !smtpHost.isBlank();
        String notice = externalConfigured
                ? "Notificações externas ativas."
                : "Suas preferências foram salvas na conta. O envio externo de e-mail ou push depende da configuração de credenciais SMTP/FCM no ambiente.";
        int goalMinutes = user.getEffectiveDailyGoalMinutes();
        return new UserPreferencesResponseDTO(
                user.getNormalizedUiLanguage(),
                user.getNormalizedThemeMode(),
                goalMinutes,
                goalMinutes * 2,
                user.isEffectiveNotifDailyReminders(),
                user.isEffectiveNotifAchievements(),
                user.isEffectiveNotifRanking(),
                user.isEffectiveNotifClassUpdates(),
                externalConfigured,
                notice
        );
    }

    public static String normalizeUiLanguage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "pt";
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("en") || lower.contains("ingl")) {
            return "en";
        }
        if (lower.startsWith("es") || lower.contains("espan")) {
            return "es";
        }
        return "pt";
    }

    public static String normalizeThemeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "system";
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if ("dark".equals(lower) || "escuro".equals(lower)) {
            return "dark";
        }
        if ("light".equals(lower) || "claro".equals(lower)) {
            return "light";
        }
        return "system";
    }

    public static int normalizeDailyGoalMinutes(Integer minutes) {
        if (minutes == null) {
            return 15;
        }
        if (minutes < 5 || minutes > 180) {
            throw badRequest("A meta diária deve estar entre 5 e 180 minutos.");
        }
        return minutes;
    }

    @Transactional
    public User updateProfilePicture(Long userId, String profilePicture) {
        if (profilePicture == null || profilePicture.isBlank()) {
            throw badRequest("A foto de perfil não pode estar vazia.");
        }

        User user = findById(userId);
        String previousPicture = user.getProfilePicture();
        user.setProfilePicture(profilePicture);
        return update(user, previousPicture, true);
    }

    @Transactional
    public void deleteOwnAccount(Long userId, DeleteAccountRequestDTO dto) {
        User user = findById(userId);
        if (user.isSuperAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A conta geral de administrador (admin@duolingo.com) não pode ser excluída.");
        }

        String passwordHash = user.getPasswordHash();
        boolean hasPassword = passwordHash != null && !passwordHash.isBlank();
        if (hasPassword) {
            String providedPassword = dto != null ? dto.currentPassword() : null;
            if (providedPassword == null
                    || providedPassword.isBlank()
                    || !passwordEncoder.matches(providedPassword, passwordHash)) {
                throw badRequest("Informe sua senha atual correta para confirmar a exclusão da conta.");
            }
        } else {
            String confirmation = dto != null && dto.confirmationText() != null
                    ? dto.confirmationText().trim()
                    : "";
            boolean validConfirmation = "EXCLUIR".equalsIgnoreCase(confirmation)
                    || "DELETE".equalsIgnoreCase(confirmation)
                    || (user.getEmail() != null && user.getEmail().equalsIgnoreCase(confirmation));
            if (!validConfirmation) {
                throw badRequest("Para confirmar a exclusão desta conta Google sem senha local, digite EXCLUIR ou informe seu e-mail.");
            }
        }

        deleteById(userId);
    }

    @Transactional
    public void delete(User user) {
        if (user != null && user.getId() != null) {
            deleteById(user.getId());
        }
    }

    @Transactional
    public void deleteById(Long id) {
        User user = findById(id);
        if (user.isSuperAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A conta geral de administrador (admin@duolingo.com) não pode ser excluída.");
        }
        String oldPicture = user.getProfilePicture();
        cleanupUserReferences(id);
        if (entityManager != null) {
            entityManager.flush();
            entityManager.clear();
        }
        userRepository.findById(id).ifPresent(userRepository::delete);
        schedulePictureCleanup(oldPicture, null);
    }

    private void cleanupUserReferences(Long userId) {
        if (jdbcTemplate == null || userId == null) {
            return;
        }
        String[] cleanupSql = new String[] {
            "DELETE FROM password_reset_tokens WHERE user_id = ?",
            "DELETE FROM exercise_submissions WHERE student_id = ?",
            "DELETE FROM exercise_submissions WHERE exercise_id IN (SELECT id FROM learning_exercises WHERE created_by_teacher_id = ?)",
            "DELETE FROM learning_exercise_questions WHERE exercise_id IN (SELECT id FROM learning_exercises WHERE created_by_teacher_id = ?)",
            "DELETE FROM learning_exercises WHERE created_by_teacher_id = ?",
            "DELETE FROM student_achievements WHERE student_id = ?",
            "UPDATE student_achievements SET granted_by_user_id = NULL WHERE granted_by_user_id = ?",
            "DELETE FROM student_languages WHERE student_id = ?",
            "DELETE FROM enrollments WHERE student_id = ?",
            "DELETE FROM class_teacher_assignments WHERE teacher_id = ?",
            "DELETE FROM teacher_documents WHERE teacher_id = ?",
            "DELETE FROM teacher_taught_languages WHERE teacher_id = ?",
            "DELETE FROM teacher_specialization_areas WHERE teacher_id = ?",
            "UPDATE admin_notifications SET related_user_id = NULL WHERE related_user_id = ?",
            "DELETE FROM institution_requests WHERE requested_by_teacher_id = ?"
        };
        for (String sql : cleanupSql) {
            try {
                jdbcTemplate.update(sql, userId);
            } catch (Exception ignored) {
                // Table may not exist in minimal unit-test schemas
            }
        }
    }

    @Transactional
    public void deleteByGoogleId(String googleId) {
        userRepository.findByGoogleId(googleId)
            .ifPresent(user -> {
                userRepository.delete(user);
                schedulePictureCleanup(user.getProfilePicture(), null);
            });
    }

    @Transactional
    public void deleteByEmail(String email) {
        User user = findByEmail(email);
        if (user != null) {
            userRepository.delete(user);
            schedulePictureCleanup(user.getProfilePicture(), null);
        }
    }

    @Transactional
    public List<User> findAll() {
        return userRepository.findAll().stream()
            .map(this::migrateLegacyProfilePicture)
            .toList();
    }

    @Transactional
    public List<User> findAllById(Iterable<Long> ids) {
        return userRepository.findAllById(ids).stream()
            .map(this::migrateLegacyProfilePicture)
            .toList();
    }

    @Transactional
    public User findById(Long id) {
        if (id == null) {
            throw badRequest("O identificador do usuário é obrigatório.");
        }
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuário não encontrado com o ID: " + id));
        return migrateLegacyProfilePicture(user);
    }

    @Transactional
    public User findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId)
            .map(this::migrateLegacyProfilePicture)
            .orElse(null);
    }

    @Transactional
    public User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
            .map(this::migrateLegacyProfilePicture)
            .orElse(null);
    }

    @Transactional
    public List<User> findByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        String searchTerm = name.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
            .filter(user -> user.getName() != null
                && user.getName().toLowerCase(Locale.ROOT).contains(searchTerm))
            .map(this::migrateLegacyProfilePicture)
            .toList();
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public boolean existsByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId).isPresent();
    }

    public boolean existsByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return normalizedEmail != null
            && userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent();
    }

    public boolean existsByName(String name) {
        return name != null && userRepository.findAll().stream()
            .anyMatch(user -> user.getName() != null
                && user.getName().equalsIgnoreCase(name.trim()));
    }

    private User migrateLegacyProfilePicture(User user) {
        String oldPicture = user.getProfilePicture();
        if (!profilePictureStorage.isInlinePicture(oldPicture)) {
            return user;
        }

        try {
            user.setProfilePicture(profilePictureStorage.store(oldPicture));
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() != HttpStatus.BAD_REQUEST) {
                LOGGER.warn("A migração da foto do usuário {} será tentada novamente.",
                    user.getId(), exception);
                return user;
            }
            LOGGER.warn("Uma foto antiga inválida foi removida do perfil do usuário {}.",
                user.getId());
            user.setProfilePicture(null);
        }
        return saveWithPictureCleanup(user, oldPicture);
    }

    private User saveWithPictureCleanup(User user, String previousPicture) {
        String currentPicture = user.getProfilePicture();
        try {
            User savedUser = userRepository.save(user);
            schedulePictureCleanup(previousPicture, currentPicture);
            return savedUser;
        } catch (RuntimeException exception) {
            if (!Objects.equals(previousPicture, currentPicture)) {
                profilePictureStorage.delete(currentPicture);
            }
            throw exception;
        }
    }

    private void schedulePictureCleanup(String oldPicture, String newPicture) {
        if (Objects.equals(oldPicture, newPicture)) {
            return;
        }

        boolean transactionActive = TransactionSynchronizationManager
            .isSynchronizationActive();
        if (!transactionActive) {
            profilePictureStorage.delete(oldPicture);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    profilePictureStorage.delete(oldPicture);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        profilePictureStorage.delete(newPicture);
                    }
                }
            });
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        if (email == null) {
            throw badRequest("O email é obrigatório.");
        }

        boolean usedByAnotherUser = userRepository.findByEmailIgnoreCase(email)
            .filter(user -> !Objects.equals(user.getId(), currentUserId))
            .isPresent();
        if (usedByAnotherUser) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Este email já está cadastrado.");
        }
    }

    private void encodePassword(User user) {
        String password = user.getPasswordHash();
        if (password != null && !password.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isEncodedPassword(String password) {
        return password.startsWith("$2a$")
            || password.startsWith("$2b$")
            || password.startsWith("$2y$");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
