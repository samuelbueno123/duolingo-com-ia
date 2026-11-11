package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.dto.community.CommunityReviewRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunityReviewResponseDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationRequestDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationResponseDTO;
import com.duolinfo.ia.proj.entity.CommunityReview;
import com.duolinfo.ia.proj.entity.CommunitySubmission;
import com.duolinfo.ia.proj.entity.PronunciationAttempt;
import com.duolinfo.ia.proj.entity.PronunciationChallenge;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.repository.CommunityReportRepository;
import com.duolinfo.ia.proj.repository.CommunityReviewRepository;
import com.duolinfo.ia.proj.repository.CommunitySubmissionRepository;
import com.duolinfo.ia.proj.repository.PronunciationAttemptRepository;
import com.duolinfo.ia.proj.repository.PronunciationChallengeRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

class PronunciationAndCommunityServiceTest {

    private PronunciationChallengeRepository challengeRepository;
    private PronunciationAttemptRepository attemptRepository;
    private StudentRepository studentRepository;
    private StudentLanguageRepository studentLanguageRepository;
    private ObjectMapper objectMapper;
    private PronunciationService pronunciationService;

    private CommunitySubmissionRepository submissionRepository;
    private CommunityReviewRepository reviewRepository;
    private CommunityReportRepository reportRepository;
    private CommunityReviewService communityReviewService;

    @BeforeEach
    void setUp() {
        challengeRepository = mock(PronunciationChallengeRepository.class);
        attemptRepository = mock(PronunciationAttemptRepository.class);
        studentRepository = mock(StudentRepository.class);
        studentLanguageRepository = mock(StudentLanguageRepository.class);
        objectMapper = new ObjectMapper();

        pronunciationService = new PronunciationService(
            challengeRepository,
            attemptRepository,
            studentRepository,
            studentLanguageRepository,
            objectMapper
        );

        submissionRepository = mock(CommunitySubmissionRepository.class);
        reviewRepository = mock(CommunityReviewRepository.class);
        reportRepository = mock(CommunityReportRepository.class);

        communityReviewService = new CommunityReviewService(
            submissionRepository,
            reviewRepository,
            reportRepository,
            studentRepository,
            studentLanguageRepository
        );
    }

    @Test
    void pronunciation_evaluatesPhoneticAccuracyAndAwardsXp() {
        PronunciationChallenge challenge = PronunciationChallenge.builder()
            .id(1L)
            .language("Inglês")
            .phraseText("Good morning, nice to meet you!")
            .difficultyScore(1)
            .build();

        Student student = new Student();
        student.setId(10L);
        student.setName("Lucas");

        StudentLanguage studentLang = new StudentLanguage();
        studentLang.setLanguageName("Inglês");
        studentLang.setScore(50);

        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));
        when(attemptRepository.save(any(PronunciationAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(studentLanguageRepository.findByStudentAndLanguageNameIgnoreCase(student, "Inglês"))
            .thenReturn(Optional.of(studentLang));

        PronunciationEvaluationRequestDTO request = new PronunciationEvaluationRequestDTO(
            1L,
            "Good morning nice to meet you"
        );

        PronunciationEvaluationResponseDTO response = pronunciationService.evaluateAttempt(10L, request);

        assertNotNull(response);
        assertTrue(response.finalScore() >= 80);
        assertTrue(studentLang.getScore() > 50);
        verify(studentLanguageRepository).save(studentLang);
    }

    @Test
    void pronunciation_createsLanguageIfMissingWhenAwardingXp() {
        PronunciationChallenge challenge = PronunciationChallenge.builder()
            .id(2L)
            .language("Alemão")
            .phraseText("Guten Tag, wie geht es dir?")
            .difficultyScore(2)
            .build();

        Student student = new Student();
        student.setId(15L);
        student.setName("Maria");

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(attemptRepository.save(any(PronunciationAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.findById(15L)).thenReturn(Optional.of(student));
        // Missing Alemão in student languages:
        when(studentLanguageRepository.findByStudentAndLanguageNameIgnoreCase(student, "Alemão"))
            .thenReturn(Optional.empty());

        PronunciationEvaluationRequestDTO request = new PronunciationEvaluationRequestDTO(
            2L,
            "Guten Tag wie geht es dir"
        );

        PronunciationEvaluationResponseDTO response = pronunciationService.evaluateAttempt(15L, request);

        assertNotNull(response);
        // Verify missing language is created and saved!
        verify(studentLanguageRepository).save(any(StudentLanguage.class));
    }

    @Test
    void communityReview_submitsReviewAndAwardsXpToReviewer() {
        CommunitySubmission submission = CommunitySubmission.builder()
            .id(100L)
            .authorStudentId(5L)
            .authorName("Aluno")
            .language("Inglês")
            .content("I goes to school yesterday.")
            .reviewCount(0)
            .build();

        Student reviewer = new Student();
        reviewer.setId(20L);
        reviewer.setName("Reviewer");

        StudentLanguage reviewerLang = new StudentLanguage();
        reviewerLang.setLanguageName("Inglês");
        reviewerLang.setScore(100);

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(studentRepository.findById(20L)).thenReturn(Optional.of(reviewer));
        when(reviewRepository.save(any(CommunityReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentLanguageRepository.findByStudentAndLanguageNameIgnoreCase(reviewer, "Inglês"))
            .thenReturn(Optional.of(reviewerLang));

        CommunityReviewRequestDTO request = new CommunityReviewRequestDTO(
            100L,
            "Ótima tentativa!",
            "I went to school yesterday.",
            "Lembre-se do passado irregular de go: went.",
            5
        );

        CommunityReviewResponseDTO response = communityReviewService.addReview(20L, request);

        assertNotNull(response);
        assertEquals(1, submission.getReviewCount());
        assertEquals("REVIEWED", submission.getStatus());
        // Reviewer earned 25 XP
        assertEquals(125, reviewerLang.getScore());
        verify(studentLanguageRepository).save(reviewerLang);
    }

    @Test
    void communityReview_authorCannotReviewOwnSubmission() {
        CommunitySubmission submission = CommunitySubmission.builder()
            .id(100L)
            .authorStudentId(5L) // Author is 5L
            .build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        CommunityReviewRequestDTO request = new CommunityReviewRequestDTO(
            100L,
            "Feedback",
            "Corrected",
            "Tips",
            5
        );

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> communityReviewService.addReview(5L, request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Você não pode avaliar seu próprio exercício"));
    }
}
