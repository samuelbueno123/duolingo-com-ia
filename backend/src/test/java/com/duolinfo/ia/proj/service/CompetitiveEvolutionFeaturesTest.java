package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.dto.ai.ExplainMistakeRequestDTO;
import com.duolinfo.ia.proj.dto.ai.ExplainMistakeResponseDTO;
import com.duolinfo.ia.proj.dto.request.ClassAnnouncementRequestDTO;
import com.duolinfo.ia.proj.dto.request.CloneExerciseRequestDTO;
import com.duolinfo.ia.proj.dto.request.VocabularyRequestDTO;
import com.duolinfo.ia.proj.dto.request.TeacherStudentNoteRequestDTO;
import com.duolinfo.ia.proj.dto.response.ClassAnalyticsResponseDTO;
import com.duolinfo.ia.proj.dto.response.ClassAnnouncementResponseDTO;
import com.duolinfo.ia.proj.dto.response.StreakResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentMistakeResponseDTO;
import com.duolinfo.ia.proj.dto.response.TeacherStudentNoteResponseDTO;
import com.duolinfo.ia.proj.dto.response.VocabularyResponseDTO;
import com.duolinfo.ia.proj.entity.ClassAnnouncement;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentMistake;
import com.duolinfo.ia.proj.entity.StudentNotification;
import com.duolinfo.ia.proj.entity.StudentVocabulary;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.TeacherStudentNote;
import com.duolinfo.ia.proj.repository.ClassAnnouncementRepository;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentMistakeRepository;
import com.duolinfo.ia.proj.repository.StudentNotificationRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.StudentVocabularyRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.TeacherStudentNoteRepository;

class CompetitiveEvolutionFeaturesTest {

    private StudentRepository studentRepository;
    private TeacherRepository teacherRepository;
    private SchoolClassRepository schoolClassRepository;
    private EnrollmentRepository enrollmentRepository;
    private LearningExerciseRepository exerciseRepository;
    private ExerciseSubmissionRepository submissionRepository;
    private ClassTeacherAssignmentRepository assignmentRepository;
    private StudentMistakeRepository mistakeRepository;
    private StudentVocabularyRepository vocabularyRepository;
    private StudentNotificationRepository notificationRepository;
    private ClassAnnouncementRepository announcementRepository;
    private TeacherStudentNoteRepository noteRepository;
    private SseEmitterService sseEmitterService;

    private StudentStreakService streakService;
    private StudentMistakeService mistakeService;
    private StudentNotificationService notificationService;
    private ClassAnnouncementService announcementService;
    private ClassAnalyticsService analyticsService;
    private StudentVocabularyService vocabularyService;
    private TeacherStudentNoteService noteService;
    private AiExplanationService aiExplanationService;
    private LearningExerciseService learningExerciseService;
    private AchievementService achievementService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        exerciseRepository = mock(LearningExerciseRepository.class);
        submissionRepository = mock(ExerciseSubmissionRepository.class);
        assignmentRepository = mock(ClassTeacherAssignmentRepository.class);
        mistakeRepository = mock(StudentMistakeRepository.class);
        vocabularyRepository = mock(StudentVocabularyRepository.class);
        notificationRepository = mock(StudentNotificationRepository.class);
        announcementRepository = mock(ClassAnnouncementRepository.class);
        noteRepository = mock(TeacherStudentNoteRepository.class);
        sseEmitterService = mock(SseEmitterService.class);
        achievementService = mock(AchievementService.class);
        objectMapper = new ObjectMapper();

        streakService = new StudentStreakService(studentRepository);
        mistakeService = new StudentMistakeService(mistakeRepository, studentRepository, streakService);
        notificationService = new StudentNotificationService(notificationRepository, studentRepository, sseEmitterService);
        announcementService = new ClassAnnouncementService(
            announcementRepository, schoolClassRepository, teacherRepository,
            enrollmentRepository, notificationService
        );
        analyticsService = new ClassAnalyticsService(
            schoolClassRepository, enrollmentRepository, exerciseRepository, submissionRepository
        );
        vocabularyService = new StudentVocabularyService(vocabularyRepository, studentRepository);
        noteService = new TeacherStudentNoteService(noteRepository, teacherRepository, studentRepository, schoolClassRepository);
        aiExplanationService = new AiExplanationService(objectMapper, "", "http://localhost", "gemini-model");

