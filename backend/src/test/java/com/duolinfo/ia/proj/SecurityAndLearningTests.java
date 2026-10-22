package com.duolinfo.ia.proj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.config.GoogleTokenVerifier;
import com.duolinfo.ia.proj.config.ProductionSecurityValidator;
import com.duolinfo.ia.proj.dto.request.AiExerciseGenerateRequestDTO;
import com.duolinfo.ia.proj.dto.request.ExerciseQuestionRequestDTO;
import com.duolinfo.ia.proj.dto.request.ForgotPasswordRequestDTO;
import com.duolinfo.ia.proj.dto.request.ResetPasswordRequestDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentProgressSummaryResponseDTO;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.PasswordResetToken;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.PasswordResetTokenRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.AchievementService;
import com.duolinfo.ia.proj.service.AiExerciseGeneratorService;
import com.duolinfo.ia.proj.service.AuthService;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.LearningExerciseService;
import com.duolinfo.ia.proj.service.PasswordResetService;
import com.duolinfo.ia.proj.service.ProfilePictureStorageService;
import com.duolinfo.ia.proj.service.StudentService;
import com.duolinfo.ia.proj.service.TeacherService;
import com.duolinfo.ia.proj.service.UserService;

import tools.jackson.databind.ObjectMapper;

class SecurityAndLearningTests {

