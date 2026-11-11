package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

class ClassTeacherAssignmentAndInstitutionServiceTest {

    private ClassTeacherAssignmentRepository assignmentRepository;
    private SchoolClassRepository schoolClassRepository;
    private TeacherRepository teacherRepository;
    private ClassTeacherAssignmentService assignmentService;

    private InstitutionRepository institutionRepository;
    private SchoolRepository schoolRepository;
    private StudentRepository studentRepository;
    private InstitutionService institutionService;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(ClassTeacherAssignmentRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        teacherRepository = mock(TeacherRepository.class);

        assignmentService = new ClassTeacherAssignmentService(
            assignmentRepository,
            schoolClassRepository,
            teacherRepository
        );

        institutionRepository = mock(InstitutionRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        studentRepository = mock(StudentRepository.class);

        institutionService = new InstitutionService(
            institutionRepository,
            schoolRepository,
            teacherRepository,
            studentRepository
        );
    }

    @Test
    void assignment_throwsWhenInstitutionsDoNotMatch() {
        Institution instA = new Institution();
        instA.setId(1L);

        Institution instB = new Institution();
        instB.setId(2L);

        School school = new School();
        school.setInstitution(instA);

        SchoolClass sc = new SchoolClass();
        sc.setId(10L);
        sc.setSchool(school);

        Teacher teacher = new Teacher();
        teacher.setId(20L);
        teacher.setInstitution(instB); // Differs from class institution

        when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(sc));
        when(teacherRepository.findById(20L)).thenReturn(Optional.of(teacher));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> assignmentService.assignTeacher(10L, 20L, LocalDate.now(), "Atribuição")
        );
        assertTrue(ex.getMessage().contains("O professor não pertence à mesma instituição da turma"));
    }

    @Test
    void assignment_npeSafetyWhenClassOrTeacherHasNoInstitution() {
        SchoolClass sc = new SchoolClass();
        sc.setId(10L);
        sc.setSchool(null); // No school link
        sc.setInstitution(null);

        Teacher teacher = new Teacher();
        teacher.setId(20L);
        teacher.setInstitution(null); // No institution link

        when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(sc));
        when(teacherRepository.findById(20L)).thenReturn(Optional.of(teacher));

        // Must not crash with NullPointerException, but throw BusinessException
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> assignmentService.assignTeacher(10L, 20L, LocalDate.now(), "Atribuição")
        );
        assertTrue(ex.getMessage().contains("O professor não pertence à mesma instituição da turma"));
    }

    @Test
    void assignment_dateOrderingValidationWhenCurrentTeacherExists() {
        Institution inst = new Institution();
        inst.setId(1L);

        School school = new School();
        school.setInstitution(inst);

        SchoolClass sc = new SchoolClass();
        sc.setId(10L);
        sc.setSchool(school);

        Teacher teacher1 = new Teacher();
        teacher1.setId(20L);
        teacher1.setInstitution(inst);

        Teacher teacher2 = new Teacher();
        teacher2.setId(30L);
        teacher2.setInstitution(inst);

        ClassTeacherAssignment current = ClassTeacherAssignment.builder()
            .id(1L)
            .schoolClass(sc)
            .teacher(teacher1)
            .startDate(LocalDate.of(2026, 3, 1))
            .build();

        when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(sc));
        when(teacherRepository.findById(30L)).thenReturn(Optional.of(teacher2));
        when(assignmentRepository.findBySchoolClassIdAndEndDateIsNull(10L)).thenReturn(Optional.of(current));

        // Assignment date before or equal to current teacher start date throws
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> assignmentService.assignTeacher(10L, 30L, LocalDate.of(2026, 2, 1), "Antes do início")
        );
        assertTrue(ex.getMessage().contains("deve ser posterior à data de início da atribuição atual"));
    }

    @Test
    void institution_createValidatesUniqueCodeEmailPhone() {
        Institution inst = new Institution();
        inst.setCode("INST-1");
        inst.setEmail("inst@edu.br");
        inst.setPhone("1199999999");

        when(institutionRepository.existsByCode("INST-1")).thenReturn(true);
        assertThrows(BusinessException.class, () -> institutionService.create(inst));

        when(institutionRepository.existsByCode("INST-1")).thenReturn(false);
        when(institutionRepository.existsByEmail("inst@edu.br")).thenReturn(true);
        assertThrows(BusinessException.class, () -> institutionService.create(inst));

        when(institutionRepository.existsByEmail("inst@edu.br")).thenReturn(false);
        when(institutionRepository.existsByPhone("1199999999")).thenReturn(true);
        assertThrows(BusinessException.class, () -> institutionService.create(inst));
    }

    @Test
    void institution_deletePreventsDeletionWithSchoolsTeachersOrStudents() {
        Institution inst = new Institution();
        inst.setId(5L);

        when(institutionRepository.findById(5L)).thenReturn(Optional.of(inst));

        // When schools exist
        when(schoolRepository.findByInstitutionId(5L)).thenReturn(List.of(new School()));
        BusinessException exSchools = assertThrows(BusinessException.class, () -> institutionService.delete(5L));
        assertTrue(exSchools.getMessage().contains("possui escolas"));

        // When teachers exist
        when(schoolRepository.findByInstitutionId(5L)).thenReturn(List.of());
        when(teacherRepository.findByInstitutionId(5L)).thenReturn(List.of(new Teacher()));
        BusinessException exTeachers = assertThrows(BusinessException.class, () -> institutionService.delete(5L));
        assertTrue(exTeachers.getMessage().contains("possui professores"));

        // When students exist
        when(teacherRepository.findByInstitutionId(5L)).thenReturn(List.of());
        when(studentRepository.findByInstitutionId(5L)).thenReturn(List.of(new Student()));
        BusinessException exStudents = assertThrows(BusinessException.class, () -> institutionService.delete(5L));
        assertTrue(exStudents.getMessage().contains("possui estudantes cadastrados"));
    }
}
