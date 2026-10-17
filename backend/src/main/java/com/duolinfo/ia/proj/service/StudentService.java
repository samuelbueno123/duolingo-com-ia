package com.duolinfo.ia.proj.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.dto.response.InstitutionMembersResponseDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionMembersResponseDTO.MemberStudentDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionMembersResponseDTO.MemberTeacherDTO;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
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
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public StudentService(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            InstitutionRepository institutionRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // CRUD
    // ==========================================

    @Transactional
    public Student create(Student student) {
        return create(student, null);
    }

    @Transactional
    public Student create(Student student, Long institutionId) {
        student.setAdmin(false);
        student.setGender(User.normalizeGender(student.getGender()));
        Institution institution = resolveInstitution(institutionId);
        student.setInstitution(institution);

        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            String email = student.getEmail().trim().toLowerCase(Locale.ROOT);
            student.setEmail(email);

            // Bloqueia se já for um estudante cadastrado
            if (studentRepository.findByEmailIgnoreCase(email).isPresent()) {
                throw new BusinessException("Já existe um estudante cadastrado com o e-mail: " + email);
            }

            Optional<User> baseUserOpt = userRepository.findByEmailIgnoreCase(email);
            if (baseUserOpt.isPresent()) {
                User baseUser = baseUserOpt.get();

                if (baseUser.isSuperAdmin() || baseUser.isAdmin()) {
                    throw new BusinessException("Contas de administrador não podem ser convertidas em aluno.");
                }

                // Bloqueia se já for um professor cadastrado
                if (teacherRepository.existsById(baseUser.getId()) || studentRepository.existsById(baseUser.getId())) {
                    throw new BusinessException("E-mail já cadastrado com outro perfil no sistema.");
                }

                boolean isGoogleAccount = baseUser.getGoogleId() != null && !baseUser.getGoogleId().isBlank();
                // Proteção contra hijacking de conta com senha existente (exceto contas Google definindo senha)
                if (!isGoogleAccount && baseUser.getPasswordHash() != null && !baseUser.getPasswordHash().isBlank()) {
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
                baseUser.setGender(User.normalizeGender(student.getGender()));
                baseUser.setAdmin(false);

                Long userId = baseUser.getId();
                userRepository.save(baseUser);

                entityManager.createNativeQuery(
                    "INSERT INTO students (id, institution_id) VALUES (:id, :institutionId)")
                    .setParameter("id", userId)
                    .setParameter("institutionId", institution != null ? institution.getId() : null)
                    .executeUpdate();

                if (student.getLanguages() != null && !student.getLanguages().isEmpty()) {
                    for (StudentLanguage lang : student.getLanguages()) {
                        if (lang.getLanguageName() != null && !lang.getLanguageName().isBlank()) {
                            String level = (lang.getLevel() != null && !lang.getLevel().isBlank())
                                ? lang.getLevel()
                                : "Básico";
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

    private Institution resolveInstitution(Long institutionId) {
        if (institutionId != null) {
            return institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Instituição não encontrada: " + institutionId));
        }
        return institutionRepository.findByCode(InstitutionService.DEFAULT_INSTITUTION_CODE)
            .orElseGet(() -> institutionRepository.findAll().stream().findFirst().orElse(null));
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
        return update(student, null);
    }

    @Transactional
    public Student update(Student student, Long institutionId) {

        Student current =
            studentRepository.findById(student.getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Estudante não encontrado: " + student.getId()
            ));

        current.setAdmin(false);
        current.setName(student.getName());
        if (student.getGender() != null && !student.getGender().isBlank()) {
            current.setGender(User.normalizeGender(student.getGender()));
        }
        if (institutionId != null) {
            current.setInstitution(resolveInstitution(institutionId));
        } else if (current.getInstitution() == null) {
            current.setInstitution(resolveInstitution(null));
        }
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            String email = student.getEmail().trim().toLowerCase(Locale.ROOT);
            boolean usedByAnotherAccount = userRepository.findByEmailIgnoreCase(email)
                .filter(account -> !account.getId().equals(current.getId()))
                .isPresent();
            if (usedByAnotherAccount) {
                throw new BusinessException("E-mail já cadastrado no sistema.");
            }
            current.setEmail(email);
        }

        if (student.getLanguages() != null) {
            current.getLanguages().clear();
            for (StudentLanguage language : student.getLanguages()) {
                if (language.getLanguageName() == null || language.getLanguageName().isBlank()) continue;
                current.addLanguage(language);
            }
        }

        return studentRepository.save(current);
    }

    @Transactional
    public InstitutionMembersResponseDTO findInstitutionMembers(String studentEmail, String search) {
        Student student = findByEmail(studentEmail);
        if (student == null) {
            throw new ResourceNotFoundException("Estudante não encontrado.");
        }

        Institution institution = student.getInstitution();
        if (institution == null) {
            List<Enrollment> activeEnrollments = findActiveEnrollments(student.getId());
            if (!activeEnrollments.isEmpty()
                    && activeEnrollments.get(0).getSchoolClass() != null
                    && activeEnrollments.get(0).getSchoolClass().getSchool() != null) {
                institution = activeEnrollments.get(0).getSchoolClass().getSchool().getInstitution();
            }
            if (institution == null) {
                institution = resolveInstitution(null);
            }
            if (institution != null) {
                student.setInstitution(institution);
                studentRepository.save(student);
            }
        }

        if (institution == null) {
            return new InstitutionMembersResponseDTO(null, "Sem instituição", List.of(), List.of());
        }

        final Long instId = institution.getId();
        final String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<MemberTeacherDTO> teachers = teacherRepository.findByInstitutionId(instId).stream()
            .filter(t -> !t.isSuperAdmin())
            .filter(t -> Boolean.TRUE.equals(t.getApproved()))
            .filter(t -> query.isEmpty()
                || (t.getName() != null && t.getName().toLowerCase(Locale.ROOT).contains(query))
                || (t.getEmail() != null && t.getEmail().toLowerCase(Locale.ROOT).contains(query))
                || (t.getTaughtLanguages() != null && t.getTaughtLanguages().stream()
                    .anyMatch(l -> l.toLowerCase(Locale.ROOT).contains(query)))
                || (t.getSpecializationAreas() != null && t.getSpecializationAreas().stream()
                    .anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(query))))
            .map(t -> new MemberTeacherDTO(
                t.getId(),
                t.getName(),
                t.getEmail(),
                t.getProfilePicture(),
                t.getNormalizedGender(),
                t.getTaughtLanguages() == null ? List.of() : List.copyOf(t.getTaughtLanguages()),
                t.getSpecializationAreas() == null ? List.of() : List.copyOf(t.getSpecializationAreas())
            ))
            .toList();

        Map<Long, Student> studentsMap = new LinkedHashMap<>();
        for (Student s : studentRepository.findByInstitutionId(instId)) {
            studentsMap.put(s.getId(), s);
        }
        for (Student s : studentRepository.findAll()) {
            if (s.getInstitution() != null && instId.equals(s.getInstitution().getId())) {
                studentsMap.put(s.getId(), s);
            }
        }

        List<MemberStudentDTO> students = new ArrayList<>();
        for (Student s : studentsMap.values()) {
            List<String> langs = s.getLanguages() == null
                ? List.of()
                : s.getLanguages().stream().map(StudentLanguage::getLanguageName).toList();
            boolean matches = query.isEmpty()
                || (s.getName() != null && s.getName().toLowerCase(Locale.ROOT).contains(query))
                || (s.getEmail() != null && s.getEmail().toLowerCase(Locale.ROOT).contains(query))
                || langs.stream().anyMatch(l -> l.toLowerCase(Locale.ROOT).contains(query));
            if (matches) {
                students.add(new MemberStudentDTO(
                    s.getId(),
                    s.getName(),
                    s.getEmail(),
                    s.getProfilePicture(),
                    s.getNormalizedGender(),
                    langs
                ));
            }
        }

        return new InstitutionMembersResponseDTO(
            institution.getId(),
            institution.getName(),
            teachers,
            students
        );
    }

    @Transactional
    public void delete(Student student) {
        studentRepository.delete(student);
    }

    @Transactional
    public void deleteById(Long id) {
        studentRepository.delete(findById(id));
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
        return studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Estudante não encontrado: " + id
            ));
    }

    @Transactional(readOnly = true)
    public Student findByGoogleId(String googleId) {
        return studentRepository.findByGoogleId(googleId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Student findByEmail(String email) {
        return studentRepository.findByEmailIgnoreCase(email).orElse(null);
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
