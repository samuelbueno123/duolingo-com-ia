package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.AchievementRequestDTO;
import com.duolinfo.ia.proj.dto.response.AchievementResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentAchievementsOverviewDTO;
import com.duolinfo.ia.proj.entity.Achievement;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentAchievement;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.repository.AchievementRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.StudentAchievementRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final StudentAchievementRepository studentAchievementRepository;
    private final StudentRepository studentRepository;
    private final ExerciseSubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public List<AchievementResponseDTO> findForStudentEmail(String email) {
        Student student = studentRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado."));
        evaluateAutoAchievements(student);
        return buildAchievementListForStudent(student.getId());
    }

    @Transactional
    public StudentAchievementsOverviewDTO getStudentOverview(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado: " + studentId));
        evaluateAutoAchievements(student);
        return new StudentAchievementsOverviewDTO(
            student.getId(),
            student.getName(),
            student.getEmail(),
            student.getNormalizedGender(),
            student.getInstitution() != null ? student.getInstitution().getName() : null,
            buildAchievementListForStudent(student.getId())
        );
    }

    @Transactional(readOnly = true)
    public List<StudentAchievementsOverviewDTO> searchStudentsWithAchievements(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return studentRepository.findAll().stream()
            .filter(student -> normalized.isEmpty()
                || (student.getName() != null && student.getName().toLowerCase(Locale.ROOT).contains(normalized))
                || (student.getEmail() != null && student.getEmail().toLowerCase(Locale.ROOT).contains(normalized)))
            .map(student -> new StudentAchievementsOverviewDTO(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getNormalizedGender(),
                student.getInstitution() != null ? student.getInstitution().getName() : null,
                buildAchievementListForStudent(student.getId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AchievementResponseDTO> findAllCatalog() {
        return achievementRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
            .map(item -> toAchievementResponse(item, null))
            .toList();
    }

    @Transactional
    public AchievementResponseDTO createCatalogAchievement(AchievementRequestDTO request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        if (achievementRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conquista com o código: " + code);
        }
        Achievement saved = achievementRepository.save(Achievement.builder()
            .code(code)
            .title(request.title().trim())
            .description(request.description().trim())
            .category(request.category().trim())
            .iconName(request.iconName() == null || request.iconName().isBlank() ? "emoji_events_rounded" : request.iconName().trim())
            .displayOrder(request.displayOrder() == null ? 99 : request.displayOrder())
            .build());
        return toAchievementResponse(saved, null);
    }

    @Transactional
    public StudentAchievementsOverviewDTO grantAchievement(
            Long studentId,
            Long achievementId,
            String grantedByEmail) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado: " + studentId));
        Achievement achievement = achievementRepository.findById(achievementId)
            .orElseThrow(() -> new ResourceNotFoundException("Conquista não encontrada: " + achievementId));

        StudentAchievement existing = studentAchievementRepository
            .findByStudentIdAndAchievementId(studentId, achievementId)
            .orElse(null);
        if (existing == null) {
            studentAchievementRepository.save(StudentAchievement.builder()
                .student(student)
                .achievement(achievement)
                .earnedAt(LocalDateTime.now())
                .grantedByAdmin(true)
                .revokedByAdmin(false)
                .build());
        } else {
            existing.setGrantedByAdmin(true);
            existing.setRevokedByAdmin(false);
            existing.setEarnedAt(LocalDateTime.now());
            studentAchievementRepository.save(existing);
        }
        return getStudentOverview(studentId);
    }

    @Transactional
    public StudentAchievementsOverviewDTO revokeAchievement(
            Long studentId,
            Long achievementId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado: " + studentId));
        Achievement achievement = achievementRepository.findById(achievementId)
            .orElseThrow(() -> new ResourceNotFoundException("Conquista não encontrada: " + achievementId));
        StudentAchievement existing = studentAchievementRepository
            .findByStudentIdAndAchievementId(studentId, achievementId)
            .orElse(null);
        if (existing != null) {
            existing.setRevokedByAdmin(true);
            existing.setGrantedByAdmin(false);
            studentAchievementRepository.save(existing);
        } else {
            studentAchievementRepository.save(StudentAchievement.builder()
                .student(student)
                .achievement(achievement)
                .earnedAt(LocalDateTime.now())
                .grantedByAdmin(false)
                .revokedByAdmin(true)
                .build());
        }
        return getStudentOverview(studentId);
    }

    @Transactional
    public void evaluateAutoAchievements(Student student) {
        if (student == null || student.getId() == null) return;
        List<ExerciseSubmission> submissions = submissionRepository.findByStudent_IdOrderBySubmittedAtDesc(student.getId());
        int submissionCount = submissions.size();
        boolean hasPerfect = submissions.stream()
            .anyMatch(s -> s.getQuestionCount() > 0 && s.getCorrectCount() >= s.getQuestionCount());
        boolean hasFeedback = submissions.stream()
            .anyMatch(s -> s.getFeedback() != null && !s.getFeedback().isBlank());
        int totalXp = student.getLanguages() == null ? 0 : student.getLanguages().stream()
            .mapToInt(StudentLanguage::getScore)
            .sum();
        int languageCount = student.getLanguages() == null ? 0 : student.getLanguages().size();
        boolean hasActiveClass = !enrollmentRepository
            .findByStudentIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE)
            .isEmpty();

        if (submissionCount >= 1) unlockByCodeIfAbsent(student, "FIRST_SUBMISSION");
        if (submissionCount >= 5) unlockByCodeIfAbsent(student, "FIVE_SUBMISSIONS");
        if (submissionCount >= 10) unlockByCodeIfAbsent(student, "TEN_SUBMISSIONS");
        if (hasPerfect) unlockByCodeIfAbsent(student, "PERFECT_SCORE");
        if (totalXp >= 100) unlockByCodeIfAbsent(student, "FIRST_100_XP");
        if (totalXp >= 500) unlockByCodeIfAbsent(student, "FIRST_500_XP");
        if (languageCount >= 2) unlockByCodeIfAbsent(student, "POLYGLOT");
        if (hasActiveClass) unlockByCodeIfAbsent(student, "CLASS_MEMBER");
        if (hasFeedback) unlockByCodeIfAbsent(student, "TEACHER_FEEDBACK");
    }

    private void unlockByCodeIfAbsent(Student student, String code) {
        achievementRepository.findByCode(code).ifPresent(achievement -> {
            if (!studentAchievementRepository.existsByStudentIdAndAchievementId(student.getId(), achievement.getId())) {
                studentAchievementRepository.save(StudentAchievement.builder()
                    .student(student)
                    .achievement(achievement)
                    .earnedAt(LocalDateTime.now())
                    .grantedByAdmin(false)
                    .build());
            }
        });
    }

    private List<AchievementResponseDTO> buildAchievementListForStudent(Long studentId) {
        List<Achievement> catalog = achievementRepository.findAllByOrderByDisplayOrderAscIdAsc();
        Map<Long, StudentAchievement> unlockedMap = studentAchievementRepository
            .findByStudentId(studentId)
            .stream()
            .filter(sa -> !sa.isRevokedByAdmin())
            .collect(Collectors.toMap(
                sa -> sa.getAchievement().getId(),
                Function.identity(),
                (a, b) -> a
            ));

        return catalog.stream()
            .map(item -> toAchievementResponse(item, unlockedMap.get(item.getId())))
            .toList();
    }

    private static AchievementResponseDTO toAchievementResponse(
            Achievement item,
            StudentAchievement unlocked) {
        return new AchievementResponseDTO(
            item.getId(),
            item.getCode(),
            item.getTitle(),
            item.getDescription(),
            item.getCategory(),
            item.getIconName(),
            item.getDisplayOrder(),
            unlocked != null,
            unlocked != null ? unlocked.getEarnedAt() : null,
            unlocked != null && unlocked.isGrantedByAdmin()
        );
    }
}
