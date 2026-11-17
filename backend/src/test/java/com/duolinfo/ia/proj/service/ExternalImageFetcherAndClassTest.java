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

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

class ExternalImageFetcherAndClassTest {

    @Test
    void externalImageFetcher_rejectsLocalhostAndPrivateAddresses() {
        ResponseStatusException localhostEx = assertThrows(
            ResponseStatusException.class,
            () -> ExternalImageFetcher.fetchExternalImage("http://localhost:8080/avatar.png")
        );
        assertEquals(400, localhostEx.getStatusCode().value());

        ResponseStatusException loopbackEx = assertThrows(
            ResponseStatusException.class,
            () -> ExternalImageFetcher.fetchExternalImage("http://127.0.0.1/secret.png")
        );
        assertEquals(400, loopbackEx.getStatusCode().value());

        ResponseStatusException nonHttpEx = assertThrows(
            ResponseStatusException.class,
            () -> ExternalImageFetcher.fetchExternalImage("ftp://example.com/photo.jpg")
        );
        assertEquals(400, nonHttpEx.getStatusCode().value());
    }

    @Test
    void schoolClassService_createForInstitutionAutoCreatesDefaultSchoolWhenMissing() {
        SchoolClassRepository classRepo = mock(SchoolClassRepository.class);
        SchoolRepository schoolRepo = mock(SchoolRepository.class);
        EnrollmentRepository enrollmentRepo = mock(EnrollmentRepository.class);
        TeacherRepository teacherRepo = mock(TeacherRepository.class);
        InstitutionRepository instRepo = mock(InstitutionRepository.class);
        ClassTeacherAssignmentRepository assignmentRepo = mock(ClassTeacherAssignmentRepository.class);
        LearningExerciseRepository exerciseRepo = mock(LearningExerciseRepository.class);
        ExerciseSubmissionRepository submissionRepo = mock(ExerciseSubmissionRepository.class);

        SchoolClassService service = new SchoolClassService(
            classRepo,
            schoolRepo,
            enrollmentRepo,
            teacherRepo,
            instRepo,
            assignmentRepo,
            exerciseRepo,
            submissionRepo
        );

        Institution inst = Institution.builder().id(2L).name("Instituto Teste").code("IT-2").build();
        School savedSchool = School.builder().id(99L).name("Instituto Teste").inepCode("INEP-2").institution(inst).build();

        when(instRepo.findById(2L)).thenReturn(Optional.of(inst));
        when(schoolRepo.findByInstitutionId(2L)).thenReturn(List.of());
        when(schoolRepo.save(any(School.class))).thenReturn(savedSchool);
        when(schoolRepo.findById(99L)).thenReturn(Optional.of(savedSchool));
        when(classRepo.findBySchoolId(99L)).thenReturn(List.of());
        when(classRepo.save(any(SchoolClass.class))).thenAnswer(inv -> {
            SchoolClass sc = inv.getArgument(0);
            sc.setId(55L);
            return sc;
        });

        SchoolClass input = SchoolClass.builder()
            .name("Inglês Intensivo A")
            .languageName("Inglês")
            .shift("MANHA")
            .build();

        SchoolClass created = service.createForInstitution(2L, input);

        assertNotNull(created);
        assertEquals(55L, created.getId());
        assertEquals(savedSchool, created.getSchool());
        assertEquals(inst, created.getInstitution());
        assertEquals("Inglês", created.getLanguageName());
        assertTrue(Boolean.TRUE.equals(created.getActive()));
    }

    @Test
    void assignTeacher_allowsSameDayReassignment() {
        ClassTeacherAssignmentRepository assignmentRepo = mock(ClassTeacherAssignmentRepository.class);
        SchoolClassRepository classRepo = mock(SchoolClassRepository.class);
        TeacherRepository teacherRepo = mock(TeacherRepository.class);

        ClassTeacherAssignmentService service = new ClassTeacherAssignmentService(
            assignmentRepo,
            classRepo,
            teacherRepo
        );

        Institution inst = Institution.builder().id(2L).name("Inst").code("INST").build();
        School school = School.builder().id(5L).name("School").institution(inst).build();
        SchoolClass sc = SchoolClass.builder().id(10L).name("Turma A").school(school).institution(inst).build();
        Teacher t1 = new Teacher();
        t1.setId(20L);
        t1.setName("Prof 1");
        t1.setInstitution(inst);
        Teacher t2 = new Teacher();
        t2.setId(30L);
        t2.setName("Prof 2");
        t2.setInstitution(inst);

        LocalDate today = LocalDate.now();
        ClassTeacherAssignment current = ClassTeacherAssignment.builder()
            .id(1L)
            .schoolClass(sc)
            .teacher(t1)
            .startDate(today)
            .build();

        when(classRepo.findById(10L)).thenReturn(Optional.of(sc));
        when(teacherRepo.findById(30L)).thenReturn(Optional.of(t2));
        when(assignmentRepo.findBySchoolClassIdAndEndDateIsNull(10L)).thenReturn(Optional.of(current));
        when(assignmentRepo.save(any(ClassTeacherAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassTeacherAssignment next = service.assignTeacher(10L, 30L, today, "Substituição no mesmo dia");

        assertNotNull(next);
        assertEquals(today, current.getEndDate());
        assertEquals(t2, next.getTeacher());
        assertEquals(today, next.getStartDate());
        verify(assignmentRepo).save(current);
    }
}
