package com.duolinfo.ia.proj.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.StudentVocabulary;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.StudentVocabularyRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
public class DatabasePersistenceAndConcurrencyIntegrationTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentLanguageRepository studentLanguageRepository;

    @Autowired
    private StudentVocabularyRepository studentVocabularyRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Garante controle de concorrência com Optimistic Locking (@Version) em Student")
    void testOptimisticLockingOnStudent() {
        Student student = new Student();
        student.setName("Student Lock Test");
        student.setEmail("locktest_" + System.currentTimeMillis() + "@test.com");
        student.setCurrentStreak(5);
        Student saved = studentRepository.saveAndFlush(student);
        Long id = saved.getId();
        Long initialVersion = saved.getVersion();
        assertNotNull(initialVersion);

        // Simula primeira transação modificando e commitando
        Student s1 = studentRepository.findById(id).orElseThrow();
        s1.setCurrentStreak(10);
        studentRepository.saveAndFlush(s1);

        // Simula segunda transação concorrente tentando salvar o registro com a versão anterior (desatualizada)
        saved.setCurrentStreak(15);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            studentRepository.saveAndFlush(saved);
        });
    }

    @Test
    @DisplayName("Garante integridade referencial com constraint única impedindo idioma duplicado para o mesmo aluno")
    void testUniqueConstraintPreventsDuplicateStudentLanguage() {
        Student student = new Student();
        student.setName("Language Uniqueness Test");
        student.setEmail("langunique_" + System.currentTimeMillis() + "@test.com");
        Student savedStudent = studentRepository.saveAndFlush(student);

        StudentLanguage lang1 = StudentLanguage.builder()
            .student(savedStudent)
            .languageName("Inglês")
            .level("A1")
            .score(100)
            .build();
        studentLanguageRepository.saveAndFlush(lang1);

        StudentLanguage lang2 = StudentLanguage.builder()
            .student(savedStudent)
            .languageName("Inglês")
            .level("B1")
            .score(200)
            .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            studentLanguageRepository.saveAndFlush(lang2);
        });
    }

    @Test
    @DisplayName("Garante integridade de vocabulário com constraint única impedindo termo duplicado para mesmo aluno e idioma")
    void testUniqueConstraintPreventsDuplicateStudentVocabulary() {
        Student student = new Student();
        student.setName("Vocab Uniqueness Test");
        student.setEmail("vocabunique_" + System.currentTimeMillis() + "@test.com");
        Student savedStudent = studentRepository.saveAndFlush(student);

        StudentVocabulary v1 = StudentVocabulary.builder()
            .student(savedStudent)
            .language("Inglês")
            .term("Book")
            .translation("Livro")
            .build();
        studentVocabularyRepository.saveAndFlush(v1);

        StudentVocabulary v2 = StudentVocabulary.builder()
            .student(savedStudent)
            .language("Inglês")
            .term("Book")
            .translation("Outro Livro")
            .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            studentVocabularyRepository.saveAndFlush(v2);
        });
    }

    @Test
    @DisplayName("Valida busca otimizada de matrículas com JOIN FETCH de aluno e idiomas sem queries N+1")
    void testEagerFetchEnrollmentsWithStudentAndLanguages() {
        long timestamp = System.currentTimeMillis();
        Institution inst = institutionRepository.saveAndFlush(Institution.builder()
            .name("Eager Inst " + timestamp)
            .code("EAG" + timestamp)
            .email("eager_" + timestamp + "@test.com")
            .active(true)
            .build());

        School school = schoolRepository.saveAndFlush(School.builder()
            .name("Eager School")
            .institution(inst)
            .build());

        SchoolClass sc = schoolClassRepository.saveAndFlush(SchoolClass.builder()
            .name("Eager Class")
            .school(school)
            .institution(inst)
            .build());

        Student student = new Student();
        student.setName("Eager Student");
        student.setEmail("eager_st_" + timestamp + "@test.com");
        student.setInstitution(inst);
        Student savedStudent = studentRepository.saveAndFlush(student);

        StudentLanguage lang = StudentLanguage.builder()
            .student(savedStudent)
            .languageName("Inglês")
            .level("B2")
            .score(500)
            .build();
        studentLanguageRepository.saveAndFlush(lang);

        Enrollment enrollment = Enrollment.builder()
            .student(savedStudent)
            .schoolClass(sc)
            .enrollmentDate(LocalDate.now())
            .status(EnrollmentStatus.ACTIVE)
            .build();
        enrollmentRepository.saveAndFlush(enrollment);

        entityManager.clear(); // Limpa o contexto de persistência para forçar nova consulta ao banco

        List<Enrollment> list = enrollmentRepository.findActiveWithStudentAndLanguagesBySchoolClassId(sc.getId(), EnrollmentStatus.ACTIVE);
        assertFalse(list.isEmpty());
        Enrollment loaded = list.get(0);
        assertNotNull(loaded.getStudent());
        assertEquals("Eager Student", loaded.getStudent().getName());
        assertNotNull(loaded.getStudent().getLanguages());
        assertEquals(1, loaded.getStudent().getLanguages().size());
        assertEquals("Inglês", loaded.getStudent().getLanguages().get(0).getLanguageName());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Valida busca de professores com @EntityGraph carregando idiomas lecionados e áreas sem N+1")
    void testEagerFetchTeachersWithDetails() {
        long timestamp = System.currentTimeMillis();
        Institution inst = institutionRepository.saveAndFlush(Institution.builder()
            .name("Teacher Inst " + timestamp)
            .code("TCH" + timestamp)
            .email("tcheager_" + timestamp + "@test.com")
            .active(true)
            .build());

        Teacher teacher = new Teacher();
        teacher.setName("Prof Eager");
        teacher.setEmail("profeager_" + timestamp + "@test.com");
        teacher.setInstitution(inst);
        teacher.addTaughtLanguage("Francês");
        teacher.addSpecializationArea("Linguística");
        teacherRepository.saveAndFlush(teacher);

        entityManager.clear();

        List<Teacher> teachers = teacherRepository.findWithDetailsByInstitutionId(inst.getId());
        assertFalse(teachers.isEmpty());
        Teacher loaded = teachers.get(0);
        assertEquals(1, loaded.getTaughtLanguages().size());
        assertTrue(loaded.getTaughtLanguages().contains("Francês"));
        assertEquals(1, loaded.getSpecializationAreas().size());
        assertTrue(loaded.getSpecializationAreas().contains("Linguística"));
    }

    @Test
    @DisplayName("Valida rollback transacional automático em caso de exceção durante operações de persistência")
    void testTransactionalRollbackOnFailure() {
        long timestamp = System.currentTimeMillis();
        String studentEmail = "rollback_" + timestamp + "@test.com";

        assertThrows(RuntimeException.class, () -> {
            transactionTemplate.execute(status -> {
                Student s = new Student();
                s.setName("Rollback Student");
                s.setEmail(studentEmail);
                studentRepository.save(s);
                throw new RuntimeException("Forced rollback simulation");
            });
        });

        assertFalse(studentRepository.findByEmail(studentEmail).isPresent());
    }
}