    private PasswordEncoder passwordEncoder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        objectMapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void googleAccountWithoutPasswordCannotSetPasswordViaLogin() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                mock(GoogleTokenVerifier.class),
                mock(JwtService.class),
                mock(UserService.class),
                mock(StudentService.class),
                mock(TeacherService.class),
                mock(ProfilePictureStorageService.class),
                ""
        );

        User googleOnlyUser = new User();
        googleOnlyUser.setId(42L);
        googleOnlyUser.setEmail("googleuser@example.com");
        googleOnlyUser.setGoogleId("google-sub-123");
        googleOnlyUser.setPasswordHash(null);

        when(userRepository.findByEmailIgnoreCase("googleuser@example.com"))
                .thenReturn(Optional.of(googleOnlyUser));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate("googleuser@example.com", "attackerPassword123")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertNull(googleOnlyUser.getPasswordHash());
        verify(userRepository, never()).save(any());
    }

    @Test
    void plaintextPasswordInDatabaseIsRejectedWithoutBcryptHash() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                mock(GoogleTokenVerifier.class),
                mock(JwtService.class),
                mock(UserService.class),
                mock(StudentService.class),
                mock(TeacherService.class),
                mock(ProfilePictureStorageService.class),
                ""
        );

        User legacyPlaintextUser = new User();
        legacyPlaintextUser.setId(50L);
        legacyPlaintextUser.setEmail("legacy@example.com");
        legacyPlaintextUser.setPasswordHash("123456");

        when(userRepository.findByEmailIgnoreCase("legacy@example.com"))
                .thenReturn(Optional.of(legacyPlaintextUser));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate("legacy@example.com", "123456")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void productionSecurityValidatorFailsFastOnDefaultOrWeakSecrets() {
        // Default dev JWT secret must fail in production
        assertThrows(
                IllegalStateException.class,
                () -> ProductionSecurityValidator.validateProductionSettings(
                        ProductionSecurityValidator.LOCAL_DEV_JWT_SECRET,
                        "StrongDbPass#2026!",
                        "admin@instituicao.edu.br",
                        "StrongAdminPass#2026!"
                )
        );

        // Default DB password must fail in production
        assertThrows(
                IllegalStateException.class,
                () -> ProductionSecurityValidator.validateProductionSettings(
                        "9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e",
                        ProductionSecurityValidator.LOCAL_DEV_DB_PASSWORD,
                        "admin@instituicao.edu.br",
                        "StrongAdminPass#2026!"
                )
        );

        // Weak default admin password ("duadmin") must fail in production
        assertThrows(
                IllegalStateException.class,
                () -> ProductionSecurityValidator.validateProductionSettings(
                        "9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e",
                        "StrongDbPass#2026!",
                        "admin@instituicao.edu.br",
                        "duadmin"
                )
        );

        // Strong settings succeed
        ProductionSecurityValidator.validateProductionSettings(
                "9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e",
                "StrongDbPass#2026!",
                "admin@instituicao.edu.br",
                "StrongAdminPass#2026!"
        );
    }

    @Test
    void passwordResetFlowEnforcesSingleUseTokenAndBcryptEncoding() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        PasswordResetService resetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                passwordEncoder
        );

        User user = new User();
        user.setId(77L);
        user.setEmail("student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.countByUser_EmailIgnoreCaseAndCreatedAtAfter(eq("student@example.com"), any()))
                .thenReturn(0L);
        when(tokenRepository.findByUser_EmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc("student@example.com"))
                .thenReturn(List.of());

        Map<String, Object> requestResult = resetService.requestPasswordReset(
                new ForgotPasswordRequestDTO("student@example.com")
        );
        assertNotNull(requestResult.get("message"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getTokenHash());
        assertNull(savedToken.getUsedAt());

        // Simulate verifying with known code "654321"
        savedToken.setTokenHash(PasswordResetService.hashToken("654321"));
        savedToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        when(tokenRepository.findByUser_EmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc("student@example.com"))
                .thenReturn(List.of(savedToken));

        // Wrong code increments attempt count and throws 400
        assertThrows(
                ResponseStatusException.class,
                () -> resetService.resetPassword(new ResetPasswordRequestDTO("student@example.com", "000000", "novaSenha123"))
        );
        assertEquals(1, savedToken.getAttemptCount());
        assertNull(savedToken.getUsedAt());

        // Correct code marks token as used and hashes password with BCrypt
        resetService.resetPassword(new ResetPasswordRequestDTO("student@example.com", "654321", "novaSenha123"));
        assertNotNull(savedToken.getUsedAt());
        assertTrue(passwordEncoder.matches("novaSenha123", user.getPasswordHash()));
    }

    @Test
    void accessControlEnforcesRecordOwnershipAndInstitutionBoundaries() {
        UserRepository userRepository = mock(UserRepository.class);
        StudentRepository studentRepository = mock(StudentRepository.class);
        TeacherRepository teacherRepository = mock(TeacherRepository.class);
        SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
        ClassTeacherAssignmentRepository assignmentRepository = mock(ClassTeacherAssignmentRepository.class);

        AccessControlService accessControl = new AccessControlService(
                userRepository,
                studentRepository,
                teacherRepository,
                schoolClassRepository,
                enrollmentRepository,
                assignmentRepository
        );

        Student studentA = new Student();
        studentA.setId(10L);
        studentA.setEmail("studenta@example.com");

        Student studentB = new Student();
        studentB.setId(20L);
        studentB.setEmail("studentb@example.com");

        when(userRepository.findByEmailIgnoreCase("studenta@example.com")).thenReturn(Optional.of(studentA));
        when(studentRepository.findById(20L)).thenReturn(Optional.of(studentB));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "studenta@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                )
        );

        // Student A can modify self, but cannot modify or read Student B
        accessControl.assertSelfOrSuperAdmin(10L);
        ResponseStatusException forbiddenModify = assertThrows(
                ResponseStatusException.class,
                () -> accessControl.assertSelfOrSuperAdmin(20L)
        );
        assertEquals(HttpStatus.FORBIDDEN, forbiddenModify.getStatusCode());

        ResponseStatusException forbiddenRead = assertThrows(
                ResponseStatusException.class,
                () -> accessControl.assertCanReadStudent(studentB)
        );
        assertEquals(HttpStatus.FORBIDDEN, forbiddenRead.getStatusCode());

        // Teacher from Institution 1 cannot manage class of Institution 2
        Institution inst1 = new Institution();
        inst1.setId(100L);
        Institution inst2 = new Institution();
        inst2.setId(200L);

        Teacher teacher1 = new Teacher();
        teacher1.setId(30L);
        teacher1.setEmail("teacher1@example.com");
        teacher1.setInstitution(inst1);
        teacher1.setApproved(true);

        School school2 = new School();
        school2.setId(201L);
        school2.setInstitution(inst2);

        SchoolClass classInst2 = new SchoolClass();
        classInst2.setId(500L);
        classInst2.setSchool(school2);

        when(userRepository.findByEmailIgnoreCase("teacher1@example.com")).thenReturn(Optional.of(teacher1));
        when(teacherRepository.findById(30L)).thenReturn(Optional.of(teacher1));
        when(assignmentRepository.findByTeacherId(30L)).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "teacher1@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
                )
        );

        ResponseStatusException forbiddenClass = assertThrows(
                ResponseStatusException.class,
                () -> accessControl.assertCanManageSchoolClass(classInst2)
        );
        assertEquals(HttpStatus.FORBIDDEN, forbiddenClass.getStatusCode());
    }

    @Test
    void exerciseResubmissionAwardsOnlyPositiveXpDeltaAndSupportsOpenEndedTeacherReview() {
        LearningExerciseRepository exerciseRepository = mock(LearningExerciseRepository.class);
        ExerciseSubmissionRepository submissionRepository = mock(ExerciseSubmissionRepository.class);
        TeacherRepository teacherRepository = mock(TeacherRepository.class);
        StudentRepository studentRepository = mock(StudentRepository.class);
        SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
        ClassTeacherAssignmentRepository assignmentRepository = mock(ClassTeacherAssignmentRepository.class);
        AchievementService achievementService = mock(AchievementService.class);

        LearningExerciseService service = new LearningExerciseService(
                objectMapper,
                exerciseRepository,
                submissionRepository,
                teacherRepository,
                studentRepository,
                schoolClassRepository,
                enrollmentRepository,
                assignmentRepository,
                achievementService
        );

        Student student = new Student();
        student.setId(1L);
        student.setName("Aluno Teste");
        student.setEmail("aluno@teste.com");
        StudentLanguage english = new StudentLanguage();
        english.setLanguageName("Inglês");
        english.setLevel("Básico");
        english.setScore(0);
        student.addLanguage(english);

        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setName("Prof Teste");
        teacher.setEmail("prof@teste.com");
        teacher.setApproved(true);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(10L);
        schoolClass.setName("Turma A1");

        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setStudent(student);
        activeEnrollment.setSchoolClass(schoolClass);
        activeEnrollment.setStatus(EnrollmentStatus.ACTIVE);

        List<ExerciseQuestionRequestDTO> questions = List.of(
                new ExerciseQuestionRequestDTO("Objetiva", "Say hello", List.of("Hello", "Bye"), "Hello", "Hello"),
                new ExerciseQuestionRequestDTO("Objetiva", "Say thanks", List.of("Thanks", "No"), "Thanks", "Thanks")
        );

        LearningExercise exercise = new LearningExercise();
        exercise.setId(99L);
        exercise.setTitle("Greetings");
        exercise.setLanguageName("Inglês");
        exercise.setProficiencyLevel("Fácil");
        exercise.setSchoolClass(schoolClass);
        exercise.setTeacher(teacher);
        exercise.setActive(true);
        exercise.setQuestionsJson(objectMapper.writeValueAsString(questions));

        List<ExerciseSubmission> storedSubmissions = new ArrayList<>();

        when(studentRepository.findByEmailIgnoreCase("aluno@teste.com")).thenReturn(Optional.of(student));
        when(teacherRepository.findByEmailIgnoreCase("prof@teste.com")).thenReturn(Optional.of(teacher));
        when(exerciseRepository.findById(99L)).thenReturn(Optional.of(exercise));
        when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(activeEnrollment));
        when(submissionRepository.findByStudent_IdAndExercise_IdOrderBySubmittedAtDesc(1L, 99L))
                .thenAnswer(inv -> new ArrayList<>(storedSubmissions));
        when(submissionRepository.findByStudent_IdOrderBySubmittedAtDesc(1L))
                .thenAnswer(inv -> new ArrayList<>(storedSubmissions));
        when(submissionRepository.save(any(ExerciseSubmission.class))).thenAnswer(inv -> {
            ExerciseSubmission s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId((long) (storedSubmissions.size() + 1));
                storedSubmissions.add(0, s);
            }
            return s;
        });

        // 1st attempt: 1 out of 2 correct -> earns 10 XP
        ExerciseSubmissionResponseDTO sub1 = service.submit(99L, List.of("Hello", "No"), "aluno@teste.com");
        assertEquals(1, sub1.correctCount());
        assertEquals(10, sub1.xpEarned());
        assertEquals(10, english.getScore());

        // 2nd attempt with same 1/2 score -> earns 0 XP (no XP farming!)
        ExerciseSubmissionResponseDTO sub2 = service.submit(99L, List.of("Hello", "No"), "aluno@teste.com");
        assertEquals(1, sub2.correctCount());
        assertEquals(0, sub2.xpEarned());
        assertEquals(10, english.getScore());

        // 3rd attempt improving to 2/2 correct -> earns only +10 XP improvement delta (total 20 XP)
        ExerciseSubmissionResponseDTO sub3 = service.submit(99L, List.of("Hello", "Thanks"), "aluno@teste.com");
        assertEquals(2, sub3.correctCount());
        assertEquals(10, sub3.xpEarned());
        assertEquals(20, english.getScore());

        // Progress summary reflects 20 total XP, 20 today XP, 1 completed exercise, 1 day streak
        StudentProgressSummaryResponseDTO progress = service.getStudentProgressSummary("aluno@teste.com", "America/Sao_Paulo");
        assertEquals(20, progress.totalXp());
        assertEquals(20, progress.todayXp());
        assertEquals(1, progress.completedExercisesCount());
        assertEquals(1, progress.currentStreakDays());
        assertTrue(progress.dailyGoalReached());
    }

    @Test
    void aiGeneratorFailsClearlyWithoutApiKeyAndParsesValidJson() {
        AiExerciseGeneratorService unconfiguredService = new AiExerciseGeneratorService(
                objectMapper,
                "",
                "https://generativelanguage.googleapis.com/v1beta/models",
                "gemini-2.5-flash"
        );
        assertFalse(unconfiguredService.isConfigured());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> unconfiguredService.generateQuestions(
                        new AiExerciseGenerateRequestDTO("Inglês", "Fácil", "Gramática", "Verbo To Be", 3, List.of(), "")
                )
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());

        String sampleJson = """
                [
                  {
                    "style": "Objetiva",
                    "question": "Complete: I ___ a student.",
                    "options": ["am", "is", "are", "be"],
                    "correctAnswer": "am",
                    "expectedAnswer": "am"
                  }
                ]
                """;
        List<ExerciseQuestionRequestDTO> parsed = unconfiguredService.parseQuestionsJson(sampleJson);
        assertEquals(1, parsed.size());
        assertEquals("Objetiva", parsed.get(0).style());
        assertEquals("am", parsed.get(0).correctAnswer());
    }
}
