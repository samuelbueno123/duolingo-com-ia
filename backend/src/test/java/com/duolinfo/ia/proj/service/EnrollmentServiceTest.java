package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

class EnrollmentServiceTest {

    private EnrollmentRepository enrollmentRepository;
    private StudentRepository studentRepository;
    private SchoolClassRepository schoolClassRepository;
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentRepository = mock(EnrollmentRepository.class);
        studentRepository = mock(StudentRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);

        enrollmentService = new EnrollmentService(
            enrollmentRepository,
            studentRepository,
            schoolClassRepository,
            null
        );
    }

    @Test
    void enrollStudent_successfulWhenNoActiveInSameClass() {
        Student student = new Student();
        student.setId(1L);

        Institution inst = new Institution();
        inst.setId(10L);

        School school = new School();
        school.setInstitution(inst);

        SchoolClass sc = new SchoolClass();
        sc.setId(100L);
        sc.setSchool(school);
        sc.setActive(true);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById(100L)).thenReturn(Optional.of(sc));
        when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE)).thenReturn(List.of());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment enrollment = enrollmentService.enrollStudent(1L, 100L, LocalDate.now(), "Matrícula nova");

        assertNotNull(enrollment);
        assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());
        assertEquals(inst, student.getInstitution());
    }

    @Test
    void enrollStudent_throwsWhenAlreadyActiveInSameClass() {
        Student student = new Student();
        student.setId(1L);

        SchoolClass sc = new SchoolClass();
        sc.setId(100L);
        sc.setActive(true);

        Enrollment existing = Enrollment.builder()
            .id(50L)
            .student(student)
            .schoolClass(sc)
            .status(EnrollmentStatus.ACTIVE)
            .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById(100L)).thenReturn(Optional.of(sc));
        when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE))
            .thenReturn(List.of(existing));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> enrollmentService.enrollStudent(1L, 100L, LocalDate.now(), null)
        );
        assertTrue(ex.getMessage().contains("já está matriculado ativamente nesta turma"));
    }

    @Test
    void enrollStudent_allowsActiveEnrollmentInDifferentClassOfSameInstitution() {
        Institution inst = new Institution();
        inst.setId(10L);

        School school = new School();
        school.setInstitution(inst);

        Student student = new Student();
        student.setId(1L);
        student.setInstitution(inst);

        SchoolClass classA = new SchoolClass();
        classA.setId(100L);
        classA.setSchool(school);
        classA.setActive(true);

        SchoolClass classB = new SchoolClass();
        classB.setId(200L);
        classB.setSchool(school);
        classB.setActive(true);

        Enrollment existingInClassA = Enrollment.builder()
            .id(50L)
            .student(student)
            .schoolClass(classA)
            .status(EnrollmentStatus.ACTIVE)
            .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById(200L)).thenReturn(Optional.of(classB));
        when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE))
            .thenReturn(List.of(existingInClassA));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment enrollmentB = enrollmentService.enrollStudent(1L, 200L, LocalDate.now(), "Segunda turma");
        assertNotNull(enrollmentB);
        assertEquals(EnrollmentStatus.ACTIVE, enrollmentB.getStatus());
    }

    @Test
    void enrollStudent_rejectsCrossInstitutionEnrollmentWithoutTransfer() {
        Institution instA = new Institution();
        instA.setId(10L);
        instA.setName("Instituição Alfa");

        Institution instB = new Institution();
        instB.setId(20L);
        instB.setName("Instituição Beta");

        School schoolA = new School();
        schoolA.setInstitution(instA);

        School schoolB = new School();
        schoolB.setInstitution(instB);

        Student student = new Student();
        student.setId(1L);
        student.setInstitution(instA);

        SchoolClass classA = new SchoolClass();
        classA.setId(100L);
        classA.setSchool(schoolA);
        classA.setActive(true);

        SchoolClass classB = new SchoolClass();
        classB.setId(200L);
        classB.setSchool(schoolB);
        classB.setActive(true);

        Enrollment activeInA = Enrollment.builder()
            .id(1L)
            .student(student)
            .schoolClass(classA)
            .status(EnrollmentStatus.ACTIVE)
            .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById(200L)).thenReturn(Optional.of(classB));
        when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE))
            .thenReturn(List.of(activeInA));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> enrollmentService.enrollStudent(1L, 200L, LocalDate.now(), null)
        );
        assertTrue(ex.getMessage().contains("já possui matrículas ativas em outra instituição"));
    }

    @Test
    void updateStatus_allowsActivatingClassWhenEnrolledInAnotherClass() {
        Student student = new Student();
        student.setId(1L);

        SchoolClass classA = new SchoolClass();
        classA.setId(100L);

        SchoolClass classB = new SchoolClass();
        classB.setId(200L);

        Enrollment activeInA = Enrollment.builder()
            .id(1L)
            .student(student)
            .schoolClass(classA)
            .status(EnrollmentStatus.ACTIVE)
            .build();

        Enrollment suspendedInB = Enrollment.builder()
            .id(2L)
            .student(student)
            .schoolClass(classB)
            .status(EnrollmentStatus.SUSPENDED)
            .build();

        when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(suspendedInB));
        when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE))
            .thenReturn(List.of(activeInA));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment reactivated = enrollmentService.updateStatus(2L, EnrollmentStatus.ACTIVE, "Reativando");
        assertEquals(EnrollmentStatus.ACTIVE, reactivated.getStatus());
        assertNull(reactivated.getCancellationDate());
    }

    @Test
    void updateStatus_rejectsActivatingWhenDuplicateActiveInSameClass() {
        Student student = new Student();
        student.setId(1L);

        SchoolClass classA = new SchoolClass();
        classA.setId(100L);

        Enrollment activeInA = Enrollment.builder()
            .id(1L)
            .student(student)
            .schoolClass(classA)
            .status(EnrollmentStatus.ACTIVE)
            .build();

        Enrollment cancelledInA = Enrollment.builder()
            .id(2L)
            .student(student)
            .schoolClass(classA)
            .status(EnrollmentStatus.CANCELLED)
            .build();

        when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(cancelledInA));
        when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE))
            .thenReturn(List.of(activeInA));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> enrollmentService.updateStatus(2L, EnrollmentStatus.ACTIVE, null)
        );
        assertTrue(ex.getMessage().contains("já possui outra matrícula ativa nesta turma"));
    }

    @Test
    void cancelAndComplete_updatesStatusAndDates() {
        Enrollment enrollment = Enrollment.builder()
            .id(10L)
            .status(EnrollmentStatus.ACTIVE)
            .build();

        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment cancelled = enrollmentService.cancel(10L, LocalDate.of(2026, 5, 1), "Cancelado pelo aluno");
        assertEquals(EnrollmentStatus.CANCELLED, cancelled.getStatus());
        assertEquals(LocalDate.of(2026, 5, 1), cancelled.getCancellationDate());
        assertEquals("Cancelado pelo aluno", cancelled.getNotes());

        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        Enrollment completed = enrollmentService.complete(10L, LocalDate.of(2026, 12, 1), "Concluído com sucesso");
        assertEquals(EnrollmentStatus.COMPLETED, completed.getStatus());
        assertEquals(LocalDate.of(2026, 12, 1), completed.getCancellationDate());
    }
}
