package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.dto.request.ExerciseQuestionRequestDTO;
import com.duolinfo.ia.proj.dto.request.ExerciseSubmissionRequestDTO;
import com.duolinfo.ia.proj.dto.request.LearningExerciseRequestDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO;
import com.duolinfo.ia.proj.dto.response.LearningExerciseResponseDTO;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

class LearningExerciseServiceTest {

    private ObjectMapper objectMapper;
    private LearningExerciseRepository exerciseRepository;
    private ExerciseSubmissionRepository submissionRepository;
    private TeacherRepository teacherRepository;
    private StudentRepository studentRepository;
    private SchoolClassRepository schoolClassRepository;
    private EnrollmentRepository enrollmentRepository;
    private ClassTeacherAssignmentRepository assignmentRepository;
    private AchievementService achievementService;
    private LearningExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exerciseRepository = mock(LearningExerciseRepository.class);
        submissionRepository = mock(ExerciseSubmissionRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        studentRepository = mock(StudentRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        assignmentRepository = mock(ClassTeacherAssignmentRepository.class);
        achievementService = mock(AchievementService.class);

        exerciseService = new LearningExerciseService(
            objectMapper,
            exerciseRepository,
            submissionRepository,
            teacherRepository,
            studentRepository,
            schoolClassRepository,
            enrollmentRepository,
            assignmentRepository,
            achievementService,
            null
        );
    }

    @Test
    void create_rejectsDueDateInThePast() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setEmail("teacher@example.com");

        when(teacherRepository.findByEmailIgnoreCase("teacher@example.com"))
            .thenReturn(Optional.of(teacher));

        LearningExerciseRequestDTO request = new LearningExerciseRequestDTO(
            "Exercício Passado",
            "Gramática",
            "Verbos",
            "Inglês",
            "A1",
            "Descrição",
            "Conteúdo",
            null,
            LocalDate.now().minusDays(1), // Past due date!
            10L,
            List.of()
        );

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> exerciseService.create(request, "teacher@example.com")
        );
        assertTrue(ex.getMessage().contains("data de entrega não pode ser anterior à data atual"));
    }

    @Test
    void create_rejectsWhenContentAndQuestionsBothEmpty() {
        Teacher teacher = new Teacher();
        teacher.setEmail("teacher@example.com");

        when(teacherRepository.findByEmailIgnoreCase("teacher@example.com"))
            .thenReturn(Optional.of(teacher));

        LearningExerciseRequestDTO request = new LearningExerciseRequestDTO(
            "Exercício Vazio",
            null,
            null,
            "Inglês",
            "A1",
            null,
            "   ",
            null,
            LocalDate.now().plusDays(5),
            10L,
            List.of()
        );

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> exerciseService.create(request, "teacher@example.com")
        );
        assertTrue(ex.getMessage().contains("Informe o conteúdo teórico ou adicione ao menos uma questão"));
    }

    @Test
    void create_rejectsClassFromDifferentInstitution() {
        Institution instA = new Institution();
        instA.setId(10L);

        Institution instB = new Institution();
        instB.setId(20L);

        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setEmail("teacher@example.com");
        teacher.setInstitution(instA);

        School school = new School();
        school.setInstitution(instB); // Different institution

        SchoolClass sc = new SchoolClass();
        sc.setId(100L);
        sc.setSchool(school);

        when(teacherRepository.findByEmailIgnoreCase("teacher@example.com"))
            .thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findById(100L)).thenReturn(Optional.of(sc));

        ExerciseQuestionRequestDTO q = new ExerciseQuestionRequestDTO(
            "MULTIPLE_CHOICE",
            "Q1",
            List.of("A", "B"),
            "A",
            "A"
        );

        LearningExerciseRequestDTO request = new LearningExerciseRequestDTO(
            "Exercício Válido",
            "Tema",
            "Subtema",
            "Inglês",
            "A1",
            "Desc",
            null,
            null,
            LocalDate.now().plusDays(2),
            100L,
            List.of(q)
        );

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> exerciseService.create(request, "teacher@example.com")
        );
        assertTrue(ex.getMessage().contains("só pode criar exercícios para turmas da sua instituição"));
    }

    @Test
    void submit_calculatesScoreAndAwardsXp() {
        Student student = new Student();
        student.setId(5L);
        student.setEmail("student@example.com");

        StudentLanguage studentLang = new StudentLanguage();
        studentLang.setLanguageName("Inglês");
        studentLang.setScore(100);
        student.setLanguages(new ArrayList<>(List.of(studentLang)));

        SchoolClass sc = new SchoolClass();
        sc.setId(10L);

        Enrollment enrollment = Enrollment.builder()
            .student(student)
            .schoolClass(sc)
            .status(EnrollmentStatus.ACTIVE)
            .build();

        ExerciseQuestionRequestDTO q1 = new ExerciseQuestionRequestDTO(
            "MULTIPLE_CHOICE",
            "What is 'livro' in English?",
            List.of("Book", "Pen", "Table"),
            "Book",
            "Book"
        );

        LearningExercise exercise = new LearningExercise();
        exercise.setId(50L);
        exercise.setLanguageName("Inglês");
        exercise.setSchoolClass(sc);
        exercise.setActive(true);
        exercise.setQuestionsJson("[{\"style\":\"MULTIPLE_CHOICE\",\"question\":\"What is 'livro' in English?\",\"options\":[\"Book\",\"Pen\",\"Table\"],\"correctAnswer\":\"Book\",\"expectedAnswer\":\"Book\"}]");

        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(exerciseRepository.findById(50L)).thenReturn(Optional.of(exercise));
        when(enrollmentRepository.findByStudentIdAndStatus(5L, EnrollmentStatus.ACTIVE)).thenReturn(List.of(enrollment));
        when(submissionRepository.findByStudent_IdAndExercise_IdOrderBySubmittedAtDesc(5L, 50L))
            .thenReturn(List.of());
        when(submissionRepository.save(any(ExerciseSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseSubmissionResponseDTO response = exerciseService.submit(50L, List.of("Book"), "student@example.com");

        assertEquals(1, response.questionCount());
        assertEquals(1, response.correctCount());
        assertTrue(response.xpEarned() > 0);
        assertTrue(studentLang.getScore() > 100);
        verify(studentRepository).save(student);
    }
}