        learningExerciseService = new LearningExerciseService(
            objectMapper, exerciseRepository, submissionRepository, teacherRepository,
            studentRepository, schoolClassRepository, enrollmentRepository, assignmentRepository,
            achievementService, sseEmitterService
        );
        learningExerciseService.setStudentMistakeService(mistakeService);
        learningExerciseService.setStudentStreakService(streakService);

        when(notificationRepository.save(any(StudentNotification.class))).thenAnswer(inv -> {
            StudentNotification sn = inv.getArgument(0);
            sn.setId(99L);
            return sn;
        });
    }

    @Test
    void testStreakServiceIncrementsAndFreezes() {
        Student student = new Student();
        student.setId(1L);
        student.setCurrentStreak(2);
        student.setLongestStreak(5);
        student.setStreakFreezes(2);
        student.setDailyXpGoal(50);
        student.setLastActiveDate(LocalDate.now().minusDays(1));

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StreakResponseDTO response = streakService.recordActivity(1L, 30);
        assertEquals(3, response.currentStreak());
        assertEquals(30, response.todayXpEarned());
        assertFalse(response.goalReached());

        // Second activity pushes over goal
        StreakResponseDTO response2 = streakService.recordActivity(1L, 25);
        assertEquals(3, response2.currentStreak());
        assertEquals(55, response2.todayXpEarned());
        assertTrue(response2.goalReached());
    }

    @Test
    void testMistakeRecordingAndReviewMastery() {
        Student student = new Student();
        student.setId(10L);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        when(mistakeRepository.save(any(StudentMistake.class))).thenAnswer(inv -> {
            StudentMistake m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        StudentMistakeResponseDTO dto = mistakeService.recordMistake(
            10L, 50L, "How do you say thank you?", "De nada", "Obrigado", "De nada means you're welcome"
        );
        assertNotNull(dto);
        assertEquals("How do you say thank you?", dto.question());
        assertEquals("De nada", dto.userWrongAnswer());
        assertFalse(dto.resolved());

        StudentMistake mistake = StudentMistake.builder()
            .id(100L)
            .student(student)
            .question(dto.question())
            .consecutiveCorrect(1)
            .resolved(false)
            .timesReviewed(1)
            .build();
        when(mistakeRepository.findByIdAndStudent_Id(100L, 10L)).thenReturn(Optional.of(mistake));

        StudentMistakeResponseDTO reviewed = mistakeService.reviewMistake(10L, 100L, true);
        assertEquals(2, reviewed.consecutiveCorrect());
        assertTrue(reviewed.resolved());
    }

    @Test
    void testClassAnnouncementsAndBroadcast() {
        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setName("Prof. Helena");

        SchoolClass sc = new SchoolClass();
        sc.setId(5L);
        sc.setName("Inglês Avançado");

        Student student = new Student();
        student.setId(20L);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findById(5L)).thenReturn(Optional.of(sc));
        when(enrollmentRepository.findBySchoolClassIdAndStatus(5L, EnrollmentStatus.ACTIVE))
            .thenReturn(List.of(enrollment));
        when(studentRepository.findById(20L)).thenReturn(Optional.of(student));

        when(announcementRepository.save(any(ClassAnnouncement.class))).thenAnswer(inv -> {
            ClassAnnouncement a = inv.getArgument(0);
            a.setId(11L);
            return a;
        });

        ClassAnnouncementRequestDTO request = new ClassAnnouncementRequestDTO(
            "Prova Semestral", "A prova será realizada na sexta-feira.", "HIGH", true
        );

        ClassAnnouncementResponseDTO resp = announcementService.createAnnouncement(2L, 5L, request);
        assertNotNull(resp);
        assertEquals("Prova Semestral", resp.title());
        assertTrue(resp.pinned());
        assertEquals("HIGH", resp.priority());
    }

    @Test
    void testClassAnalyticsCalculatesMetrics() {
        SchoolClass sc = new SchoolClass();
        sc.setId(7L);
        sc.setName("Turma Espanhol B1");

        Student s1 = new Student();
        s1.setId(101L);
        s1.setName("Carlos");

        Enrollment e1 = new Enrollment();
        e1.setStudent(s1);
        e1.setStatus(EnrollmentStatus.ACTIVE);

        LearningExercise ex1 = new LearningExercise();
        ex1.setId(1L);
        ex1.setTitle("Subjuntivo 1");

        ExerciseSubmission sub1 = new ExerciseSubmission();
        sub1.setStudent(s1);
        sub1.setExercise(ex1);
        sub1.setCorrectCount(8);
        sub1.setQuestionCount(10);
        sub1.setSubmittedAt(Instant.now());

        when(schoolClassRepository.findById(7L)).thenReturn(Optional.of(sc));
        when(enrollmentRepository.findBySchoolClassIdAndStatus(7L, EnrollmentStatus.ACTIVE))
            .thenReturn(List.of(e1));
        when(exerciseRepository.findBySchoolClass_IdOrderByCreatedAtDesc(7L))
            .thenReturn(List.of(ex1));
        when(submissionRepository.findByExercise_SchoolClass_IdOrderBySubmittedAtDesc(7L))
            .thenReturn(List.of(sub1));

        ClassAnalyticsResponseDTO analytics = analyticsService.getClassAnalytics(7L);
        assertNotNull(analytics);
        assertEquals(1, analytics.totalStudents());
        assertEquals(1, analytics.totalSubmissions());
        assertEquals(80.0, analytics.averageAccuracyPercent());
        assertEquals(0, analytics.studentsNeedingAttention().size());
    }

    @Test
    void testVocabularyVaultOperations() {
        Student student = new Student();
        student.setId(30L);
        when(studentRepository.findById(30L)).thenReturn(Optional.of(student));

        when(vocabularyRepository.findByStudent_IdAndLanguageIgnoreCaseOrderByCreatedAtDesc(30L, "Alemão"))
            .thenReturn(List.of());
        when(vocabularyRepository.save(any(StudentVocabulary.class))).thenAnswer(inv -> {
            StudentVocabulary v = inv.getArgument(0);
            v.setId(55L);
            return v;
        });

        VocabularyRequestDTO request = new VocabularyRequestDTO(
            "Alemão", "das Buch", "o livro", "SUBSTANTIVO", "Ich lese das Buch.", "LEARNING"
        );
        VocabularyResponseDTO created = vocabularyService.addWord(30L, request);
        assertNotNull(created);
        assertEquals("das Buch", created.term());
        assertEquals("o livro", created.translation());

        StudentVocabulary saved = StudentVocabulary.builder()
            .id(55L)
            .student(student)
            .language("Alemão")
            .term("das Buch")
            .translation("o livro")
            .masteryLevel("LEARNING")
            .timesPracticed(0)
            .build();
        when(vocabularyRepository.findByIdAndStudent_Id(55L, 30L)).thenReturn(Optional.of(saved));

        VocabularyResponseDTO updated = vocabularyService.updateMastery(30L, 55L, "MASTERED");
        assertEquals("MASTERED", updated.masteryLevel());
        assertEquals(1, updated.timesPracticed());
    }

    @Test
    void testTeacherStudentNotes() {
        Teacher teacher = new Teacher();
        teacher.setId(3L);
        teacher.setName("Prof. Carlos");

        Student student = new Student();
        student.setId(40L);
        student.setName("Mariana");

        when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher));
        when(studentRepository.findById(40L)).thenReturn(Optional.of(student));
        when(noteRepository.save(any(TeacherStudentNote.class))).thenAnswer(inv -> {
            TeacherStudentNote n = inv.getArgument(0);
            n.setId(88L);
            return n;
        });

        TeacherStudentNoteRequestDTO req = new TeacherStudentNoteRequestDTO(
            40L, null, "Excelente pronúncia, revisar uso de preposições em inglês."
        );
        TeacherStudentNoteResponseDTO noteResp = noteService.saveNote(3L, req);
        assertNotNull(noteResp);
        assertEquals(40L, noteResp.studentId());
        assertEquals("Excelente pronúncia, revisar uso de preposições em inglês.", noteResp.note());
    }

    @Test
    void testAiExplanationFallback() {
        ExplainMistakeRequestDTO req = new ExplainMistakeRequestDTO(
            "Qual o passado de to go?", "goed", "went", "Inglês"
        );
        ExplainMistakeResponseDTO response = aiExplanationService.explainMistake(req);
        assertNotNull(response);
        assertNotNull(response.explanation());
        assertTrue(response.explanation().contains("went"));
        assertNotNull(response.grammarRule());
        assertNotNull(response.memoryTip());
        assertFalse(response.similarExamples().isEmpty());
    }
}
