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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.repository.AdminNotificationRepository;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;

import jakarta.persistence.EntityManager;

class TeacherServiceTest {

    private TeacherRepository teacherRepository;
    private ClassTeacherAssignmentRepository assignmentRepository;
    private InstitutionRepository institutionRepository;
    private UserRepository userRepository;
    private StudentRepository studentRepository;
    private AdminNotificationRepository notificationRepository;
    private PasswordEncoder passwordEncoder;
    private EntityManager entityManager;
    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherRepository = mock(TeacherRepository.class);
        assignmentRepository = mock(ClassTeacherAssignmentRepository.class);
        institutionRepository = mock(InstitutionRepository.class);
        userRepository = mock(UserRepository.class);
        studentRepository = mock(StudentRepository.class);
        notificationRepository = mock(AdminNotificationRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        entityManager = mock(EntityManager.class);

        teacherService = new TeacherService(
            teacherRepository,
            assignmentRepository,
            institutionRepository,
            userRepository,
            studentRepository,
            notificationRepository,
            passwordEncoder
        );
        ReflectionTestUtils.setField(teacherService, "entityManager", entityManager);
    }

    @Test
    void create_deduplicatesTaughtLanguagesAndSpecializations() {
        Teacher teacher = new Teacher();
        teacher.setName("Prof. Carlos");
        teacher.setEmail("carlos@example.com");
        teacher.setPasswordHash("pass123");
        teacher.setTaughtLanguages(new ArrayList<>(List.of("Inglês", "  inglês  ", "Inglês", "Espanhol")));
        teacher.setSpecializationAreas(new ArrayList<>(List.of("Gramática", "Gramática", "Conversação")));

        Institution inst = new Institution();
        inst.setId(1L);

        when(userRepository.findByEmailIgnoreCase("carlos@example.com")).thenReturn(Optional.empty());
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(inst));
        when(teacherRepository.countByInstitutionIdAndEmailNotIgnoreCase(1L, "admin@duolingo.com")).thenReturn(1L); // Not first teacher
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        Teacher saved = teacherService.create(1L, teacher);

        assertEquals(2, saved.getTaughtLanguages().size());
        assertEquals("Inglês", saved.getTaughtLanguages().get(0));
        assertEquals("Espanhol", saved.getTaughtLanguages().get(1));

        assertEquals(2, saved.getSpecializationAreas().size());
        assertEquals("Gramática", saved.getSpecializationAreas().get(0));
        assertEquals("Conversação", saved.getSpecializationAreas().get(1));
    }

    @Test
    void create_firstTeacherInInstitutionBecomesPrimaryAdmin() {
        Teacher teacher = new Teacher();
        teacher.setName("Prof. Founder");
        teacher.setEmail("founder@example.com");
        teacher.setPasswordHash("pass123");

        Institution inst = new Institution();
        inst.setId(10L);
        inst.setCode(InstitutionService.DEFAULT_INSTITUTION_CODE);

        when(userRepository.findByEmailIgnoreCase("founder@example.com")).thenReturn(Optional.empty());
        when(institutionRepository.findById(10L)).thenReturn(Optional.of(inst));
        when(teacherRepository.countByInstitutionIdAndEmailNotIgnoreCase(10L, "admin@duolingo.com")).thenReturn(0L); // First teacher!
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        Teacher saved = teacherService.create(10L, teacher);

        assertTrue(Boolean.TRUE.equals(saved.getApproved()));
        assertTrue(saved.isInstitutionAdmin());
        assertTrue(saved.isPrimaryInstitutionAdmin());
    }

    @Test
    void updateApproval_superAdminApprovalCannotBeRevoked() {
        Teacher superAdmin = new Teacher();
        superAdmin.setId(1L);
        superAdmin.setEmail("admin@duolingo.com");
        superAdmin.setAdmin(true);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> teacherService.updateApproval(1L, false)
        );
        assertTrue(ex.getMessage().contains("O administrador geral não pode ter a aprovação revogada"));
    }

    @Test
    void updateApproval_primaryInstitutionAdminCannotBeRevoked() {
        Teacher primary = new Teacher();
        primary.setId(2L);
        primary.setEmail("founder@school.com");
        primary.setPrimaryInstitutionAdmin(true);

        when(teacherRepository.findById(2L)).thenReturn(Optional.of(primary));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> teacherService.updateApproval(2L, false)
        );
        assertTrue(ex.getMessage().contains("O administrador fundador da instituição não pode ser revogado"));
    }

    @Test
    void update_deduplicatesLanguagesAndSpecializations() {
        Teacher current = new Teacher();
        current.setId(5L);
        current.setName("Teacher");
        current.setEmail("teacher@example.com");

        when(teacherRepository.findById(5L)).thenReturn(Optional.of(current));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        Teacher data = new Teacher();
        data.setName("Teacher Renamed");
        data.setEmail("teacher@example.com");
        data.setTaughtLanguages(List.of("Alemão", "Alemão", "Francês"));
        data.setSpecializationAreas(List.of("Fonética", "  Fonética  ", "Literatura"));

        Teacher updated = teacherService.update(5L, null, data);

        assertEquals(2, updated.getTaughtLanguages().size());
        assertEquals("Alemão", updated.getTaughtLanguages().get(0));
        assertEquals("Francês", updated.getTaughtLanguages().get(1));

        assertEquals(2, updated.getSpecializationAreas().size());
        assertEquals("Fonética", updated.getSpecializationAreas().get(0));
        assertEquals("Literatura", updated.getSpecializationAreas().get(1));
    }
}
