package com.duolinfo.ia.proj.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.dto.request.ExerciseQuestionRequestDTO;
import com.duolinfo.ia.proj.dto.request.LearningExerciseRequestDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseQuestionResponseDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO;
import com.duolinfo.ia.proj.dto.response.LearningExerciseResponseDTO;
import com.duolinfo.ia.proj.dto.response.LearningRankingEntryResponseDTO;
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
            && Objects.equals(teacher.getId(), exercise.getTeacher().getId());
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
        if (schoolClass.getSchool() == null
                || !Objects.equals(
                    schoolClass.getSchool().getInstitution().getId(),
                    teacher.getInstitution().getId()
                )) {
            throw new BusinessException(
                "Você só pode criar exercícios para turmas da sua instituição."
            );
        }
        boolean assigned = assignmentRepository.findByTeacherId(teacher.getId())
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
        if (answers.size() != questions.size()) {
            throw new BusinessException(
                "Envie uma resposta para cada questão do exercício."
            );
        }

        int correct = 0;
        for (int index = 0; index < questions.size(); index++) {
            ExerciseQuestionRequestDTO question = questions.get(index);
            String expected = clean(question.correctAnswer());
            if (expected == null) expected = clean(question.expectedAnswer());
            if (expected != null
                    && normalize(expected).equals(normalize(answers.get(index)))) {
                correct++;
            }
        }

        ExerciseSubmission submission = new ExerciseSubmission();
        submission.setExercise(exercise);
        submission.setStudent(student);
        submission.setAnswersJson(writeJson(answers));
        submission.setCorrectCount(correct);
        submission.setQuestionCount(questions.size());
        submission.setXpEarned(correct * 10);
        submission.setSubmittedAt(Instant.now());
        submissionRepository.save(submission);

        int remainingXp = submission.getXpEarned();
        for (StudentLanguage language : student.getLanguages()) {
            if (language.getLanguageName().equalsIgnoreCase(exercise.getLanguageName())) {
                language.setScore(language.getScore() + remainingXp);
                break;
            }
        }
        studentRepository.save(student);
        achievementService.evaluateAutoAchievements(student);
        return toSubmissionResponse(submission);
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

    @Transactional(readOnly = true)
    public List<LearningRankingEntryResponseDTO> findRanking(
            String period,
            boolean groupRanking) {
        Instant cutoff = cutoffFor(period);
        Map<String, RankingTotal> totals = new HashMap<>();
        for (ExerciseSubmission submission : submissionRepository
                .findBySubmittedAtAfterOrderBySubmittedAtDesc(cutoff)) {
            String key = groupRanking
                ? "class:" + submission.getExercise().getSchoolClass().getId()
                : "student:" + submission.getStudent().getId();
            RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                groupRanking ? null : submission.getStudent().getName(),
                groupRanking ? submission.getExercise().getSchoolClass().getName() : null
            ));
            total.xp += submission.getXpEarned();
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
        Teacher teacher = findTeacher(email);
        ExerciseSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada."));
        if (!Objects.equals(
                submission.getExercise().getTeacher().getId(), teacher.getId())) {
            throw new ResourceNotFoundException("Entrega não encontrada.");
        }
        submission.setFeedback(feedback.trim());
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
            submission.getReviewedAt()
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException error) {
            throw new BusinessException("Não foi possível salvar o conteúdo.");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
