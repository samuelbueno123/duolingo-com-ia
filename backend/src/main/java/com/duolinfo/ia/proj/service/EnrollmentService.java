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

public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SseEmitterService sseEmitterService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository) {
        this(enrollmentRepository, studentRepository, schoolClassRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SseEmitterService sseEmitterService) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.sseEmitterService = sseEmitterService;
    }

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

        List<Enrollment> activeEnrollments =
            enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
            );

        boolean alreadyInSameClass = activeEnrollments.stream()
            .anyMatch(active -> active.getSchoolClass() != null
                && active.getSchoolClass().getId().equals(schoolClassId));
        if (alreadyInSameClass) {
            throw new BusinessException(
                "O aluno já está matriculado ativamente nesta turma."
            );
        }

        if (schoolClass.getSchool() != null && schoolClass.getSchool().getInstitution() != null) {
            student.setInstitution(schoolClass.getSchool().getInstitution());
            studentRepository.save(student);
        } else if (schoolClass.getInstitution() != null) {
            student.setInstitution(schoolClass.getInstitution());
            studentRepository.save(student);
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

        Enrollment saved = enrollmentRepository.save(enrollment);
        notifyEnrollmentChanged(saved);
        return saved;
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

        Enrollment saved = enrollmentRepository.save(enrollment);
        notifyEnrollmentChanged(saved);
        return saved;
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

        Enrollment saved = enrollmentRepository.save(enrollment);
        notifyEnrollmentChanged(saved);
        return saved;
    }

    @Transactional
    public Enrollment updateStatus(Long enrollmentId, EnrollmentStatus status, String notes) {
        Enrollment enrollment = findById(enrollmentId);
        if (status == EnrollmentStatus.ACTIVE) {
            List<Enrollment> active = enrollmentRepository.findByStudentIdAndStatus(
                enrollment.getStudent().getId(), EnrollmentStatus.ACTIVE);
            if (active.stream().anyMatch(item -> !item.getId().equals(enrollmentId))) {
                throw new BusinessException("O aluno jÃ¡ possui outra matrÃ­cula ativa.");
            }
            enrollment.setCancellationDate(null);
        } else if (status == EnrollmentStatus.CANCELLED || status == EnrollmentStatus.COMPLETED || status == EnrollmentStatus.TRANSFERRED) {
            enrollment.setCancellationDate(LocalDate.now());
        } else if (status == EnrollmentStatus.SUSPENDED) {
            enrollment.setCancellationDate(null);
        }
        enrollment.setStatus(status);
        if (notes != null) enrollment.setNotes(notes);
        Enrollment saved = enrollmentRepository.save(enrollment);
        notifyEnrollmentChanged(saved);
        return saved;
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

    private void notifyEnrollmentChanged(Enrollment enrollment) {
        if (enrollment == null || sseEmitterService == null) return;
        try {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("enrollmentId", enrollment.getId());
            if (enrollment.getStudent() != null) {
                data.put("studentId", enrollment.getStudent().getId());
                data.put("studentName", enrollment.getStudent().getName());
            }
            if (enrollment.getSchoolClass() != null) {
                data.put("classId", enrollment.getSchoolClass().getId());
                data.put("className", enrollment.getSchoolClass().getName());
            }
            if (enrollment.getStatus() != null) {
                data.put("status", enrollment.getStatus().name());
            }

            if (enrollment.getStudent() != null) {
                sseEmitterService.sendToUser(enrollment.getStudent().getId(), "ENROLLMENT_UPDATED", data);
            }
            sseEmitterService.sendToRole("TEACHER", "ENROLLMENT_UPDATED", data);
            sseEmitterService.sendToRole("ADMIN", "ENROLLMENT_UPDATED", data);
        } catch (Exception ignored) {
        }
    }

    private SchoolClass findSchoolClass(Long id) {

        return schoolClassRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Turma não encontrada: " + id
            ));
    }
}
