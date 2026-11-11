package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.InstitutionMembersResponseDTO;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

class StudentServiceTest {

    private StudentRepository studentRepository;
    private EnrollmentRepository enrollmentRepository;
    private UserRepository userRepository;
    private TeacherRepository teacherRepository;
    private InstitutionRepository institutionRepository;
    private PasswordEncoder passwordEncoder;
    private EntityManager entityManager;
    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        userRepository = mock(UserRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        institutionRepository = mock(InstitutionRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        entityManager = mock(EntityManager.class);

        studentService = new StudentService(
            studentRepository,
            enrollmentRepository,
            userRepository,
            teacherRepository,
            institutionRepository,
            passwordEncoder
        );
        ReflectionTestUtils.setField(studentService, "entityManager", entityManager);
    }

    @Test
    void create_deduplicatesLanguagesOnNewStudent() {
        Student student = new Student();
        student.setName("Alice");
        student.setEmail("alice@example.com");
        student.setPasswordHash("plainPassword123");

        StudentLanguage l1 = new StudentLanguage();
        l1.setLanguageName("Inglês");
        l1.setLevel("Básico");
        l1.setScore(10);

        StudentLanguage l2 = new StudentLanguage();
        l2.setLanguageName("  inglês  ");
        l2.setLevel("Intermediário");
        l2.setScore(20);

        StudentLanguage l3 = new StudentLanguage();
        l3.setLanguageName("Espanhol");
        l3.setLevel("Básico");
        l3.setScore(5);

        student.setLanguages(new ArrayList<>(List.of(l1, l2, l3)));

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        Student saved = studentService.create(student);

        // Inglês duplicate was merged/deduplicated; only Inglês and Espanhol remain
        assertEquals(2, saved.getLanguages().size());
        assertEquals("Inglês", saved.getLanguages().get(0).getLanguageName());
        assertEquals("Espanhol", saved.getLanguages().get(1).getLanguageName());
        assertTrue(passwordEncoder.matches("plainPassword123", saved.getPasswordHash()));
    }

    @Test
    void create_throwsWhenEmailBelongsToExistingStudent() {
        Student student = new Student();
        student.setEmail("existing@example.com");

        when(studentRepository.findByEmailIgnoreCase("existing@example.com"))
            .thenReturn(Optional.of(new Student()));

        assertThrows(BusinessException.class, () -> studentService.create(student));
    }

    @Test
    void create_convertsUserWithExistingPassword_requiresMatchingPassword() {
        User existingUser = new User();
        existingUser.setId(99L);
        existingUser.setEmail("user@example.com");
        existingUser.setPasswordHash(passwordEncoder.encode("secretPassword"));

        Student student = new Student();
        student.setEmail("user@example.com");
        student.setPasswordHash("wrongPassword");

        when(studentRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existingUser));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> studentService.create(student)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void update_preservesExistingScoresAndDeduplicatesIncomingLanguages() {
        Student current = new Student();
        current.setId(5L);
        current.setName("Bob");
        current.setEmail("bob@example.com");

        StudentLanguage existingLang = new StudentLanguage();
        existingLang.setLanguageName("Inglês");
        existingLang.setScore(150);
        existingLang.setLevel("Intermediário");
        current.addLanguage(existingLang);

        when(studentRepository.findById(5L)).thenReturn(Optional.of(current));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        Student updateData = new Student();
        updateData.setId(5L);
        updateData.setName("Bob Updated");
        updateData.setEmail("bob@example.com");

        StudentLanguage incoming1 = new StudentLanguage();
        incoming1.setLanguageName("Inglês");
        incoming1.setLevel("Avançado");
        incoming1.setScore(0); // Score was 0 in request, should preserve 150

        StudentLanguage incoming2 = new StudentLanguage();
        incoming2.setLanguageName("inglês"); // Duplicate in request
        incoming2.setLevel("Avançado");
        incoming2.setScore(0);

        StudentLanguage incoming3 = new StudentLanguage();
        incoming3.setLanguageName("Francês");
        incoming3.setLevel("Básico");
        incoming3.setScore(50);

        updateData.setLanguages(new ArrayList<>(List.of(incoming1, incoming2, incoming3)));

        Student saved = studentService.update(updateData);

        assertEquals(2, saved.getLanguages().size());
        assertEquals("Inglês", saved.getLanguages().get(0).getLanguageName());
        assertEquals(150, saved.getLanguages().get(0).getScore());
        assertEquals("Francês", saved.getLanguages().get(1).getLanguageName());
        assertEquals(50, saved.getLanguages().get(1).getScore());
    }

    @Test
    void update_throwsWhenEmailBelongsToAnotherAccount() {
        Student current = new Student();
        current.setId(5L);
        current.setEmail("bob@example.com");

        User otherUser = new User();
        otherUser.setId(6L);
        otherUser.setEmail("taken@example.com");

        when(studentRepository.findById(5L)).thenReturn(Optional.of(current));
        when(userRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(otherUser));

        Student updateData = new Student();
        updateData.setId(5L);
        updateData.setEmail("taken@example.com");

        assertThrows(BusinessException.class, () -> studentService.update(updateData));
    }
}
