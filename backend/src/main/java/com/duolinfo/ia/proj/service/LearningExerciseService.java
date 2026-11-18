package com.duolinfo.ia.proj.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.dto.request.CloneExerciseRequestDTO;
import com.duolinfo.ia.proj.dto.request.ExerciseFeedbackRequestDTO;
import com.duolinfo.ia.proj.dto.request.ExerciseQuestionRequestDTO;
import com.duolinfo.ia.proj.dto.request.LearningExerciseRequestDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseQuestionResponseDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO.QuestionFeedbackDTO;
import com.duolinfo.ia.proj.dto.response.LearningExerciseResponseDTO;
import com.duolinfo.ia.proj.dto.response.LearningRankingEntryResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentProgressSummaryResponseDTO;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.Institution;
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
import com.duolinfo.ia.proj.util.SupportedLanguages;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service

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
    private final SseEmitterService sseEmitterService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StudentMistakeService studentMistakeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StudentStreakService studentStreakService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StudentNotificationService studentNotificationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TeacherNotificationService teacherNotificationService;

    public void setStudentMistakeService(StudentMistakeService studentMistakeService) {
        this.studentMistakeService = studentMistakeService;
    }

    public void setStudentStreakService(StudentStreakService studentStreakService) {
        this.studentStreakService = studentStreakService;
    }

    public void setStudentNotificationService(StudentNotificationService studentNotificationService) {
        this.studentNotificationService = studentNotificationService;
    }

    public void setTeacherNotificationService(TeacherNotificationService teacherNotificationService) {
        this.teacherNotificationService = teacherNotificationService;
    }

    public LearningExerciseService(
            ObjectMapper objectMapper,
            LearningExerciseRepository exerciseRepository,
            ExerciseSubmissionRepository submissionRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            EnrollmentRepository enrollmentRepository,
            ClassTeacherAssignmentRepository assignmentRepository,
            AchievementService achievementService) {
        this(objectMapper, exerciseRepository, submissionRepository, teacherRepository,
             studentRepository, schoolClassRepository, enrollmentRepository, assignmentRepository,
             achievementService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public LearningExerciseService(
            ObjectMapper objectMapper,
            LearningExerciseRepository exerciseRepository,
            ExerciseSubmissionRepository submissionRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            EnrollmentRepository enrollmentRepository,
            ClassTeacherAssignmentRepository assignmentRepository,
            AchievementService achievementService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SseEmitterService sseEmitterService) {
        this.objectMapper = objectMapper;
        this.exerciseRepository = exerciseRepository;
        this.submissionRepository = submissionRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.achievementService = achievementService;
        this.sseEmitterService = sseEmitterService;
    }

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
                .findByStudentId(student.getId())
                .stream()
                .anyMatch(item -> Objects.equals(
                    item.getSchoolClass().getId(), exercise.getSchoolClass().getId()
                ));
            if (!enrolled || !exercise.isActive()) {
                throw new ResourceNotFoundException("Exercício não encontrado.");
            }
        }
        boolean hasSubmitted = false;
        Student callerStudent = studentRepository.findByEmailIgnoreCase(email).orElse(null);
        if (callerStudent != null) {
            hasSubmitted = !submissionRepository.findByStudent_IdAndExercise_IdOrderBySubmittedAtDesc(
                callerStudent.getId(), exercise.getId()
            ).isEmpty();
        }
        return toExerciseResponse(exercise, isOwner || hasSubmitted);
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
        if (request.dueDate() != null && request.dueDate().isBefore(LocalDate.now())) {
            throw new BusinessException("A data de entrega não pode ser anterior à data atual.");
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
        String classLang = SupportedLanguages.normalizeOrDefault(
            schoolClass.getLanguageName(),
            SupportedLanguages.inferFromClassNameOrDefault(schoolClass.getName())
        );
        String exerciseLang = SupportedLanguages.normalizeOrDefault(request.languageName(), classLang);
        if (!SupportedLanguages.matches(exerciseLang, classLang)) {
            throw new BusinessException(
                "O idioma do exercício (" + exerciseLang + ") deve coincidir com o idioma da turma (" + classLang + ")."
            );
        }
        LearningExercise exercise = new LearningExercise();
        exercise.setTitle(request.title().trim());
        exercise.setCategory(clean(request.category()));
        exercise.setTheme(clean(request.theme()));
        exercise.setLanguageName(exerciseLang);
        exercise.setProficiencyLevel(request.proficiencyLevel().trim());
        exercise.setDescription(clean(request.description()));
        exercise.setContentBody(clean(request.contentBody()));
        exercise.setMediaUrl(clean(request.mediaUrl()));
        exercise.setMascotHelpMessage(clean(request.mascotHelpMessage()));
        exercise.setDueDate(request.dueDate());
        exercise.setSchoolClass(schoolClass);
        exercise.setTeacher(teacher);
        exercise.setQuestionsJson(writeJson(questions == null ? List.of() : questions));
        exercise.setCreatedAt(Instant.now());
        LearningExercise saved = exerciseRepository.save(exercise);
        notifyStudentsNewExercise(saved);
        return toExerciseResponse(saved, true);
    }

    @Transactional
    public LearningExerciseResponseDTO update(
            Long id,
            LearningExerciseRequestDTO request,
            String email) {
        Teacher teacher = findTeacher(email);
        LearningExercise exercise = findExercise(id);

        boolean canEdit = Objects.equals(teacher.getId(), exercise.getTeacher().getId())
            || teacher.isSuperAdmin()
            || (teacher.isInstitutionAdmin()
                && teacher.getInstitution() != null
                && exercise.getSchoolClass().getSchool() != null
                && exercise.getSchoolClass().getSchool().getInstitution() != null
                && Objects.equals(
                    teacher.getInstitution().getId(),
                    exercise.getSchoolClass().getSchool().getInstitution().getId()
                ));
        if (!canEdit) {
            throw new BusinessException("Você não tem permissão para editar esta atividade.");
        }

        List<ExerciseQuestionRequestDTO> questions = request.questions();
        if ((questions == null || questions.isEmpty())
                && clean(request.contentBody()) == null
                && clean(request.mediaUrl()) == null) {
            throw new BusinessException(
                "Informe o conteúdo teórico ou adicione ao menos uma questão."
            );
        }
        if (request.dueDate() != null
                && !Objects.equals(request.dueDate(), exercise.getDueDate())
                && request.dueDate().isBefore(LocalDate.now())) {
            throw new BusinessException("A data de entrega não pode ser anterior à data atual.");
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
                "Você só pode mover exercícios para turmas da sua instituição."
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
                "Você só pode atribuir atividades a turmas vinculadas ao seu perfil."
            );
        }

        String classLang = SupportedLanguages.normalizeOrDefault(
            schoolClass.getLanguageName(),
            SupportedLanguages.inferFromClassNameOrDefault(schoolClass.getName())
        );
        String exerciseLang = SupportedLanguages.normalizeOrDefault(request.languageName(), classLang);
        if (!SupportedLanguages.matches(exerciseLang, classLang)) {
            throw new BusinessException(
                "O idioma do exercício (" + exerciseLang + ") deve coincidir com o idioma da turma (" + classLang + ")."
            );
        }

        exercise.setTitle(request.title().trim());
        exercise.setCategory(clean(request.category()));
        exercise.setTheme(clean(request.theme()));
        exercise.setLanguageName(exerciseLang);
        exercise.setProficiencyLevel(request.proficiencyLevel().trim());
        exercise.setDescription(clean(request.description()));
        exercise.setContentBody(clean(request.contentBody()));
        exercise.setMediaUrl(clean(request.mediaUrl()));
        exercise.setMascotHelpMessage(clean(request.mascotHelpMessage()));
        exercise.setDueDate(request.dueDate());
        exercise.setSchoolClass(schoolClass);
        exercise.setQuestionsJson(writeJson(questions == null ? List.of() : questions));

        LearningExercise saved = exerciseRepository.save(exercise);
        return toExerciseResponse(saved, true);
    }

    @Transactional
    public LearningExerciseResponseDTO cloneExercise(
            Long teacherId,
            Long exerciseId,
            CloneExerciseRequestDTO request) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado."));
        LearningExercise source = findExercise(exerciseId);

        SchoolClass targetClass = schoolClassRepository.findById(request.targetSchoolClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Turma de destino não encontrada."));

        boolean isAssigned = teacher.isSuperAdmin()
            || teacher.isInstitutionAdmin()
            || assignmentRepository.findByTeacherId(teacher.getId()).stream()
                .anyMatch(a -> a.getEndDate() == null && Objects.equals(a.getSchoolClass().getId(), targetClass.getId()));
        if (!isAssigned) {
            throw new BusinessException("Você não tem permissão para adicionar exercícios a esta turma.");
        }

        String targetClassLang = SupportedLanguages.normalizeOrDefault(
            targetClass.getLanguageName(),
            SupportedLanguages.inferFromClassNameOrDefault(targetClass.getName())
        );
        String sourceLang = SupportedLanguages.normalizeOrDefault(source.getLanguageName(), targetClassLang);
        if (!SupportedLanguages.matches(sourceLang, targetClassLang)) {
            throw new BusinessException(
                "Não é possível clonar um exercício de " + sourceLang + " para uma turma de " + targetClassLang + "."
            );
        }

        LearningExercise clone = new LearningExercise();
        String title = request.newTitle() != null && !request.newTitle().isBlank()
            ? request.newTitle().trim()
            : source.getTitle() + " (Cópia)";
        clone.setTitle(title);
        clone.setCategory(source.getCategory());
        clone.setTheme(source.getTheme());
        clone.setLanguageName(sourceLang);
        clone.setProficiencyLevel(source.getProficiencyLevel());
        clone.setDescription(source.getDescription());
        clone.setContentBody(source.getContentBody());
        clone.setMediaUrl(source.getMediaUrl());
        clone.setMascotHelpMessage(source.getMascotHelpMessage());
        clone.setDueDate(request.newDueDate() != null ? request.newDueDate() : source.getDueDate());
        clone.setSchoolClass(targetClass);
        clone.setTeacher(teacher);
        clone.setQuestionsJson(source.getQuestionsJson());
        clone.setActive(true);
        clone.setCreatedAt(Instant.now());

        LearningExercise saved = exerciseRepository.save(clone);
        notifyStudentsNewExercise(saved);
        return toExerciseResponse(saved, true);
    }

    @Transactional
    public LearningExerciseResponseDTO cloneExercise(
            Long exerciseId,
            CloneExerciseRequestDTO request,
            String email) {
        Teacher teacher = findTeacher(email);
        return cloneExercise(teacher.getId(), exerciseId, request);
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
            if (expected == null && isListeningStyle(style)) {
                expected = clean(question.audioText());
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
            } else if (isMatchingStyle(style)) {
                Map<String, String> expectedPairs = extractNormalizedPairMap(question.options(), expected);
                Map<String, String> studentPairs = extractNormalizedPairMap(List.of(), rawAnswer);
                if (!expectedPairs.isEmpty() && expectedPairs.equals(studentPairs)) {
                    correct++;
                    questionResults.add(new QuestionFeedbackDTO(
                        index,
                        style,
                        "CORRECT",
                        "Todos os pares associados corretamente!"
                    ));
                } else {
                    questionResults.add(new QuestionFeedbackDTO(
                        index,
                        style,
                        "INCORRECT",
                        "Alguns pares não correspondem. Revise as associações e tente novamente."
                    ));
                    if (studentMistakeService != null) {
                        try {
                            studentMistakeService.recordMistake(
                                student.getId(),
                                exercise.getId(),
                                question.question(),
                                rawAnswer,
                                expected != null ? expected : String.join(" | ", question.options() != null ? question.options() : List.of()),
                                null
                            );
                        } catch (Exception ignored) {
                        }
                    }
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
                    if (studentMistakeService != null) {
                        try {
                            studentMistakeService.recordMistake(
                                student.getId(),
                                exercise.getId(),
                                question.question(),
                                rawAnswer,
                                expected != null ? expected : "",
                                null
                            );
                        } catch (Exception ignored) {
                        }
                    }
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
        if (studentStreakService != null) {
            try {
                studentStreakService.recordActivity(student.getId(), xpDelta);
            } catch (Exception ignored) {
            }
        }
        achievementService.evaluateAutoAchievements(student);
        notifyExerciseSubmitted(submission, xpDelta);
        return toSubmissionResponse(submission);
    }

    private void awardLanguageXp(Student student, String languageName, int xpDelta) {
        if (xpDelta <= 0 || languageName == null || languageName.isBlank()) {
            return;
        }
        String canonical = SupportedLanguages.normalizeOrDefault(languageName, SupportedLanguages.DEFAULT);
        for (StudentLanguage language : student.getLanguages()) {
            if (SupportedLanguages.matches(language.getLanguageName(), canonical)) {
                language.setScore((language.getScore() != null ? language.getScore() : 0) + xpDelta);
                return;
            }
        }
        StudentLanguage newLang = new StudentLanguage();
        newLang.setLanguageName(canonical);
        newLang.setLevel("Básico");
        newLang.setScore(xpDelta);
        student.addLanguage(newLang);
    }

    @Transactional(readOnly = true)
    public ExerciseSubmissionResponseDTO findMySubmissionForExercise(Long exerciseId, String email) {
        Student student = findStudent(email);
        return submissionRepository.findByStudent_IdAndExercise_IdOrderBySubmittedAtDesc(student.getId(), exerciseId)
            .stream()
            .findFirst()
            .map(this::toSubmissionResponse)
            .orElse(null);
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

        if (student.getLastActiveDate() != null
                && student.getLastActiveDate().equals(today)
                && student.getTodayXpEarned() != null) {
            todayXp = Math.max(todayXp, Math.max(0, student.getTodayXpEarned()));
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
        Map<String, Integer> rawXpByLang = new HashMap<>();
        boolean submissionsChanged = false;

        for (List<ExerciseSubmission> exSubs : byExercise.values()) {
            exSubs.sort(Comparator.comparing(ExerciseSubmission::getSubmittedAt));
            int rawForExercise = 0;
            int awardedSoFar = 0;
            for (ExerciseSubmission sub : exSubs) {
                rawForExercise += Math.max(0, sub.getXpEarned());
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
                String canonical = SupportedLanguages.normalize(langName).orElse(langName);
                String key = SupportedLanguages.stripAccentsLower(canonical);
                rawXpByLang.merge(key, rawForExercise, Integer::sum);
                deduplicatedXpByLang.merge(key, awardedSoFar, Integer::sum);
            }
        }

        boolean studentChanged = false;
        for (StudentLanguage lang : student.getLanguages()) {
            if (lang.getLanguageName() == null) continue;
            String canonical = SupportedLanguages.normalize(lang.getLanguageName()).orElse(lang.getLanguageName());
            String key = SupportedLanguages.stripAccentsLower(canonical);
            Integer expectedExerciseXp = deduplicatedXpByLang.get(key);
            if (expectedExerciseXp == null) {
                continue;
            }
            int currentScore = lang.getScore() != null ? lang.getScore() : 0;
            int rawExerciseXp = rawXpByLang.getOrDefault(key, 0);
            int nextScore = currentScore;
            if (submissionsChanged && rawExerciseXp != expectedExerciseXp) {
                // Adjust only by the exercise over-award/under-award delta so XP from SRS, community, pronunciation, and paths stays intact.
                nextScore = Math.max(expectedExerciseXp, Math.max(0, currentScore + (expectedExerciseXp - rawExerciseXp)));
            } else if (currentScore < expectedExerciseXp) {
                nextScore = expectedExerciseXp;
            }
            if (nextScore != currentScore) {
                lang.setScore(nextScore);
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
        return findScopedRanking(
            groupRanking ? "app_classes" : "app_students",
            period,
            null,
            null,
            null
        );
    }

    @Transactional(readOnly = true)
    public List<LearningRankingEntryResponseDTO> findScopedRanking(
            String scope,
            String period,
            Long schoolClassId,
            Long institutionId,
            String viewerEmail) {
        String canonicalScope = normalizeRankingScope(scope);
        Long targetClassId = schoolClassId != null
            ? schoolClassId
            : resolveDefaultClassId(viewerEmail, canonicalScope);
        Long targetInstitutionId = institutionId != null
            ? institutionId
            : resolveUserInstitutionId(viewerEmail, targetClassId);

        Instant cutoff = cutoffFor(period);
        List<ExerciseSubmission> recentSubmissions =
            submissionRepository.findBySubmittedAtAfterOrderBySubmittedAtDesc(cutoff);

        Map<String, Integer> maxXpPerStudentExercise = new HashMap<>();
        Map<String, ExerciseSubmission> representativeSubmission = new HashMap<>();
        for (ExerciseSubmission sub : recentSubmissions) {
            if (sub.getStudent() == null || sub.getExercise() == null) continue;
            String pairKey = sub.getStudent().getId() + ":" + sub.getExercise().getId();
            int rawXp = Math.max(sub.getXpEarned(), sub.getCorrectCount() * 10);
            int previousMax = maxXpPerStudentExercise.getOrDefault(pairKey, -1);
            if (rawXp > previousMax) {
                maxXpPerStudentExercise.put(pairKey, rawXp);
                representativeSubmission.put(pairKey, sub);
            }
        }

        boolean teacherMode = canonicalScope.endsWith("_teachers");
        Map<Long, List<Teacher>> activeClassTeachers = teacherMode
            ? loadActiveTeachersByClass()
            : Map.of();

        Map<String, RankingTotal> totals = new HashMap<>();
        for (Map.Entry<String, ExerciseSubmission> entry : representativeSubmission.entrySet()) {
            ExerciseSubmission submission = entry.getValue();
            int effectiveXp = maxXpPerStudentExercise.getOrDefault(entry.getKey(), 0);
            SchoolClass sc = submission.getExercise().getSchoolClass();
            Institution classInst = resolveClassInstitution(sc);
            Student student = submission.getStudent();
            Institution studentInst = student.getInstitution() != null ? student.getInstitution() : classInst;

            switch (canonicalScope) {
                case "class_students" -> {
                    if (sc == null || targetClassId == null || !Objects.equals(sc.getId(), targetClassId)) {
                        continue;
                    }
                    String key = "student:" + student.getId();
                    RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                        student.getId(),
                        student.getName(),
                        sc.getName(),
                        student.getProfilePicture(),
                        classInst != null ? classInst.getName() : null
                    ));
                    total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                }
                case "school_students" -> {
                    if (targetInstitutionId == null || studentInst == null
                            || !Objects.equals(studentInst.getId(), targetInstitutionId)) {
                        continue;
                    }
                    String key = "student:" + student.getId();
                    RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                        student.getId(),
                        student.getName(),
                        null,
                        student.getProfilePicture(),
                        studentInst.getName()
                    ));
                    total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                }
                case "app_students" -> {
                    String key = "student:" + student.getId();
                    RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                        student.getId(),
                        student.getName(),
                        null,
                        student.getProfilePicture(),
                        studentInst != null ? studentInst.getName() : null
                    ));
                    total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                }
                case "school_classes" -> {
                    if (sc == null || targetInstitutionId == null || classInst == null
                            || !Objects.equals(classInst.getId(), targetInstitutionId)) {
                        continue;
                    }
                    String key = "class:" + sc.getId();
                    RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                        sc.getId(),
                        null,
                        sc.getName(),
                        null,
                        classInst.getName()
                    ));
                    total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                }
                case "app_classes" -> {
                    if (sc == null) continue;
                    String key = "class:" + sc.getId();
                    RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                        sc.getId(),
                        null,
                        sc.getName(),
                        null,
                        classInst != null ? classInst.getName() : null
                    ));
                    total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                }
                case "schools" -> {
                    Institution inst = classInst != null ? classInst : studentInst;
                    if (inst == null) continue;
                    String key = "school:" + inst.getId();
                    String loc = (inst.getCity() != null && !inst.getCity().isBlank())
                        ? inst.getCity()
                        : inst.getCode();
                    RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                        inst.getId(),
                        inst.getName(),
                        loc,
                        null,
                        inst.getName()
                    ));
                    total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                }
                case "class_teachers", "school_teachers", "app_teachers" -> {
                    if (sc == null) continue;
                    if ("class_teachers".equals(canonicalScope)
                            && (targetClassId == null || !Objects.equals(sc.getId(), targetClassId))) {
                        continue;
                    }
                    List<Teacher> teachersForClass = resolveTeachersForSubmission(
                        sc.getId(),
                        submission.getExercise().getTeacher(),
                        activeClassTeachers
                    );
                    for (Teacher t : teachersForClass) {
                        Institution tInst = t.getInstitution() != null ? t.getInstitution() : classInst;
                        if ("school_teachers".equals(canonicalScope)
                                && (targetInstitutionId == null || tInst == null
                                    || !Objects.equals(tInst.getId(), targetInstitutionId))) {
                            continue;
                        }
                        String key = "teacher:" + t.getId();
                        RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                            t.getId(),
                            t.getName(),
                            sc.getName(),
                            t.getProfilePicture(),
                            tInst != null ? tInst.getName() : null
                        ));
                        total.addClassLabel(sc.getName());
                        total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                    }
                }
                default -> {
                    String key = "student:" + student.getId();
                    RankingTotal total = totals.computeIfAbsent(key, ignored -> new RankingTotal(
                        student.getId(),
                        student.getName(),
                        null,
                        student.getProfilePicture(),
                        studentInst != null ? studentInst.getName() : null
                    ));
                    total.accumulate(effectiveXp, submission.getCorrectCount(), submission.getQuestionCount());
                }
            }
        }

        Comparator<RankingTotal> comparator = teacherMode
            ? Comparator.comparingDouble(RankingTotal::teacherPerformanceScore)
                .thenComparingInt((RankingTotal item) -> item.xp)
                .thenComparingInt((RankingTotal item) -> item.correct)
                .reversed()
            : Comparator.comparingInt((RankingTotal item) -> item.xp)
                .thenComparingInt((RankingTotal item) -> item.correct)
                .reversed();

        List<RankingTotal> ordered = totals.values().stream()
            .sorted(comparator)
            .toList();

        List<LearningRankingEntryResponseDTO> result = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            RankingTotal item = ordered.get(index);
            String labelClass = (teacherMode && !item.distinctClasses.isEmpty())
                ? String.join(", ", item.distinctClasses)
                : item.className;
            result.add(new LearningRankingEntryResponseDTO(
                index + 1,
                item.studentName,
                labelClass,
                item.xp,
                item.activities,
                item.correct,
                item.questions,
                item.profilePicture,
                item.schoolName,
                item.entityId,
                item.accuracyPercent()
            ));
        }
        return result;
    }

    private static String normalizeRankingScope(String raw) {
        if (raw == null || raw.isBlank()) return "app_students";
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "class_students", "individual_turma", "student_class" -> "class_students";
            case "school_students", "individual_escola", "student_school" -> "school_students";
            case "app_students", "individual", "individual_app", "student_app" -> "app_students";
            case "school_classes", "turma_escola", "class_school", "groups_school" -> "school_classes";
            case "app_classes", "groups", "turma_app", "class_app", "groups_app" -> "app_classes";
            case "schools", "escolas", "institutions", "school" -> "schools";
            case "class_teachers", "professor_turma", "teacher_class" -> "class_teachers";
            case "school_teachers", "professor_escola", "teacher_school" -> "school_teachers";
            case "app_teachers", "teachers", "professor_app", "teacher_app" -> "app_teachers";
            default -> "app_students";
        };
    }

    private Long resolveDefaultClassId(String email, String scope) {
        if (email == null || email.isBlank() || (!"class_students".equals(scope) && !"class_teachers".equals(scope))) {
            return null;
        }
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        Student student = studentRepository.findByEmailIgnoreCase(cleanEmail).orElse(null);
        if (student != null) {
            List<Enrollment> active = enrollmentRepository.findByStudentIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE);
            if (!active.isEmpty() && active.get(0).getSchoolClass() != null) {
                return active.get(0).getSchoolClass().getId();
            }
        }
        Teacher teacher = teacherRepository.findByEmailIgnoreCase(cleanEmail).orElse(null);
        if (teacher != null) {
            return assignmentRepository.findByTeacherIdOrderByStartDateDesc(teacher.getId()).stream()
                .filter(a -> a.getEndDate() == null && a.getSchoolClass() != null)
                .map(a -> a.getSchoolClass().getId())
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    private Long resolveUserInstitutionId(String email, Long fallbackClassId) {
        if (email != null && !email.isBlank()) {
            String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
            Student student = studentRepository.findByEmailIgnoreCase(cleanEmail).orElse(null);
            if (student != null) {
                if (student.getInstitution() != null) return student.getInstitution().getId();
                List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
                for (Enrollment e : enrollments) {
                    Institution inst = resolveClassInstitution(e.getSchoolClass());
                    if (inst != null) return inst.getId();
                }
            }
            Teacher teacher = teacherRepository.findByEmailIgnoreCase(cleanEmail).orElse(null);
            if (teacher != null && teacher.getInstitution() != null) {
                return teacher.getInstitution().getId();
            }
        }
        if (fallbackClassId != null) {
            return schoolClassRepository.findById(fallbackClassId)
                .map(LearningExerciseService::resolveClassInstitution)
                .map(Institution::getId)
                .orElse(null);
        }
        return null;
    }

    private static Institution resolveClassInstitution(SchoolClass sc) {
        if (sc == null) return null;
        if (sc.getInstitution() != null) return sc.getInstitution();
        return sc.getSchool() != null ? sc.getSchool().getInstitution() : null;
    }

    private Map<Long, List<Teacher>> loadActiveTeachersByClass() {
        Map<Long, List<Teacher>> byClass = new HashMap<>();
        for (ClassTeacherAssignment cta : assignmentRepository.findAll()) {
            if (cta.getEndDate() == null && cta.getSchoolClass() != null && cta.getTeacher() != null) {
                byClass.computeIfAbsent(cta.getSchoolClass().getId(), ignored -> new ArrayList<>())
                    .add(cta.getTeacher());
            }
        }
        return byClass;
    }

    private static List<Teacher> resolveTeachersForSubmission(
            Long classId,
            Teacher fallbackCreator,
            Map<Long, List<Teacher>> activeByClass) {
        List<Teacher> assigned = activeByClass.get(classId);
        if (assigned != null && !assigned.isEmpty()) {
            // Avoid duplicates while preserving order
            Map<Long, Teacher> unique = new java.util.LinkedHashMap<>();
            for (Teacher t : assigned) {
                if (t != null && t.getId() != null) unique.putIfAbsent(t.getId(), t);
            }
            if (!unique.isEmpty()) return new ArrayList<>(unique.values());
        }
        return fallbackCreator != null ? List.of(fallbackCreator) : List.of();
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
        private final Long entityId;
        private final String studentName;
        private final String className;
        private final String profilePicture;
        private final String schoolName;
        private final Set<String> distinctClasses = new LinkedHashSet<>();
        private int xp;
        private int activities;
        private int correct;
        private int questions;

        private RankingTotal(
                Long entityId,
                String studentName,
                String className,
                String profilePicture,
                String schoolName) {
            this.entityId = entityId;
            this.studentName = studentName;
            this.className = className;
            this.profilePicture = profilePicture;
            this.schoolName = schoolName;
        }

        private void addClassLabel(String name) {
            if (name != null && !name.isBlank()) {
                distinctClasses.add(name);
            }
        }

        private void accumulate(int addXp, int addCorrect, int addQuestions) {
            this.xp += addXp;
            this.activities++;
            this.correct += addCorrect;
            this.questions += addQuestions;
        }

        private double accuracyPercent() {
            return questions > 0 ? Math.round((correct * 1000.0) / questions) / 10.0 : 0.0;
        }

        private double teacherPerformanceScore() {
            return accuracyPercent() * 10.0 + xp;
        }
    }

    @Transactional
    public ExerciseSubmissionResponseDTO addFeedback(
            Long submissionId,
            String feedback,
            String email) {
        return addFeedback(submissionId, feedback, null, null, email);
    }

    @Transactional
    public ExerciseSubmissionResponseDTO addFeedback(
            Long submissionId,
            String feedback,
            Integer adjustedCorrectCount,
            String email) {
        return addFeedback(submissionId, feedback, adjustedCorrectCount, null, email);
    }

    @Transactional
    public ExerciseSubmissionResponseDTO addFeedback(
            Long submissionId,
            String feedback,
            Integer adjustedCorrectCount,
            List<ExerciseFeedbackRequestDTO.QuestionReviewItemDTO> questionReviews,
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

        int effectiveCorrect = submission.getCorrectCount();
        if (questionReviews != null && !questionReviews.isEmpty()) {
            List<ExerciseQuestionRequestDTO> questions = Collections.emptyList();
            try {
                if (submission.getExercise() != null && submission.getExercise().getQuestionsJson() != null) {
                    questions = readQuestions(submission.getExercise());
                }
            } catch (Exception ignored) {}
            List<String> answers = readAnswers(submission);

            Map<Integer, ExerciseFeedbackRequestDTO.QuestionReviewItemDTO> reviewMap = new HashMap<>();
            for (var r : questionReviews) {
                if (r != null && r.questionIndex() != null) {
                    reviewMap.put(r.questionIndex(), r);
                }
            }

            List<QuestionFeedbackDTO> currentFeedback = readQuestionFeedback(submission);
            Map<Integer, QuestionFeedbackDTO> feedbackMap = new HashMap<>();
            for (var cf : currentFeedback) {
                if (cf != null) {
                    feedbackMap.put(cf.questionIndex(), cf);
                }
            }

            int count = Math.max(questions.size(), Math.max(answers.size(), reviewMap.size()));
            List<QuestionFeedbackDTO> updatedFeedback = new ArrayList<>();
            int correctCountFromReviews = 0;

            for (int i = 0; i < count; i++) {
                ExerciseQuestionRequestDTO q = i < questions.size() ? questions.get(i) : null;
                String studentAns = i < answers.size() ? answers.get(i) : "";
                QuestionFeedbackDTO existing = feedbackMap.get(i);
                var review = reviewMap.get(i);

                String style = existing != null && existing.style() != null && !existing.style().isBlank()
                    ? existing.style()
                    : (q != null && q.style() != null ? q.style() : "Objetiva");
                String questionText = q != null && q.question() != null ? q.question() : "Questão " + (i + 1);
                List<String> options = q != null && q.options() != null ? q.options() : List.of();
                String expected = q != null ? (q.correctAnswer() != null ? q.correctAnswer() : q.expectedAnswer()) : null;

                boolean isCorrect;
                String status;
                String message;

                if (review != null) {
                    isCorrect = Boolean.TRUE.equals(review.isCorrect()) || "CORRECT".equalsIgnoreCase(review.status());
                    status = isCorrect ? "CORRECT" : "INCORRECT";
                    message = review.feedback() != null ? review.feedback().trim() : (existing != null && existing.message() != null ? existing.message() : "");
                } else if (existing != null) {
                    status = existing.status() != null ? existing.status() : "PENDING_REVIEW";
                    isCorrect = "CORRECT".equalsIgnoreCase(status);
                    message = existing.message() != null ? existing.message() : "";
                } else {
                    isCorrect = false;
                    status = "INCORRECT";
                    message = "";
                }

                if (isCorrect) {
                    correctCountFromReviews++;
                }

                updatedFeedback.add(new QuestionFeedbackDTO(
                    i,
                    style,
                    status,
                    message,
                    questionText,
                    options,
                    expected,
                    studentAns,
                    isCorrect
                ));
            }

            submission.setQuestionFeedbackJson(writeJson(updatedFeedback));
            effectiveCorrect = adjustedCorrectCount != null ? adjustedCorrectCount : correctCountFromReviews;
        } else if (adjustedCorrectCount != null) {
            effectiveCorrect = adjustedCorrectCount;
        }

        if (effectiveCorrect < 0 || effectiveCorrect > submission.getQuestionCount()) {
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
        int newTargetXp = effectiveCorrect * 10;
        int additionalXp = Math.max(0, newTargetXp - totalAwardedSoFar);

        submission.setCorrectCount(effectiveCorrect);
        if (additionalXp > 0) {
            submission.setXpEarned(submission.getXpEarned() + additionalXp);
            awardLanguageXp(
                submission.getStudent(),
                submission.getExercise().getLanguageName(),
                additionalXp
            );
            studentRepository.save(submission.getStudent());
        }

        if (feedback != null && !feedback.isBlank()) {
            submission.setFeedback(feedback.trim());
        }
        submission.setReviewStatus("REVIEWED");
        submission.setReviewedAt(Instant.now());
        ExerciseSubmission saved = submissionRepository.save(submission);
        achievementService.evaluateAutoAchievements(saved.getStudent());
        notifySubmissionReviewed(saved);
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
            .map(question -> {
                List<String> responseOptions = question.options() == null ? List.of() : question.options();
                if (isMatchingStyle(question.style()) && !includeAnswers) {
                    String fallbackCombined = clean(question.correctAnswer()) != null
                        ? question.correctAnswer()
                        : question.expectedAnswer();
                    List<String[]> pairs = extractRawPairs(question.options(), fallbackCombined);
                    if (!pairs.isEmpty()) {
                        List<String> lefts = new ArrayList<>(pairs.size());
                        List<String> rights = new ArrayList<>(pairs.size());
                        for (String[] pair : pairs) {
                            lefts.add("LEFT::" + pair[0]);
                            rights.add("RIGHT::" + pair[1]);
                        }
                        List<String> originalRights = new ArrayList<>(rights);
                        long seed = (exercise.getId() != null ? exercise.getId() : 17L) * 31L
                            + (question.question() != null ? question.question().hashCode() : 0L);
                        Collections.shuffle(rights, new Random(seed));
                        if (rights.size() > 1 && rights.equals(originalRights)) {
                            Collections.rotate(rights, 1);
                        }
                        List<String> merged = new ArrayList<>(lefts.size() + rights.size());
                        merged.addAll(lefts);
                        merged.addAll(rights);
                        responseOptions = merged;
                    }
                } else if (isTrueFalseStyle(question.style()) && responseOptions.isEmpty()) {
                    responseOptions = List.of("Verdadeiro", "Falso");
                }
                return new ExerciseQuestionResponseDTO(
                    question.style(),
                    question.question(),
                    responseOptions,
                    includeAnswers ? question.correctAnswer() : null,
                    includeAnswers ? question.expectedAnswer() : null,
                    clean(question.mascotHelpMessage()),
                    clean(question.audioText())
                );
            })
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
            questions,
            exercise.getMascotHelpMessage()
        );
    }

    private ExerciseSubmissionResponseDTO toSubmissionResponse(
            ExerciseSubmission submission) {
        String status = submission.getReviewStatus();
        if (status == null || status.isBlank()) {
            status = submission.getReviewedAt() != null ? "REVIEWED" : "AUTO_GRADED";
        }
        String studentPic = submission.getStudent() != null ? submission.getStudent().getProfilePicture() : null;
        List<QuestionFeedbackDTO> detailedQuestions = buildDetailedQuestionFeedback(submission);
        return new ExerciseSubmissionResponseDTO(
            submission.getId(),
            submission.getExercise().getId(),
            submission.getExercise().getTitle(),
            submission.getExercise().getLanguageName(),
            submission.getStudent().getId(),
            submission.getStudent().getName(),
            studentPic,
            readAnswers(submission),
            submission.getCorrectCount(),
            submission.getQuestionCount(),
            submission.getXpEarned(),
            submission.getFeedback(),
            submission.getSubmittedAt(),
            submission.getReviewedAt(),
            status,
            detailedQuestions
        );
    }

    private List<QuestionFeedbackDTO> buildDetailedQuestionFeedback(ExerciseSubmission submission) {
        List<ExerciseQuestionRequestDTO> questions = Collections.emptyList();
        try {
            if (submission.getExercise() != null && submission.getExercise().getQuestionsJson() != null) {
                questions = readQuestions(submission.getExercise());
            }
        } catch (Exception ignored) {}

        List<String> answers = readAnswers(submission);
        List<QuestionFeedbackDTO> rawFeedback = readQuestionFeedback(submission);
        Map<Integer, QuestionFeedbackDTO> feedbackMap = new HashMap<>();
        for (QuestionFeedbackDTO qf : rawFeedback) {
            if (qf != null) {
                feedbackMap.put(qf.questionIndex(), qf);
            }
        }

        int count = Math.max(questions.size(), answers.size());
        if (count == 0) {
            return rawFeedback;
        }

        List<QuestionFeedbackDTO> detailed = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ExerciseQuestionRequestDTO q = i < questions.size() ? questions.get(i) : null;
            String studentAns = i < answers.size() ? answers.get(i) : "";
            QuestionFeedbackDTO existing = feedbackMap.get(i);

            String style = existing != null && existing.style() != null && !existing.style().isBlank()
                ? existing.style()
                : (q != null && q.style() != null ? q.style() : "Objetiva");

            String expected = q != null ? (q.correctAnswer() != null ? q.correctAnswer() : q.expectedAnswer()) : null;

            String status;
            if (existing != null && existing.status() != null && !existing.status().isBlank()) {
                status = existing.status();
            } else if (isOpenEndedStyle(style)) {
                status = "PENDING_REVIEW";
            } else if (isMatchingStyle(style)) {
                List<String> opts = q != null && q.options() != null ? q.options() : List.of();
                Map<String, String> expMap = extractNormalizedPairMap(opts, expected);
                Map<String, String> ansMap = extractNormalizedPairMap(List.of(), studentAns);
                status = (!expMap.isEmpty() && expMap.equals(ansMap)) ? "CORRECT" : "INCORRECT";
            } else if (expected != null && normalize(expected).equals(normalize(studentAns))) {
                status = "CORRECT";
            } else {
                status = "INCORRECT";
            }

            String message = existing != null && existing.message() != null ? existing.message() : "";
            String questionText = q != null && q.question() != null ? q.question() : "Questão " + (i + 1);
            List<String> options = q != null && q.options() != null ? q.options() : List.of();
            boolean isCorrect = "CORRECT".equalsIgnoreCase(status);

            detailed.add(new QuestionFeedbackDTO(
                i,
                style,
                status,
                message,
                questionText,
                options,
                expected,
                studentAns,
                isCorrect
            ));
        }
        return detailed;
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

    private static boolean isTrueFalseStyle(String style) {
        if (style == null) return false;
        String lower = style.trim().toLowerCase(Locale.ROOT);
        return lower.contains("verdadeiro") || lower.contains("falso")
            || lower.contains("true_false") || lower.contains("true/false");
    }

    private static boolean isMatchingStyle(String style) {
        if (style == null) return false;
        String lower = style.trim().toLowerCase(Locale.ROOT);
        return lower.contains("correspond") || lower.contains("match")
            || lower.contains("associ") || lower.contains("ligar");
    }

    private static boolean isListeningStyle(String style) {
        if (style == null) return false;
        String lower = style.trim().toLowerCase(Locale.ROOT);
        return lower.contains("áudio") || lower.contains("audio")
            || lower.contains("escuta") || lower.contains("listening");
    }

    private static List<String[]> extractRawPairs(List<String> options, String combined) {
        List<String> tokens = new ArrayList<>();
        if (options != null) {
            for (String item : options) {
                if (item != null && !item.isBlank()) {
                    tokens.add(item.trim());
                }
            }
        }
        if (tokens.isEmpty() && combined != null && !combined.isBlank()) {
            for (String chunk : combined.split("[|;\\n]+")) {
                if (!chunk.isBlank()) {
                    tokens.add(chunk.trim());
                }
            }
        }
        List<String[]> pairs = new ArrayList<>();
        for (String token : tokens) {
            String[] split = splitPairToken(token);
            if (split != null) {
                pairs.add(split);
            }
        }
        return pairs;
    }

    private static Map<String, String> extractNormalizedPairMap(List<String> options, String combined) {
        Map<String, String> map = new LinkedHashMap<>();
        // Prefer combined expected answer if provided (especially when options hold shuffled tokens)
        if (combined != null && !combined.isBlank()) {
            for (String[] pair : extractRawPairs(List.of(), combined)) {
                String k = normalize(pair[0]);
                String v = normalize(pair[1]);
                if (!k.isEmpty() && !v.isEmpty()) {
                    map.put(k, v);
                }
            }
            if (!map.isEmpty()) {
                return map;
            }
        }
        for (String[] pair : extractRawPairs(options, null)) {
            String k = normalize(pair[0]);
            String v = normalize(pair[1]);
            if (!k.isEmpty() && !v.isEmpty()) {
                map.put(k, v);
            }
        }
        return map;
    }

    private static String[] splitPairToken(String token) {
        if (token == null || token.isBlank()) return null;
        for (String sep : new String[]{"=>", "->", "↔", "::", "="}) {
            int idx = token.indexOf(sep);
            if (idx > 0 && idx + sep.length() < token.length()) {
                String left = token.substring(0, idx).trim();
                String right = token.substring(idx + sep.length()).trim();
                if (!left.isEmpty() && !right.isEmpty()) {
                    return new String[]{left, right};
                }
            }
        }
        return null;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void notifyStudentsNewExercise(LearningExercise exercise) {
        if (exercise == null || exercise.getSchoolClass() == null) return;
        try {
            var enrollments = enrollmentRepository.findBySchoolClassIdAndStatusOrderByEnrollmentDateAsc(
                exercise.getSchoolClass().getId(), EnrollmentStatus.ACTIVE);
            List<Long> studentIds = enrollments.stream()
                .map(e -> e.getStudent().getId())
                .distinct()
                .toList();

            if (sseEmitterService != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("exerciseId", exercise.getId());
                data.put("classId", exercise.getSchoolClass().getId());
                data.put("title", exercise.getTitle());
                data.put("language", exercise.getLanguageName());
                data.put("proficiencyLevel", exercise.getProficiencyLevel());
                sseEmitterService.sendToUsers(studentIds, "EXERCISE_CREATED", data);
            }

            if (studentNotificationService != null && !studentIds.isEmpty()) {
                String className = exercise.getSchoolClass().getName() != null
                    ? exercise.getSchoolClass().getName()
                    : "sua turma";
                String message = String.format(
                    Locale.ROOT,
                    "\"%s\" foi publicado na turma %s (%s).",
                    exercise.getTitle(),
                    className,
                    exercise.getLanguageName()
                );
                for (Long studentId : studentIds) {
                    try {
                        studentNotificationService.notifyStudent(
                            studentId,
                            "Novo exercício publicado",
                            message,
                            "NEW_EXERCISE",
                            "/exercises"
                        );
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void notifyExerciseSubmitted(ExerciseSubmission submission, int xpDelta) {
        if (submission == null) return;
        try {
            LearningExercise ex = submission.getExercise();
            if (ex != null) {
                Map<Long, Teacher> recipients = new LinkedHashMap<>();
                if (ex.getTeacher() != null && ex.getTeacher().getId() != null) {
                    recipients.put(ex.getTeacher().getId(), ex.getTeacher());
                }
                if (ex.getSchoolClass() != null && ex.getSchoolClass().getId() != null && assignmentRepository != null) {
                    for (ClassTeacherAssignment cta : assignmentRepository.findBySchoolClassIdOrderByStartDateDesc(ex.getSchoolClass().getId())) {
                        if (cta.getEndDate() == null && cta.getTeacher() != null && cta.getTeacher().getId() != null) {
                            recipients.putIfAbsent(cta.getTeacher().getId(), cta.getTeacher());
                        }
                    }
                }

                String studentName = submission.getStudent() != null && submission.getStudent().getName() != null
                    ? submission.getStudent().getName()
                    : "Um aluno";
                String exTitle = ex.getTitle() != null ? ex.getTitle() : "Atividade";
                boolean needsTeacherReview = "PENDING_REVIEW".equalsIgnoreCase(submission.getReviewStatus());

                Map<String, Object> teacherData = new HashMap<>();
                teacherData.put("submissionId", submission.getId());
                teacherData.put("exerciseId", ex.getId());
                teacherData.put("exerciseTitle", exTitle);
                if (submission.getStudent() != null) {
                    teacherData.put("studentId", submission.getStudent().getId());
                }
                teacherData.put("studentName", studentName);
                teacherData.put("reviewStatus", submission.getReviewStatus());
                teacherData.put("xpEarned", submission.getXpEarned());

                String classSuffix = (ex.getSchoolClass() != null && ex.getSchoolClass().getName() != null)
                    ? " (" + ex.getSchoolClass().getName() + ")"
                    : "";
                String pendingMessage = String.format(
                    Locale.ROOT,
                    "%s respondeu \"%s\"%s e aguarda sua correção.",
                    studentName,
                    exTitle,
                    classSuffix
                );

                for (Long teacherId : recipients.keySet()) {
                    if (sseEmitterService != null) {
                        sseEmitterService.sendToUser(teacherId, "EXERCISE_SUBMITTED", teacherData);
                    }
                    if (needsTeacherReview) {
                        if (teacherNotificationService != null) {
                            try {
                                teacherNotificationService.notifyTeacher(
                                    teacherId,
                                    "Correção pendente",
                                    pendingMessage,
                                    "CORRECTION_PENDING",
                                    "/review-exercises"
                                );
                            } catch (Exception ignored) {
                            }
                        } else if (sseEmitterService != null) {
                            Map<String, Object> notifPayload = new HashMap<>();
                            notifPayload.put("teacherId", teacherId);
                            notifPayload.put("title", "Correção pendente");
                            notifPayload.put("message", pendingMessage);
                            notifPayload.put("type", "CORRECTION_PENDING");
                            notifPayload.put("actionUrl", "/review-exercises");
                            sseEmitterService.sendToUser(teacherId, "NOTIFICATION_ADDED", notifPayload);
                        }
                    }
                }
            }

            if (sseEmitterService != null && submission.getStudent() != null) {
                Map<String, Object> rankData = new HashMap<>();
                rankData.put("studentId", submission.getStudent().getId());
                rankData.put("xpDelta", xpDelta);
                if (ex != null && ex.getSchoolClass() != null) {
                    rankData.put("classId", ex.getSchoolClass().getId());
                }

                sseEmitterService.sendToUser(submission.getStudent().getId(), "RANKING_UPDATED", rankData);

                if (ex != null && ex.getSchoolClass() != null) {
                    var peers = enrollmentRepository.findBySchoolClassIdAndStatusOrderByEnrollmentDateAsc(
                        ex.getSchoolClass().getId(), EnrollmentStatus.ACTIVE);
                    List<Long> peerIds = peers.stream()
                        .map(e -> e.getStudent().getId())
                        .distinct()
                        .toList();
                    sseEmitterService.sendToUsers(peerIds, "RANKING_UPDATED", rankData);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void notifySubmissionReviewed(ExerciseSubmission submission) {
        if (submission == null || submission.getStudent() == null) return;
        try {
            Long studentId = submission.getStudent().getId();
            String exTitle = submission.getExercise() != null ? submission.getExercise().getTitle() : "Exercício";
            if (sseEmitterService != null) {
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("submissionId", submission.getId());
                studentData.put("exerciseId", submission.getExercise() != null ? submission.getExercise().getId() : null);
                studentData.put("exerciseTitle", exTitle);
                studentData.put("reviewStatus", submission.getReviewStatus());
                studentData.put("feedback", submission.getFeedback() != null ? submission.getFeedback() : "");
                studentData.put("correctCount", submission.getCorrectCount());
                studentData.put("questionCount", submission.getQuestionCount());
                studentData.put("xpEarned", submission.getXpEarned());

                sseEmitterService.sendToUser(studentId, "SUBMISSION_GRADED", studentData);
                sseEmitterService.sendToUser(studentId, "RANKING_UPDATED", Map.of(
                    "studentId", studentId,
                    "xpEarned", submission.getXpEarned()
                ));
            }
            if (studentNotificationService != null) {
                String feedbackNote = submission.getFeedback() != null && !submission.getFeedback().isBlank()
                    ? " Comentário: " + submission.getFeedback().trim()
                    : "";
                studentNotificationService.notifyStudent(
                    studentId,
                    "Correção disponível",
                    String.format(
                        Locale.ROOT,
                        "\"%s\": %d/%d acertos (+%d XP).%s",
                        exTitle,
                        submission.getCorrectCount(),
                        submission.getQuestionCount(),
                        submission.getXpEarned(),
                        feedbackNote
                    ),
                    "EXERCISE_GRADED",
                    "/exercises"
                );
            }
        } catch (Exception ignored) {
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String decomposed = java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFD);
        return decomposed
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("^[.,!?;:\"'¡¿\\s]+|[.,!?;:\"'¡¿\\s]+$", "")
            .replaceAll("\\s+", " ");
    }
}
