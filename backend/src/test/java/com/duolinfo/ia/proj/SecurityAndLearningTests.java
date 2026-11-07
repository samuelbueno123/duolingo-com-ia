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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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
import com.duolinfo.ia.proj.service.SpacedRepetitionService;
import com.duolinfo.ia.proj.service.AiConversationService;
import com.duolinfo.ia.proj.service.PronunciationService;
import com.duolinfo.ia.proj.service.CommunityReviewService;
import com.duolinfo.ia.proj.service.LearningPathService;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewAnswerDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewItemResponseDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationStartDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationMessageRequestDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationSessionResponseDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationRequestDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationResponseDTO;
import com.duolinfo.ia.proj.dto.community.CommunitySubmissionRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunitySubmissionResponseDTO;
import com.duolinfo.ia.proj.dto.community.CommunityReviewRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunityReviewResponseDTO;
import com.duolinfo.ia.proj.dto.learningpath.DiagnosticAnswerDTO;
import com.duolinfo.ia.proj.dto.learningpath.DiagnosticResultDTO;
import com.duolinfo.ia.proj.dto.learningpath.LearningGoalUpdateDTO;
import com.duolinfo.ia.proj.dto.learningpath.LearningPathDTO;
import com.duolinfo.ia.proj.entity.SpacedReviewItem;
import com.duolinfo.ia.proj.entity.AiConversationSession;
import com.duolinfo.ia.proj.entity.AiConversationMessage;
import com.duolinfo.ia.proj.entity.PronunciationChallenge;
import com.duolinfo.ia.proj.entity.PronunciationAttempt;
import com.duolinfo.ia.proj.entity.CommunitySubmission;
import com.duolinfo.ia.proj.entity.CommunityReview;
import com.duolinfo.ia.proj.entity.StudentLearningPath;
import com.duolinfo.ia.proj.entity.LearningPathModule;
import com.duolinfo.ia.proj.repository.SpacedReviewItemRepository;
import com.duolinfo.ia.proj.repository.AiConversationSessionRepository;
import com.duolinfo.ia.proj.repository.AiConversationMessageRepository;
import com.duolinfo.ia.proj.repository.PronunciationChallengeRepository;
import com.duolinfo.ia.proj.repository.PronunciationAttemptRepository;
import com.duolinfo.ia.proj.repository.CommunitySubmissionRepository;
import com.duolinfo.ia.proj.repository.CommunityReviewRepository;
import com.duolinfo.ia.proj.repository.CommunityReportRepository;
import com.duolinfo.ia.proj.repository.StudentLearningPathRepository;
import com.duolinfo.ia.proj.repository.LearningPathModuleRepository;
import com.duolinfo.ia.proj.repository.StudentModuleProgressRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;

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
        User user = new User();
        user.setId(77L);
        user.setEmail("student@example.com");
        JwtService jwtService = spy(new JwtService("0123456789abcdef0123456789abcdef"));
        String rawJwtToken = jwtService.generatePasswordResetToken(user);
        doReturn(rawJwtToken).when(jwtService).generatePasswordResetToken(user);
        PasswordResetService resetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                jwtService
        );

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
        savedToken.setId(88L);
        assertEquals(PasswordResetService.hashToken(rawJwtToken), savedToken.getTokenHash());
        assertNull(savedToken.getUsedAt());
        assertTrue(jwtService.isPasswordResetTokenValid(rawJwtToken));
        assertEquals("student@example.com", jwtService.getEmailFromPasswordResetToken(rawJwtToken));
        assertEquals(Optional.of(77L), jwtService.getUserIdFromPasswordResetToken(rawJwtToken));
        assertFalse(jwtService.isPasswordResetTokenValid(jwtService.generateToken(user)));

        savedToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        when(tokenRepository.findFirstByUser_IdAndTokenHashAndUsedAtIsNullOrderByCreatedAtDesc(
                77L, PasswordResetService.hashToken(rawJwtToken)))
                .thenReturn(Optional.of(savedToken), Optional.empty());
        doAnswer(invocation -> {
            savedToken.setUsedAt(invocation.getArgument(1));
            return 1;
        }).when(tokenRepository).markUsedIfUnusedAndNotExpired(eq(savedToken.getId()), any());

        // An invalid JWT is rejected without consuming the valid token.
        assertThrows(
                ResponseStatusException.class,
                () -> resetService.resetPassword(new ResetPasswordRequestDTO("student@example.com", "invalid.jwt.token", "novaSenha123"))
        );
        assertNull(savedToken.getUsedAt());

        // The valid JWT marks the record as used and hashes the password with BCrypt.
        resetService.resetPassword(new ResetPasswordRequestDTO(rawJwtToken, "novaSenha123"));
        verify(tokenRepository).markUsedIfUnusedAndNotExpired(eq(savedToken.getId()), any());
        assertNotNull(savedToken.getUsedAt());
        assertTrue(passwordEncoder.matches("novaSenha123", user.getPasswordHash()));
        assertThrows(
                ResponseStatusException.class,
                () -> resetService.resetPassword(new ResetPasswordRequestDTO(rawJwtToken, "outraSenha123"))
        );
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

    @Test
    void accessControlBlocksCrossInstitutionSchoolAndClassManagement() {
        UserRepository userRepository = mock(UserRepository.class);
        TeacherRepository teacherRepository = mock(TeacherRepository.class);
        StudentRepository studentRepository = mock(StudentRepository.class);
        SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
        ClassTeacherAssignmentRepository assignmentRepository = mock(ClassTeacherAssignmentRepository.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);

        Institution inst1 = new Institution(); inst1.setId(10L);
        Institution inst2 = new Institution(); inst2.setId(20L);

        Teacher teacherInst1 = new Teacher();
        teacherInst1.setId(1L);
        teacherInst1.setEmail("prof@inst1.com");
        teacherInst1.setInstitution(inst1);
        teacherInst1.setInstitutionAdmin(false);

        School schoolInst2 = new School();
        schoolInst2.setId(200L);
        schoolInst2.setInstitution(inst2);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("prof@inst1.com", null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
        );

        when(userRepository.findByEmailIgnoreCase("prof@inst1.com")).thenReturn(Optional.of(teacherInst1));
        when(teacherRepository.findByEmailIgnoreCase("prof@inst1.com")).thenReturn(Optional.of(teacherInst1));
        when(schoolRepository.findById(200L)).thenReturn(Optional.of(schoolInst2));

        AccessControlService acs = new AccessControlService(
                userRepository,
                studentRepository,
                teacherRepository,
                schoolClassRepository,
                enrollmentRepository,
                assignmentRepository,
                schoolRepository
        );

        // Teacher from institution 1 cannot manage school from institution 2 -> 403 FORBIDDEN
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                acs.assertCanManageSchool(200L)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void spacedRepetitionCalculatesSm2IntervalsCorrectly() {
        SpacedReviewItemRepository itemRepository = mock(SpacedReviewItemRepository.class);
        StudentRepository studentRepository = mock(StudentRepository.class);

        Student student = new Student();
        student.setId(5L);
        student.setEmail("aluno@teste.com");

        SpacedReviewItem item = SpacedReviewItem.builder()
                .id(100L)
                .student(student)
                .languageName("Inglês")
                .term("to look forward to")
                .translation("esperar ansiosamente por")
                .repetitionNumber(0)
                .intervalDays(1)
                .easeFactor(2.5)
                .lapseCount(0)
                .dueDate(java.time.LocalDate.now())
                .build();

        when(studentRepository.findByEmailIgnoreCase("aluno@teste.com")).thenReturn(Optional.of(student));
        when(itemRepository.findByIdAndStudent_Id(100L, 5L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(SpacedReviewItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpacedRepetitionService srs = new SpacedRepetitionService(itemRepository, studentRepository);

        // 1st review with quality 5 (Perfect)
        SpacedReviewItemResponseDTO r1 = srs.reviewItem("aluno@teste.com", 100L, new SpacedReviewAnswerDTO(5));
        assertEquals(1, r1.repetitionNumber());
        assertEquals(1, r1.intervalDays());
        assertTrue(r1.easeFactor() >= 2.5);

        // 2nd review with quality 4 (Good)
        SpacedReviewItemResponseDTO r2 = srs.reviewItem("aluno@teste.com", 100L, new SpacedReviewAnswerDTO(4));
        assertEquals(2, r2.repetitionNumber());
        assertEquals(6, r2.intervalDays());

        // 3rd review with quality 0 (Blackout) -> resets repetition number and interval
        SpacedReviewItemResponseDTO r3 = srs.reviewItem("aluno@teste.com", 100L, new SpacedReviewAnswerDTO(0));
        assertEquals(0, r3.repetitionNumber());
        assertEquals(1, r3.intervalDays());
        assertEquals(1, r3.lapseCount());
    }

    @Test
    void aiConversationDetectsGrammarMistakesAndProvidesCorrection() {
        AiConversationSessionRepository sessionRepo = mock(AiConversationSessionRepository.class);
        AiConversationMessageRepository messageRepo = mock(AiConversationMessageRepository.class);
        StudentRepository studentRepo = mock(StudentRepository.class);
        StudentLanguageRepository slRepo = mock(StudentLanguageRepository.class);

        Student student = new Student();
        student.setId(10L);
        student.setEmail("user@test.com");

        AiConversationSession session = AiConversationSession.builder()
                .id(1L)
                .studentId(10L)
                .scenarioCode("coffee_shop")
                .scenarioTitle("Café da Manhã")
                .targetLanguage("Inglês")
                .cefrLevel("A1")
                .personaRole("Barista")
                .active(true)
                .totalTurns(0)
                .grammarCorrectionsCount(0)
                .build();

        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(messageRepo.save(any())).thenAnswer(i -> {
            AiConversationMessage m = i.getArgument(0);
            m.setId(99L);
            return m;
        });
        when(messageRepo.findBySessionIdOrderByTimestampAsc(1L)).thenReturn(List.of());

        AiConversationService service = new AiConversationService(
                sessionRepo, messageRepo, studentRepo, slRepo, objectMapper,
                "", "", "" // unconfigured API key tests robust intelligent fallback engine
        );

        // Student says "i have 20 years" (classic Portuguese interference mistake)
        AiConversationSessionResponseDTO response = service.sendMessage(
                10L, 1L, new AiConversationMessageRequestDTO("i have 20 years", null)
        );

        ArgumentCaptor<AiConversationMessage> captor = ArgumentCaptor.forClass(AiConversationMessage.class);
        verify(messageRepo, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        List<AiConversationMessage> savedMessages = captor.getAllValues();
        AiConversationMessage userSaved = savedMessages.stream().filter(m -> "USER".equals(m.getSender())).findFirst().orElseThrow();

        assertTrue(userSaved.getHasGrammarIssues());
        assertNotNull(userSaved.getGrammarCorrection());
        assertTrue(userSaved.getGrammarCorrection().contains("years old"));
        assertTrue(userSaved.getExplanation().contains("to be"));
        assertEquals(1, session.getGrammarCorrectionsCount());
    }

    @Test
    void pronunciationServiceEvaluatesSpeechPhonetically() {
        PronunciationChallengeRepository challengeRepo = mock(PronunciationChallengeRepository.class);
        PronunciationAttemptRepository attemptRepo = mock(PronunciationAttemptRepository.class);
        StudentRepository studentRepo = mock(StudentRepository.class);
        StudentLanguageRepository slRepo = mock(StudentLanguageRepository.class);

        PronunciationChallenge challenge = PronunciationChallenge.builder()
                .id(1L)
                .language("Inglês")
                .cefrLevel("A1")
                .category("Cumprimentos")
                .phraseText("Good morning, nice to meet you!")
                .phoneticTips("Ligue as palavras com suavidade.")
                .build();

        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challenge));
        when(attemptRepo.save(any())).thenAnswer(i -> {
            PronunciationAttempt a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        PronunciationService service = new PronunciationService(challengeRepo, attemptRepo, studentRepo, slRepo, objectMapper);

        // Test near-perfect speech
        PronunciationEvaluationResponseDTO result = service.evaluateAttempt(
                5L, new PronunciationEvaluationRequestDTO(1L, "Good morning nice to meet you")
        );

        assertTrue(result.accuracyScore() >= 95.0);
        assertTrue(result.completenessScore() >= 95.0);
        assertTrue(result.finalScore() >= 90.0);
        assertEquals(15, result.xpEarned());
        assertTrue(result.feedbackSummary().contains("fantástica") || result.feedbackSummary().contains("excelente"));
    }

    @Test
    void communityPeerReviewWorkflowEnforcesIntegrity() {
        CommunitySubmissionRepository subRepo = mock(CommunitySubmissionRepository.class);
        CommunityReviewRepository reviewRepo = mock(CommunityReviewRepository.class);
        CommunityReportRepository reportRepo = mock(CommunityReportRepository.class);
        StudentRepository studentRepo = mock(StudentRepository.class);
        StudentLanguageRepository slRepo = mock(StudentLanguageRepository.class);

        Student author = new Student();
        author.setId(1L);
        author.setName("Autor");

        Student reviewer = new Student();
        reviewer.setId(2L);
        reviewer.setName("Revisor Fluente");

        CommunitySubmission sub = CommunitySubmission.builder()
                .id(50L)
                .authorStudentId(1L)
                .authorName("Autor")
                .language("Inglês")
                .promptTitle("Meu café da manhã")
                .content("I eat bread and coffee.")
                .reviewCount(0)
                .status("PENDING_REVIEW")
                .build();

        when(subRepo.findById(50L)).thenReturn(Optional.of(sub));
        when(studentRepo.findById(1L)).thenReturn(Optional.of(author));
        when(studentRepo.findById(2L)).thenReturn(Optional.of(reviewer));
        when(reviewRepo.existsBySubmissionIdAndReviewerStudentId(50L, 2L)).thenReturn(false);
        when(reviewRepo.save(any())).thenAnswer(i -> {
            CommunityReview r = i.getArgument(0);
            r.setId(500L);
            return r;
        });

        CommunityReviewService service = new CommunityReviewService(subRepo, reviewRepo, reportRepo, studentRepo, slRepo);

        // Author cannot review own submission -> 400 Bad Request
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.addReview(1L, new CommunityReviewRequestDTO(50L, "Muito bom", "I have bread and coffee", "Use have", 5))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        // Peer submits review
        CommunityReviewResponseDTO review = service.addReview(
                2L, new CommunityReviewRequestDTO(50L, "Boa frase!", "I usually have bread and coffee for breakfast.", "Nativos dizem 'have breakfast'", 5)
        );
        assertNotNull(review);
        assertEquals(1, sub.getReviewCount());
        assertEquals("REVIEWED", sub.getStatus());
    }

    @Test
    void learningPathDiagnosticScoresCorrectlyAndUnlocksModules() {
        StudentLearningPathRepository pathRepo = mock(StudentLearningPathRepository.class);
        LearningPathModuleRepository moduleRepo = mock(LearningPathModuleRepository.class);
        StudentModuleProgressRepository progressRepo = mock(StudentModuleProgressRepository.class);
        StudentRepository studentRepo = mock(StudentRepository.class);
        StudentLanguageRepository slRepo = mock(StudentLanguageRepository.class);

        when(pathRepo.findByStudentIdAndLanguageIgnoreCase(7L, "Inglês")).thenReturn(Optional.empty());
        when(pathRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        LearningPathModule m1 = LearningPathModule.builder().id(1L).language("Inglês").cefrLevel("A1").moduleOrder(1).title("M1").build();
        LearningPathModule m2 = LearningPathModule.builder().id(2L).language("Inglês").cefrLevel("A2").moduleOrder(2).title("M2").build();
        LearningPathModule m3 = LearningPathModule.builder().id(3L).language("Inglês").cefrLevel("B1").moduleOrder(3).title("M3").build();
        LearningPathModule m4 = LearningPathModule.builder().id(4L).language("Inglês").cefrLevel("B2").moduleOrder(4).title("M4").build();

        when(moduleRepo.findByLanguageIgnoreCaseOrderByModuleOrderAsc("Inglês")).thenReturn(List.of(m1, m2, m3, m4));
        when(progressRepo.findByStudentIdAndModuleId(eq(7L), any())).thenReturn(Optional.empty());

        LearningPathService service = new LearningPathService(pathRepo, moduleRepo, progressRepo, studentRepo, slRepo);

        // Answer 8 questions correctly (100%) -> should determine level B2
        Map<Integer, Integer> answers = Map.of(
                1, 1,
                2, 0,
                3, 1,
                4, 1,
                5, 1,
                6, 1,
                7, 0,
                8, 1
        );

        DiagnosticResultDTO result = service.submitDiagnostic(7L, new DiagnosticAnswerDTO("Inglês", answers));
        assertEquals("B2", result.determinedCefrLevel());
        assertEquals(8, result.correctCount());
        assertEquals(100.0, result.scorePercentage());
        assertTrue(result.diagnosticSummary().contains("B2"));
    }
}
