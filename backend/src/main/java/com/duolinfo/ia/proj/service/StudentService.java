package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudentService(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository) {

        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // ==========================================
    // CRUD
    // ==========================================

    public Student create(Student student) {
        return studentRepository.save(student);
    }

    public Student update(Student student) {

        Student current =
            studentRepository.findById(student.getId())
                .orElse(null);

        if (current == null) {
            return null;
        }

        current.setName(student.getName());
        current.setEmail(student.getEmail());
        current.setGoogleId(student.getGoogleId());

        return studentRepository.save(current);
    }

    public void delete(Student student) {
        studentRepository.delete(student);
    }

    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }

    public void deleteByGoogleId(String googleId) {
        studentRepository.findByGoogleId(googleId)
                .ifPresent(studentRepository::delete);
    }

    public void deleteByEmail(String email) {
        studentRepository.findByEmail(email)
                .ifPresent(studentRepository::delete);
    }

    // ==========================================
    // Busca
    // ==========================================

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public List<Student> findAllById(Iterable<Long> ids) {
        return studentRepository.findAllById(ids);
    }

    public Student findById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }

    public Student findByGoogleId(String googleId) {
        return studentRepository.findByGoogleId(googleId).orElse(null);
    }

    public Student findByEmail(String email) {
        return studentRepository.findByEmail(email).orElse(null);
    }

    public List<Student> findByName(String name) {
        return studentRepository.findAll().stream()
                .filter(student ->
                        student.getName() != null
                                && student.getName()
                                        .toLowerCase()
                                        .contains(name.toLowerCase()))
                .toList();
    }

    public List<Student> findByPreferredLanguage(String preferredLanguage) {
        return studentRepository.findByPreferredLanguage(preferredLanguage);
    }

    // ==========================================
    // Matrículas
    // ==========================================

    /**
     * Todas as matrículas do aluno.
     */
    public List<Enrollment> findEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    /**
     * Matrículas ativas do aluno.
     */
    public List<Enrollment> findActiveEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
        );
    }

    /**
     * Alunos matriculados em uma determinada turma.
     */
    public List<Student> findByClass(Long schoolClassId) {
        return studentRepository.findByEnrollments_SchoolClass_Id(
                schoolClassId
        );
    }

    /**
     * Alunos com matrícula ativa em uma turma.
     */
    public List<Student> findActiveByClass(Long schoolClassId) {
        return studentRepository
                .findByEnrollments_SchoolClass_IdAndEnrollments_Status(
                        schoolClassId,
                        EnrollmentStatus.ACTIVE
                );
    }

    // ==========================================
    // Verificação
    // ==========================================

    public boolean existsById(Long id) {
        return studentRepository.existsById(id);
    }

    public boolean existsByGoogleId(String googleId) {
        return studentRepository.findByGoogleId(googleId).isPresent();
    }

    public boolean existsByEmail(String email) {
        return studentRepository.findByEmail(email).isPresent();
    }

    public boolean existsByName(String name) {
        return studentRepository.findAll().stream()
                .anyMatch(student ->
                        student.getName() != null
                                && student.getName().equalsIgnoreCase(name));
    }
}