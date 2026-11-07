package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.community.CommunityReportRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunityReviewRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunityReviewResponseDTO;
import com.duolinfo.ia.proj.dto.community.CommunitySubmissionRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunitySubmissionResponseDTO;
import com.duolinfo.ia.proj.entity.CommunityReport;
import com.duolinfo.ia.proj.entity.CommunityReview;
import com.duolinfo.ia.proj.entity.CommunitySubmission;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.repository.CommunityReportRepository;
import com.duolinfo.ia.proj.repository.CommunityReviewRepository;
import com.duolinfo.ia.proj.repository.CommunitySubmissionRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

@Service
public class CommunityReviewService {

    private static final Logger log = LoggerFactory.getLogger(CommunityReviewService.class);

    private final CommunitySubmissionRepository submissionRepository;
    private final CommunityReviewRepository reviewRepository;
    private final CommunityReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final StudentLanguageRepository studentLanguageRepository;

    public CommunityReviewService(
            CommunitySubmissionRepository submissionRepository,
            CommunityReviewRepository reviewRepository,
            CommunityReportRepository reportRepository,
            StudentRepository studentRepository,
            StudentLanguageRepository studentLanguageRepository) {
        this.submissionRepository = submissionRepository;
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.studentRepository = studentRepository;
        this.studentLanguageRepository = studentLanguageRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialCommunityFeed() {
        if (submissionRepository.count() > 0) {
            return;
        }

        List<CommunitySubmission> starterSubmissions = List.of(
            CommunitySubmission.builder()
                .authorStudentId(0L)
                .authorName("Lucas Silva (Estudante)")
                .language("Inglês")
                .promptTitle("Descreva sua rotina matinal ideal")
                .content("In the morning I like wake up at 7 AM. I drink one black coffee and eats bread with butter. After I go to work with my bicycle.")
                .targetLevel("A2")
                .isPublic(true)
                .status("PENDING_REVIEW")
                .reviewCount(0)
                .createdAt(LocalDateTime.now().minusHours(4))
                .build(),

            CommunitySubmission.builder()
                .authorStudentId(0L)
                .authorName("Mariana Costa (Estudante)")
                .language("Inglês")
                .promptTitle("Por que aprender novos idiomas é importante?")
                .content("Learning language help people to connect with others cultures. When I travel, I can making friends more easy and understand movies without subtitles.")
                .targetLevel("B1")
                .isPublic(true)
                .status("PENDING_REVIEW")
                .reviewCount(0)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build(),

            CommunitySubmission.builder()
                .authorStudentId(0L)
                .authorName("Carlos Mendes (Estudante)")
                .language("Espanhol")
                .promptTitle("Mi viaje favorito de vacaciones")
                .content("El año pasado yo fue a Buenos Aires con mis amigos. Nosotros comemos comida muy rica y caminamos por todos los barrios antiguos.")
                .targetLevel("A2")
                .isPublic(true)
                .status("PENDING_REVIEW")
                .reviewCount(0)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build()
        );

        submissionRepository.saveAll(starterSubmissions);
        log.info("Inicializados {} posts comunitários de exemplo para revisão colaborativa.", starterSubmissions.size());
    }

    @Transactional
    public CommunitySubmissionResponseDTO createSubmission(Long studentId, CommunitySubmissionRequestDTO dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        CommunitySubmission submission = CommunitySubmission.builder()
                .authorStudentId(studentId)
                .authorName(student.getName())
                .language(dto.language().trim())
                .promptTitle(dto.promptTitle().trim())
                .content(dto.content().trim())
                .audioUrl(dto.audioUrl())
                .targetLevel(dto.targetLevel() != null ? dto.targetLevel() : "A2")
                .isPublic(dto.isPublic() != null ? dto.isPublic() : true)
                .status("PENDING_REVIEW")
                .reviewCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        submission = submissionRepository.save(submission);
        return CommunitySubmissionResponseDTO.fromEntity(submission, List.of());
    }

    @Transactional(readOnly = true)
    public List<CommunitySubmissionResponseDTO> getCommunityFeed(Long studentId, String language) {
        List<CommunitySubmission> list = (language != null && !language.isBlank())
                ? submissionRepository.findByLanguageIgnoreCaseAndIsPublicTrueOrderByCreatedAtDesc(language.trim())
                : submissionRepository.findByIsPublicTrueOrderByCreatedAtDesc();

        return list.stream()
                .filter(s -> !s.getAuthorStudentId().equals(studentId)) // do not show own submissions in review feed
                .map(s -> {
                    List<CommunityReviewResponseDTO> reviews = reviewRepository.findBySubmissionIdOrderByCreatedAtDesc(s.getId())
                            .stream()
                            .map(CommunityReviewResponseDTO::fromEntity)
                            .toList();
                    return CommunitySubmissionResponseDTO.fromEntity(s, reviews);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunitySubmissionResponseDTO> getMySubmissions(Long studentId) {
        List<CommunitySubmission> list = submissionRepository.findByAuthorStudentIdOrderByCreatedAtDesc(studentId);
        return list.stream()
                .map(s -> {
                    List<CommunityReviewResponseDTO> reviews = reviewRepository.findBySubmissionIdOrderByCreatedAtDesc(s.getId())
                            .stream()
                            .map(CommunityReviewResponseDTO::fromEntity)
                            .toList();
                    return CommunitySubmissionResponseDTO.fromEntity(s, reviews);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunitySubmissionResponseDTO getSubmissionDetails(Long submissionId) {
        CommunitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercício comunitário não encontrado."));

        List<CommunityReviewResponseDTO> reviews = reviewRepository.findBySubmissionIdOrderByCreatedAtDesc(submission.getId())
                .stream()
                .map(CommunityReviewResponseDTO::fromEntity)
                .toList();

        return CommunitySubmissionResponseDTO.fromEntity(submission, reviews);
    }

    @Transactional
    public CommunityReviewResponseDTO addReview(Long reviewerStudentId, CommunityReviewRequestDTO dto) {
        CommunitySubmission submission = submissionRepository.findById(dto.submissionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercício não encontrado."));

        if (submission.getAuthorStudentId().equals(reviewerStudentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode avaliar seu próprio exercício submetido.");
        }

        if (reviewRepository.existsBySubmissionIdAndReviewerStudentId(submission.getId(), reviewerStudentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você já enviou uma avaliação para este exercício.");
        }

        Student reviewer = studentRepository.findById(reviewerStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil do avaliador não encontrado."));

        // Determine reviewer fluency
        String fluencyLevel = "Intermediário (B2)";
        StudentLanguage sl = studentLanguageRepository.findByStudentAndLanguageNameIgnoreCase(reviewer, submission.getLanguage())
                .orElse(null);
        if (sl != null) {
            int currentScore = sl.getScore() != null ? sl.getScore() : 0;
            if (currentScore > 500) {
                fluencyLevel = "Avançado (C1)";
            } else if (currentScore > 200) {
                fluencyLevel = "Independente (B2)";
            } else {
                fluencyLevel = "Praticante (B1)";
            }
        }

        CommunityReview review = CommunityReview.builder()
                .submissionId(submission.getId())
                .reviewerStudentId(reviewerStudentId)
                .reviewerName(reviewer.getName())
                .reviewerFluencyLevel(fluencyLevel)
                .overallFeedback(dto.overallFeedback().trim())
                .correctedContent(dto.correctedContent().trim())
                .nativeSpeakerTips(dto.nativeSpeakerTips() != null ? dto.nativeSpeakerTips().trim() : "")
                .rating(dto.rating() != null ? dto.rating() : 5)
                .createdAt(LocalDateTime.now())
                .build();

        review = reviewRepository.save(review);

        // Update submission status
        submission.setReviewCount(submission.getReviewCount() + 1);
        submission.setStatus("REVIEWED");
        submissionRepository.save(submission);

        // Award reviewer with 25 XP for helping the community
        awardExperience(reviewerStudentId, submission.getLanguage(), 25);

        return CommunityReviewResponseDTO.fromEntity(review);
    }

    @Transactional
    public void reportReview(Long studentId, CommunityReportRequestDTO dto) {
        if (!reviewRepository.existsById(dto.reviewId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação a ser denunciada não foi encontrada.");
        }

        CommunityReport report = CommunityReport.builder()
                .reportedReviewId(dto.reviewId())
                .reportedByStudentId(studentId)
                .reason(dto.reason().trim())
                .details(dto.details() != null ? dto.details().trim() : "")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        reportRepository.save(report);
    }

    private void awardExperience(Long studentId, String language, int xpAmount) {
        try {
            studentRepository.findById(studentId).ifPresent(student -> {
                StudentLanguage sl = studentLanguageRepository
                        .findByStudentAndLanguageNameIgnoreCase(student, language)
                        .orElse(null);
                if (sl != null) {
                    sl.setScore((sl.getScore() != null ? sl.getScore() : 0) + xpAmount);
                    studentLanguageRepository.save(sl);
                }
            });
        } catch (Exception e) {
            log.warn("Erro ao atribuir XP de revisão comunitária: {}", e.getMessage());
        }
    }
}
