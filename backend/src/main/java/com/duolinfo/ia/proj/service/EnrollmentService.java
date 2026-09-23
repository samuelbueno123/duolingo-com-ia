package com.duolinfo.ia.proj.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public Enrollment enrollStudent(
        Long studentId,
        Long schoolClassId,
        LocalDate enrollmentDate,
        String notes
    ) {

        Student student = findStudent(studentId);

        SchoolClass schoolClass = findSchoolClass(schoolClassId);

        if (!Boolean.TRUE.equals(schoolClass.getActive())) {
            throw new BusinessException(
                "Não é possível matricular o aluno em uma turma inativa."
            );
        }

        /*
         * O aluno não pode possuir duas matrículas ativas.
         */
        List<Enrollment> activeEnrollments =
            enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
            );

        if (!activeEnrollments.isEmpty()) {
            Enrollment active = activeEnrollments.get(0);

            if (active.getSchoolClass().getId().equals(schoolClassId)) {
                throw new BusinessException(
                    "O aluno já está matriculado nesta turma."
                );
            }

            throw new BusinessException(
                "O aluno já possui uma matrícula ativa em outra turma."
            );
        }

        Enrollment enrollment = Enrollment.builder()
            .student(student)
            .schoolClass(schoolClass)
            .enrollmentDate(
                enrollmentDate != null
                    ? enrollmentDate
                    : LocalDate.now()
            )
            .status(EnrollmentStatus.ACTIVE)
            .notes(notes)
            .build();

        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public Enrollment findById(Long id) {

        return enrollmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Matrícula não encontrada: " + id
            ));
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByStudent(Long studentId) {

        findStudent(studentId);

        return enrollmentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByClass(Long schoolClassId) {

        findSchoolClass(schoolClassId);

        return enrollmentRepository.findBySchoolClassId(schoolClassId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findActiveByClass(Long schoolClassId) {

        findSchoolClass(schoolClassId);

        return enrollmentRepository
            .findBySchoolClassIdAndStatusOrderByEnrollmentDateAsc(
                schoolClassId,
                EnrollmentStatus.ACTIVE
            );
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findActiveByStudent(Long studentId) {

        findStudent(studentId);

        return enrollmentRepository
            .findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
            );
    }

    @Transactional
    public Enrollment cancel(
        Long enrollmentId,
        LocalDate cancellationDate,
        String notes
    ) {

        Enrollment enrollment = findById(enrollmentId);

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessException(
                "A matrícula não está ativa."
            );
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);

        enrollment.setCancellationDate(
            cancellationDate != null
                ? cancellationDate
                : LocalDate.now()
        );

        if (notes != null) {
            enrollment.setNotes(notes);
        }

        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public Enrollment complete(
        Long enrollmentId,
        LocalDate completionDate,
        String notes
    ) {

        Enrollment enrollment = findById(enrollmentId);

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessException(
                "A matrícula não está ativa."
            );
        }

        enrollment.setStatus(EnrollmentStatus.COMPLETED);

        enrollment.setCancellationDate(
            completionDate != null
                ? completionDate
                : LocalDate.now()
        );

        if (notes != null) {
            enrollment.setNotes(notes);
        }

        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public Enrollment transfer(
        Long studentId,
        Long newSchoolClassId,
        LocalDate transferDate,
        String notes
    ) {

        SchoolClass newClass = findSchoolClass(newSchoolClassId);

        if (!Boolean.TRUE.equals(newClass.getActive())) {
            throw new BusinessException(
                "A nova turma está inativa."
            );
        }

        List<Enrollment> activeEnrollments =
            enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
            );

        Enrollment currentEnrollment = null;

        if (!activeEnrollments.isEmpty()) {
            currentEnrollment = activeEnrollments.get(0);
        }

        LocalDate date =
            transferDate != null
                ? transferDate
                : LocalDate.now();

        /*
         * Encerra a matrícula atual.
         */
        if (currentEnrollment != null) {

            if (currentEnrollment.getSchoolClass()
                    .getId()
                    .equals(newSchoolClassId)) {

                throw new BusinessException(
                    "O aluno já está matriculado nesta turma."
                );
            }

            currentEnrollment.setStatus(
                EnrollmentStatus.TRANSFERRED
            );

            currentEnrollment.setCancellationDate(date);

            if (notes != null) {
                currentEnrollment.setNotes(notes);
            }

            enrollmentRepository.save(currentEnrollment);
        }

        /*
         * Cria nova matrícula.
         */
        Student student = findStudent(studentId);

        Enrollment newEnrollment = Enrollment.builder()
            .student(student)
            .schoolClass(newClass)
            .enrollmentDate(date)
            .status(EnrollmentStatus.ACTIVE)
            .notes(notes)
            .build();

        return enrollmentRepository.save(newEnrollment);
    }

    @Transactional
    public void delete(Long enrollmentId) {

        Enrollment enrollment = findById(enrollmentId);

        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
            throw new BusinessException(
                "Não é possível excluir uma matrícula ativa. "
                + "Cancele ou transfira a matrícula primeiro."
            );
        }

        enrollmentRepository.delete(enrollment);
    }

    private Student findStudent(Long id) {

        return studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Aluno não encontrado: " + id
            ));
    }

    private SchoolClass findSchoolClass(Long id) {

        return schoolClassRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Turma não encontrada: " + id
            ));
    }
}