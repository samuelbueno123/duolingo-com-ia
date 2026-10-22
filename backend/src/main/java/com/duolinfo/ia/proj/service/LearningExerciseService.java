package com.duolinfo.ia.proj.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.dto.request.ExerciseQuestionRequestDTO;
import com.duolinfo.ia.proj.dto.request.LearningExerciseRequestDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseQuestionResponseDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO.QuestionFeedbackDTO;
import com.duolinfo.ia.proj.dto.response.LearningExerciseResponseDTO;
import com.duolinfo.ia.proj.dto.response.LearningRankingEntryResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentProgressSummaryResponseDTO;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LearningExerciseService {

    private static final TypeReference<List<ExerciseQuestionRequestDTO>> QUESTION_LIST =
        new TypeReference<>() {};
    private static final TypeReference<List<QuestionFeedbackDTO>> QUESTION_FEEDBACK_LIST =
        new TypeReference<>() {};
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ObjectMapper objectMapper;
    private final LearningExerciseRepository exerciseRepository;
    private final ExerciseSubmissionRepository submissionRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassTeacherAssignmentRepository assignmentRepository;
    private final AchievementService achievementService;

    @Transactional(readOnly = true)
    public List<LearningExerciseResponseDTO> findForStudent(String email) {
        Student student = findStudent(email);
        List<Long> classIds = enrollmentRepository
            .findByStudentIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE)
            .stream()
            .map(enrollment -> enrollment.getSchoolClass().getId())
            .distinct()
            .toList();
        if (classIds.isEmpty()) return List.of();
        return exerciseRepository
            .findBySchoolClass_IdInAndActiveTrueOrderByCreatedAtDesc(classIds)
            .stream()
            .map(exercise -> toExerciseResponse(exercise, false))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LearningExerciseResponseDTO> findForTeacher(String email) {
        Teacher teacher = findTeacher(email);
        return exerciseRepository.findByTeacher_IdOrderByCreatedAtDesc(teacher.getId())
            .stream()
            .map(exercise -> toExerciseResponse(exercise, true))
            .toList();
    }

    @Transactional(readOnly = true)
    public LearningExerciseResponseDTO findById(Long id, String email) {
        LearningExercise exercise = findExercise(id);
        Teacher teacher = teacherRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean isOwner = teacher != null
            && (Objects.equals(teacher.getId(), exercise.getTeacher().getId())
                || teacher.isSuperAdmin()
                || (teacher.isInstitutionAdmin()
                    && teacher.getInstitution() != null
                    && exercise.getSchoolClass().getSchool() != null
                    && exercise.getSchoolClass().getSchool().getInstitution() != null
                    && Objects.equals(
                        teacher.getInstitution().getId(),
                        exercise.getSchoolClass().getSchool().getInstitution().getId())));
        if (!isOwner) {
            Student student = findStudent(email);
            boolean enrolled = enrollmentRepository
                .findByStudentIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE)
                .stream()
                .anyMatch(item -> Objects.equals(
                    item.getSchoolClass().getId(), exercise.getSchoolClass().getId()
                ));
            if (!enrolled || !exercise.isActive()) {
                throw new ResourceNotFoundException("Exercício não encontrado.");
            }
        }
        return toExerciseResponse(exercise, isOwner);
    }

    @Transactional
    public LearningExerciseResponseDTO create(
            LearningExerciseRequestDTO request,
            String email) {
        Teacher teacher = findTeacher(email);
        List<ExerciseQuestionRequestDTO> questions = request.questions();
        if ((questions == null || questions.isEmpty())
                && clean(request.contentBody()) == null
                && clean(request.mediaUrl()) == null) {
            throw new BusinessException(
                "Informe o conteúdo teórico ou adicione ao menos uma questão."
            );
        }
        SchoolClass schoolClass = schoolClassRepository.findById(request.schoolClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada."));
        if (!teacher.isSuperAdmin()
                && (schoolClass.getSchool() == null
                    || teacher.getInstitution() == null
                    || !Objects.equals(
                        schoolClass.getSchool().getInstitution().getId(),
                        teacher.getInstitution().getId()
                    ))) {
            throw new BusinessException(
                "Você só pode criar exercícios para turmas da sua instituição."
            );
        }
        boolean assigned = teacher.isSuperAdmin()
            || teacher.isInstitutionAdmin()
            || assignmentRepository.findByTeacherId(teacher.getId())
                .stream()
                .anyMatch(assignment -> assignment.getEndDate() == null
                    && Objects.equals(
                        assignment.getSchoolClass().getId(), schoolClass.getId()
                    ));
        if (!assigned) {
            throw new BusinessException(
                "Você só pode publicar atividades em turmas atribuídas ao seu perfil."
            );
        }
        LearningExercise exercise = new LearningExercise();
        exercise.setTitle(request.title().trim());
        exercise.setCategory(clean(request.category()));
        exercise.setTheme(clean(request.theme()));
        exercise.setLanguageName(request.languageName().trim());
        exercise.setProficiencyLevel(request.proficiencyLevel().trim());
        exercise.setDescription(clean(request.description()));
        exercise.setContentBody(clean(request.contentBody()));
        exercise.setMediaUrl(clean(request.mediaUrl()));
        exercise.setDueDate(request.dueDate());
        exercise.setSchoolClass(schoolClass);
        exercise.setTeacher(teacher);
        exercise.setQuestionsJson(writeJson(questions == null ? List.of() : questions));
        exercise.setCreatedAt(Instant.now());
        return toExerciseResponse(exerciseRepository.save(exercise), true);
    }

    @Transactional
    public ExerciseSubmissionResponseDTO submit(
            Long exerciseId,
            List<String> answers,
            String email) {
        Student student = findStudent(email);
        LearningExercise exercise = findExercise(exerciseId);
        boolean enrolled = enrollmentRepository
            .findByStudentIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE)
            .stream()
            .anyMatch(item -> Objects.equals(
                item.getSchoolClass().getId(), exercise.getSchoolClass().getId()
            ));
        if (!enrolled || !exercise.isActive()) {
            throw new ResourceNotFoundException("Exercício não encontrado.");
        }
        List<ExerciseQuestionRequestDTO> questions = readQuestions(exercise);
        if (answers == null || answers.size() != questions.size()) {
            throw new BusinessException(
                "Envie uma resposta para cada questão do exercício."
            );
        }

        List<ExerciseSubmission> previousSubmissions = submissionRepository
            .findByStudent_IdAndExercise_IdOrderBySubmittedAtDesc(student.getId(), exercise.getId());
        int previouslyAwardedXp = previousSubmissions.stream()
            .mapToInt(ExerciseSubmission::getXpEarned)
            .sum();

        int correct = 0;
        boolean hasPendingOpenReview = false;
        List<QuestionFeedbackDTO> questionResults = new ArrayList<>();

        for (int index = 0; index < questions.size(); index++) {
            ExerciseQuestionRequestDTO question = questions.get(index);
            String style = clean(question.style()) != null ? question.style().trim() : "Objetiva";
            String rawAnswer = answers.get(index) != null ? answers.get(index).trim() : "";
            String expected = clean(question.correctAnswer());
            if (expected == null) {
                expected = clean(question.expectedAnswer());
            }

            if (isOpenEndedStyle(style)) {
                if (rawAnswer.isEmpty()) {
                    questionResults.add(new QuestionFeedbackDTO(
                        index,
                        style,
                        "INCORRECT",
                        "Resposta em branco."
                    ));
                } else if (expected != null && normalize(expected).equals(normalize(rawAnswer))) {
                    correct++;
                    questionResults.add(new QuestionFeedbackDTO(
                        index,
                        style,
                        "CORRECT",
                        "Resposta validada automaticamente!"
                    ));
                } else {
                    hasPendingOpenReview = true;
                    questionResults.add(new QuestionFeedbackDTO(
                        index,
                        style,
                        "PENDING_REVIEW",
                        "Resposta discursiva registrada para avaliação do professor."
                    ));
                }
            } else {
                if (expected != null && !rawAnswer.isEmpty()
                        && normalize(expected).equals(normalize(rawAnswer))) {
                    correct++;
                    questionResults.add(new QuestionFeedbackDTO(
                        index,
                        style,
                        "CORRECT",
                        "Resposta correta!"
                    ));
                } else {
                    questionResults.add(new QuestionFeedbackDTO(
                        index,
                        style,
                        "INCORRECT",
                        "Resposta incorreta. Revise o enunciado e tente novamente para conquistar o XP restante."
                    ));
                }
            }
        }

        int rawAttemptXp = correct * 10;
        int xpDelta = Math.max(0, rawAttemptXp - previouslyAwardedXp);

        ExerciseSubmission submission = new ExerciseSubmission();
        submission.setExercise(exercise);
        submission.setStudent(student);
        submission.setAnswersJson(writeJson(answers));
        submission.setCorrectCount(correct);
        submission.setQuestionCount(questions.size());
        submission.setXpEarned(xpDelta);
        submission.setReviewStatus(hasPendingOpenReview ? "PENDING_REVIEW" : "AUTO_GRADED");
        submission.setQuestionFeedbackJson(writeJson(questionResults));
        submission.setSubmittedAt(Instant.now());
        submissionRepository.save(submission);

        if (xpDelta > 0) {
            awardLanguageXp(student, exercise.getLanguageName(), xpDelta);
            studentRepository.save(student);
        }
        achievementService.evaluateAutoAchievements(student);
        return toSubmissionResponse(submission);
    }

    private void awardLanguageXp(Student student, String languageName, int xpDelta) {
        if (xpDelta <= 0 || languageName == null || languageName.isBlank()) {
            return;
        }
        for (StudentLanguage language : student.getLanguages()) {
            if (language.getLanguageName().equalsIgnoreCase(languageName.trim())) {
                language.setScore(language.getScore() + xpDelta);
                return;
            }
        }
        StudentLanguage newLang = new StudentLanguage();
        newLang.setLanguageName(languageName.trim());
        newLang.setLevel("Básico");
        newLang.setScore(xpDelta);
        student.addLanguage(newLang);
    }

    @Transactional(readOnly = true)
    public List<ExerciseSubmissionResponseDTO> findSubmissions(String email) {
        Teacher teacher = teacherRepository.findByEmailIgnoreCase(email).orElse(null);
        List<ExerciseSubmission> submissions = teacher == null
            ? submissionRepository.findByStudent_IdOrderBySubmittedAtDesc(
                findStudent(email).getId()
            )
            : submissionRepository.findByExercise_Teacher_IdOrderBySubmittedAtDesc(
                teacher.getId()
            );
        return submissions.stream().map(this::toSubmissionResponse).toList();
    }

    @Transactional
    public StudentProgressSummaryResponseDTO getStudentProgressSummary(
            String email,
            String timezone) {
        Student student = findStudent(email);
        ZoneId zone = resolveZone(timezone);
        List<ExerciseSubmission> submissions = submissionRepository
            .findByStudent_IdOrderBySubmittedAtDesc(student.getId());

        reconcileStudentLanguageScoresIfNeeded(student, submissions);

        int totalXp = student.getLanguages() == null
            ? 0
            : student.getLanguages().stream()
                .mapToInt(l -> l.getScore() != null ? l.getScore() : 0)
                .sum();

        LocalDate today = LocalDate.now(zone);
        int todayXp = 0;
        Set<Long> completedExerciseIds = new LinkedHashSet<>();
        TreeSet<LocalDate> activeDates = new TreeSet<>();

        for (ExerciseSubmission sub : submissions) {
            if (sub.getExercise() != null && sub.getExercise().getId() != null) {
                completedExerciseIds.add(sub.getExercise().getId());
            }
            if (sub.getSubmittedAt() != null) {
                LocalDate subDate = sub.getSubmittedAt().atZone(zone).toLocalDate();
                activeDates.add(subDate);
                if (subDate.equals(today)) {
                    todayXp += Math.max(0, sub.getXpEarned());
                }
            }
        }

        int dailyGoalMinutes = student.getEffectiveDailyGoalMinutes();
        int dailyGoalXp = dailyGoalMinutes * 2;
        boolean dailyGoalCompleted = todayXp >= dailyGoalXp;

        int currentStreakDays = calculateCurrentStreak(activeDates, today);
        int bestStreakDays = calculateBestStreak(activeDates);

        String criteria = "Cada questão correta concede 10 XP inéditos por exercício (reenvios premiam apenas a evolução da nota). "
            + "Meta diária: 2 XP por minuto configurado (" + dailyGoalMinutes + " min = " + dailyGoalXp + " XP/dia). "
            + "Ofensiva (streak) contabiliza dias consecutivos com atividade concluída no fuso " + zone.getId() + ".";

        return new StudentProgressSummaryResponseDTO(
            totalXp,
            todayXp,
            dailyGoalMinutes,
            dailyGoalXp,
            currentStreakDays,
            bestStreakDays,
            completedExerciseIds.size(),
            submissions.size(),
            dailyGoalCompleted,
            criteria
        );
    }

    private void reconcileStudentLanguageScoresIfNeeded(
            Student student,
            List<ExerciseSubmission> submissions) {
        if (submissions.isEmpty() || student.getLanguages() == null) {
            return;
        }
        Map<Long, List<ExerciseSubmission>> byExercise = new HashMap<>();
        for (ExerciseSubmission sub : submissions) {
            if (sub.getExercise() != null && sub.getExercise().getId() != null) {
                byExercise.computeIfAbsent(sub.getExercise().getId(), k -> new ArrayList<>()).add(sub);
            }
        }

        Map<String, Integer> deduplicatedXpByLang = new HashMap<>();
        boolean submissionsChanged = false;

        for (List<ExerciseSubmission> exSubs : byExercise.values()) {
            exSubs.sort(Comparator.comparing(ExerciseSubmission::getSubmittedAt));
            int awardedSoFar = 0;
            for (ExerciseSubmission sub : exSubs) {
                int targetXp = sub.getCorrectCount() * 10;
                int expectedDelta = Math.max(0, targetXp - awardedSoFar);
                if (sub.getXpEarned() != expectedDelta) {
                    sub.setXpEarned(expectedDelta);
                    submissionRepository.save(sub);
                    submissionsChanged = true;
                }
                awardedSoFar += expectedDelta;
            }
            String langName = exSubs.get(0).getExercise().getLanguageName();
            if (langName != null) {
                String key = langName.trim().toLowerCase(Locale.ROOT);
                deduplicatedXpByLang.merge(key, awardedSoFar, Integer::sum);
            }
        }

        boolean studentChanged = false;
        for (StudentLanguage lang : student.getLanguages()) {
            if (lang.getLanguageName() == null) continue;
            String key = lang.getLanguageName().trim().toLowerCase(Locale.ROOT);
            Integer expectedScore = deduplicatedXpByLang.get(key);
            if (expectedScore != null && !Objects.equals(lang.getScore(), expectedScore)) {
                lang.setScore(expectedScore);
                studentChanged = true;
            }
        }
        if (studentChanged || submissionsChanged) {
            studentRepository.save(student);
        }
    }

    private static int calculateCurrentStreak(TreeSet<LocalDate> activeDates, LocalDate today) {
        if (activeDates.isEmpty()) {
            return 0;
        }
        LocalDate cursor = activeDates.contains(today) ? today : today.minusDays(1);
        if (!activeDates.contains(cursor)) {
            return 0;
        }
        int streak = 0;
        while (activeDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static int calculateBestStreak(TreeSet<LocalDate> activeDates) {
        if (activeDates.isEmpty()) {
            return 0;
        }
        int best = 0;
        int current = 0;
        LocalDate prev = null;
        for (LocalDate date : activeDates) {
            if (prev != null && prev.plusDays(1).equals(date)) {
                current++;
            } else {
                current = 1;
            }
            if (current > best) {
                best = current;
            }
            prev = date;
        }
        return best;
    }

    private static ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ignored) {
            return DEFAULT_ZONE;
        }
    }

    @Transactional(readOnly = true)
    public List<LearningRankingEntryResponseDTO> findRanking(
            String period,
            boolean groupRanking) {
        Instant cutoff = cutoffFor(period);
        List<ExerciseSubmission> recentSubmissions =
            submissionRepository.findBySubmittedAtAfterOrderBySubmittedAtDesc(cutoff);

        Map<String, Integer> maxXpPerStudentExercise = new HashMap<>();
        Map<String, ExerciseSubmission> representativeSubmission = new HashMap<>();
        for (ExerciseSubmission sub : recentSubmissions) {
            String pairKey = sub.getStudent().getId() + ":" + sub.getExercise().getId();
            int rawXp = Math.max(sub.getXpEarned(), sub.getCorrectCount() * 10);
            int previousMax = maxXpPerStudentExercise.getOrDefault(pairKey, -1);
            if (rawXp > previousMax) {
                maxXpPerStudentExercise.put(pairKey, rawXp);
                representativeSubmission.put(pairKey, sub);
            }
        }

        Map<String, RankingTotal> totals = new HashMap<>();
        for (Map.Entry<String, ExerciseSubmission> entry : representativeSubmission.entrySet()) {
            ExerciseSubmission submission = entry.getValue();
            int effectiveXp = maxXpPerStudentExercise.getOrDefault(entry.getKey(), 0);
            String key = groupRanking
                ? "class:" + submission.getExercise().getSchoolClass().getId()
                : "student:" + submission.getStudent().getId();
            RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                groupRanking ? null : submission.getStudent().getName(),
                groupRanking ? submission.getExercise().getSchoolClass().getName() : null
            ));
            total.xp += effectiveXp;
            total.activities++;
            total.correct += submission.getCorrectCount();
            total.questions += submission.getQuestionCount();
        }
        List<RankingTotal> ordered = totals.values().stream()
            .sorted(Comparator.comparingInt((RankingTotal item) -> item.xp).reversed())
            .toList();
        List<LearningRankingEntryResponseDTO> result = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            RankingTotal item = ordered.get(index);
            result.add(new LearningRankingEntryResponseDTO(
                index + 1,
                item.studentName,
                item.className,
                item.xp,
                item.activities,
                item.correct,
                item.questions
            ));
        }
        return result;
    }

    private static Instant cutoffFor(String period) {
        Instant now = Instant.now();
        if (period == null) return now.minusSeconds(7L * 24 * 60 * 60);
        return switch (period.trim().toLowerCase(Locale.ROOT)) {
            case "month", "monthly", "mensal" -> now.minusSeconds(30L * 24 * 60 * 60);
            case "year", "yearly", "anual" -> now.minusSeconds(365L * 24 * 60 * 60);
            case "all", "alltime", "geral" -> Instant.EPOCH;
            default -> now.minusSeconds(7L * 24 * 60 * 60);
        };
    }

    private static final class RankingTotal {
        private final String studentName;
        private final String className;
        private int xp;
        private int activities;
        private int correct;
        private int questions;

        private RankingTotal(String studentName, String className) {
            this.studentName = studentName;
            this.className = className;
        }
    }

    @Transactional
    public ExerciseSubmissionResponseDTO addFeedback(
            Long submissionId,
            String feedback,
            String email) {
        return addFeedback(submissionId, feedback, null, email);
    }

    @Transactional
    public ExerciseSubmissionResponseDTO addFeedback(
            Long submissionId,
            String feedback,
            Integer adjustedCorrectCount,
            String email) {
        Teacher teacher = findTeacher(email);
        ExerciseSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada."));
        boolean canReview = Objects.equals(submission.getExercise().getTeacher().getId(), teacher.getId())
            || teacher.isSuperAdmin()
            || (teacher.isInstitutionAdmin()
                && teacher.getInstitution() != null
                && submission.getExercise().getSchoolClass().getSchool() != null
                && submission.getExercise().getSchoolClass().getSchool().getInstitution() != null
                && Objects.equals(
                    teacher.getInstitution().getId(),
                    submission.getExercise().getSchoolClass().getSchool().getInstitution().getId()));
        if (!canReview) {
            throw new ResourceNotFoundException("Entrega não encontrada.");
        }

        if (adjustedCorrectCount != null) {
            if (adjustedCorrectCount < 0 || adjustedCorrectCount > submission.getQuestionCount()) {
                throw new BusinessException(
                    "A quantidade de acertos avaliada deve estar entre 0 e " + submission.getQuestionCount() + "."
                );
            }
            List<ExerciseSubmission> allForExercise = submissionRepository
                .findByStudent_IdAndExercise_IdOrderBySubmittedAtDesc(
                    submission.getStudent().getId(),
                    submission.getExercise().getId()
                );
            int totalAwardedSoFar = allForExercise.stream()
                .mapToInt(ExerciseSubmission::getXpEarned)
                .sum();
            int newTargetXp = adjustedCorrectCount * 10;
            int additionalXp = Math.max(0, newTargetXp - totalAwardedSoFar);

            submission.setCorrectCount(adjustedCorrectCount);
            if (additionalXp > 0) {
                submission.setXpEarned(submission.getXpEarned() + additionalXp);
                awardLanguageXp(
                    submission.getStudent(),
                    submission.getExercise().getLanguageName(),
                    additionalXp
                );
                studentRepository.save(submission.getStudent());
            }
        }

        submission.setFeedback(feedback.trim());
        submission.setReviewStatus("REVIEWED");
        submission.setReviewedAt(Instant.now());
        ExerciseSubmission saved = submissionRepository.save(submission);
        achievementService.evaluateAutoAchievements(saved.getStudent());
        return toSubmissionResponse(saved);
    }

    private Student findStudent(String email) {
        return studentRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado."));
    }

    private Teacher findTeacher(String email) {
        return teacherRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado."));
    }

    private LearningExercise findExercise(Long id) {
        return exerciseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado."));
    }

    private LearningExerciseResponseDTO toExerciseResponse(
            LearningExercise exercise,
            boolean includeAnswers) {
        List<ExerciseQuestionResponseDTO> questions = readQuestions(exercise)
            .stream()
            .map(question -> new ExerciseQuestionResponseDTO(
                question.style(),
                question.question(),
                question.options() == null ? List.of() : question.options(),
                includeAnswers ? question.correctAnswer() : null,
                includeAnswers ? question.expectedAnswer() : null
            ))
            .toList();
        return new LearningExerciseResponseDTO(
            exercise.getId(),
            exercise.getTitle(),
            exercise.getCategory(),
            exercise.getTheme(),
            exercise.getLanguageName(),
            exercise.getProficiencyLevel(),
            exercise.getDescription(),
            exercise.getContentBody(),
            exercise.getMediaUrl(),
            exercise.getDueDate(),
            exercise.getSchoolClass().getId(),
            exercise.getSchoolClass().getName(),
            exercise.getTeacher().getId(),
            exercise.getTeacher().getName(),
            exercise.getCreatedAt(),
            questions
        );
    }

    private ExerciseSubmissionResponseDTO toSubmissionResponse(
            ExerciseSubmission submission) {
        String status = submission.getReviewStatus();
        if (status == null || status.isBlank()) {
            status = submission.getReviewedAt() != null ? "REVIEWED" : "AUTO_GRADED";
        }
        return new ExerciseSubmissionResponseDTO(
            submission.getId(),
            submission.getExercise().getId(),
            submission.getExercise().getTitle(),
            submission.getExercise().getLanguageName(),
            submission.getStudent().getId(),
            submission.getStudent().getName(),
            readAnswers(submission),
            submission.getCorrectCount(),
            submission.getQuestionCount(),
            submission.getXpEarned(),
            submission.getFeedback(),
            submission.getSubmittedAt(),
            submission.getReviewedAt(),
            status,
            readQuestionFeedback(submission)
        );
    }

    private List<ExerciseQuestionRequestDTO> readQuestions(LearningExercise exercise) {
        try {
            return objectMapper.readValue(exercise.getQuestionsJson(), QUESTION_LIST);
        } catch (JacksonException error) {
            throw new BusinessException("O conteúdo deste exercício está inválido.");
        }
    }

    private List<String> readAnswers(ExerciseSubmission submission) {
        try {
            return objectMapper.readValue(
                submission.getAnswersJson(), new TypeReference<List<String>>() {}
            );
        } catch (JacksonException error) {
            return new ArrayList<>();
        }
    }

    private List<QuestionFeedbackDTO> readQuestionFeedback(ExerciseSubmission submission) {
        if (submission.getQuestionFeedbackJson() == null || submission.getQuestionFeedbackJson().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(submission.getQuestionFeedbackJson(), QUESTION_FEEDBACK_LIST);
        } catch (JacksonException error) {
            return Collections.emptyList();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException error) {
            throw new BusinessException("Não foi possível salvar o conteúdo.");
        }
    }

    private static boolean isOpenEndedStyle(String style) {
        if (style == null) return false;
        String lower = style.trim().toLowerCase(Locale.ROOT);
        return lower.contains("aberta") || lower.contains("discursiva") || lower.contains("open");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("^[.,!?;:\"'¡¿\\s]+|[.,!?;:\"'¡¿\\s]+$", "")
            .replaceAll("\\s+", " ");
    }
}
