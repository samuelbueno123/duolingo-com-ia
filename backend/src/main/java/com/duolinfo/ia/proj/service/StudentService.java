package com.duolinfo.ia.proj.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public StudentService(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // CRUD
    // ==========================================

    @Transactional
    public Student create(Student student) {
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            String email = student.getEmail().trim().toLowerCase();
            student.setEmail(email);

            // Bloqueia se já for um estudante cadastrado
            if (studentRepository.findByEmail(email).isPresent()) {
                throw new BusinessException("Já existe um estudante cadastrado com o e-mail: " + email);
            }

            Optional<User> baseUserOpt = userRepository.findByEmail(email);
            if (baseUserOpt.isPresent()) {
                User baseUser = baseUserOpt.get();

                // Bloqueia se já for um professor cadastrado
                if (teacherRepository.existsById(baseUser.getId()) || studentRepository.existsById(baseUser.getId())) {
                    throw new BusinessException("E-mail já cadastrado com outro perfil no sistema.");
                }

                // Proteção contra hijacking de conta com senha existente
                if (baseUser.getPasswordHash() != null && !baseUser.getPasswordHash().isBlank()) {
                    if (student.getPasswordHash() == null ||
                        student.getPasswordHash().isBlank() ||
                        !passwordEncoder.matches(student.getPasswordHash(), baseUser.getPasswordHash())) {
                        throw new BusinessException("Este e-mail já possui uma conta cadastrada. Faça login para continuar.");
                    }
                } else if (student.getPasswordHash() != null && !student.getPasswordHash().isBlank()) {
                    baseUser.setPasswordHash(encodePasswordIfNeeded(student.getPasswordHash()));
                }

                // Preserva o nome original se já existir, atualizando apenas se estiver em branco
                if ((baseUser.getName() == null || baseUser.getName().isBlank()) &&
                    student.getName() != null && !student.getName().isBlank()) {
                    baseUser.setName(student.getName().trim());
                }

                Long userId = baseUser.getId();
                userRepository.save(baseUser);

                entityManager.createNativeQuery("INSERT INTO students (id) VALUES (:id)")
                    .setParameter("id", userId)
                    .executeUpdate();

                if (student.getLanguages() != null && !student.getLanguages().isEmpty()) {
                    for (StudentLanguage lang : student.getLanguages()) {
                        if (lang.getLanguageName() != null && !lang.getLanguageName().isBlank()) {
                            String level = (lang.getLevel() != null && !lang.getLevel().isBlank())
                                ? lang.getLevel()
                                : "1";
                            int score = (lang.getScore() != null) ? lang.getScore() : 0;

                            entityManager.createNativeQuery(
                                "INSERT INTO student_languages (student_id, language_name, level, score) VALUES (:studentId, :langName, :level, :score)"
                            )
                            .setParameter("studentId", userId)
                            .setParameter("langName", lang.getLanguageName().trim())
                            .setParameter("level", level)
                            .setParameter("score", score)
                            .executeUpdate();
                        }
                    }
                }

                entityManager.flush();
                entityManager.clear();

                return studentRepository.findById(userId).orElseThrow();
            }

            // Se for uma conta nova e com senha pura informada, criptografa a senha antes de salvar
            if (student.getPasswordHash() != null && !student.getPasswordHash().isBlank()) {
                student.setPasswordHash(encodePasswordIfNeeded(student.getPasswordHash()));
            }
        }

        return studentRepository.save(student);
    }

    private String encodePasswordIfNeeded(String password) {
        if (password == null || password.isBlank()) {
            return null;
        }
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }

    @Transactional
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

    @Transactional
    public void delete(Student student) {
        studentRepository.delete(student);
    }

    @Transactional
    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }

    @Transactional
    public void deleteByGoogleId(String googleId) {
        studentRepository.findByGoogleId(googleId)
                .ifPresent(studentRepository::delete);
    }

    @Transactional
    public void deleteByEmail(String email) {
        studentRepository.findByEmail(email)
                .ifPresent(studentRepository::delete);
    }

    // ==========================================
    // Busca
    // ==========================================

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Student> findAllById(Iterable<Long> ids) {
        return studentRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Student findByGoogleId(String googleId) {
        return studentRepository.findByGoogleId(googleId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Student findByEmail(String email) {
        return studentRepository.findByEmail(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Student> findByName(String name) {
        return studentRepository.findAll().stream()
                .filter(student ->
                        student.getName() != null
                                && student.getName()
                                        .toLowerCase()
                                        .contains(name.toLowerCase()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Student> findByPreferredLanguage(String preferredLanguage) {
        return studentRepository.findByPreferredLanguage(preferredLanguage);
    }

    // ==========================================
    // Matrículas
    // ==========================================

    @Transactional(readOnly = true)
    public List<Enrollment> findEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findActiveEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                EnrollmentStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public List<Student> findByClass(Long schoolClassId) {
        return studentRepository.findByEnrollments_SchoolClass_Id(
                schoolClassId
        );
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return studentRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByGoogleId(String googleId) {
        return studentRepository.findByGoogleId(googleId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return studentRepository.findByEmail(email).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return studentRepository.findAll().stream()
                .anyMatch(student ->
                        student.getName() != null
                                && student.getName().equalsIgnoreCase(name));
    }
}
