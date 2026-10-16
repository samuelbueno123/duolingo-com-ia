package com.duolinfo.ia.proj.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final ClassTeacherAssignmentRepository assignmentRepository;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public TeacherService(
            TeacherRepository teacherRepository,
            ClassTeacherAssignmentRepository assignmentRepository,
            InstitutionRepository institutionRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {

        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // CRUD
    // ==========================================

    @Transactional
    public Teacher create(
            Long institutionId,
            Teacher teacher) {

        teacher.setApproved(false);

        Institution institution =
            institutionRepository.findById(institutionId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Instituição não encontrada: " + institutionId
                    )
                );

        if (teacher.getEmail() != null && !teacher.getEmail().isBlank()) {
            String email = teacher.getEmail().trim().toLowerCase();
            teacher.setEmail(email);

            if (teacherRepository.findByEmailIgnoreCase(email).isPresent()) {
                throw new BusinessException("Já existe um professor cadastrado com o e-mail: " + email);
            }

            Optional<User> baseUserOpt = userRepository.findByEmailIgnoreCase(email);
            if (baseUserOpt.isPresent()) {
                User baseUser = baseUserOpt.get();

                if (studentRepository.existsById(baseUser.getId()) || teacherRepository.existsById(baseUser.getId())) {
                    throw new BusinessException("E-mail já cadastrado com outro perfil no sistema.");
                }

                // Proteção contra hijacking de conta com senha existente
                if (baseUser.getPasswordHash() != null && !baseUser.getPasswordHash().isBlank()) {
                    if (teacher.getPasswordHash() == null ||
                        teacher.getPasswordHash().isBlank() ||
                        !passwordEncoder.matches(teacher.getPasswordHash(), baseUser.getPasswordHash())) {
                        throw new BusinessException("Este e-mail já possui uma conta cadastrada. Faça login para continuar.");
                    }
                } else if (teacher.getPasswordHash() != null && !teacher.getPasswordHash().isBlank()) {
                    baseUser.setPasswordHash(encodePasswordIfNeeded(teacher.getPasswordHash()));
                }

                if ((baseUser.getName() == null || baseUser.getName().isBlank()) &&
                    teacher.getName() != null && !teacher.getName().isBlank()) {
                    baseUser.setName(teacher.getName().trim());
                }

                Long userId = baseUser.getId();
                userRepository.save(baseUser);

                entityManager.createNativeQuery(
                    "INSERT INTO teachers (id, institution_id, bibliography) VALUES (:id, :institutionId, :bibliography)"
                )
                .setParameter("id", userId)
                .setParameter("institutionId", institutionId)
                .setParameter("bibliography", teacher.getBibliography())
                .executeUpdate();

                if (teacher.getTaughtLanguages() != null && !teacher.getTaughtLanguages().isEmpty()) {
                    for (String lang : teacher.getTaughtLanguages()) {
                        if (lang != null && !lang.isBlank()) {
                            entityManager.createNativeQuery(
                                "INSERT INTO teacher_taught_languages (teacher_id, language_name) VALUES (:teacherId, :lang)"
                            )
                            .setParameter("teacherId", userId)
                            .setParameter("lang", lang.trim())
                            .executeUpdate();
                        }
                    }
                }

                if (teacher.getSpecializationAreas() != null && !teacher.getSpecializationAreas().isEmpty()) {
                    for (String area : teacher.getSpecializationAreas()) {
                        if (area != null && !area.isBlank()) {
                            entityManager.createNativeQuery(
                                "INSERT INTO teacher_specialization_areas (teacher_id, specialization_area) VALUES (:teacherId, :area)"
                            )
                            .setParameter("teacherId", userId)
                            .setParameter("area", area.trim())
                            .executeUpdate();
                        }
                    }
                }

                if (teacher.getDocuments() != null && !teacher.getDocuments().isEmpty()) {
                    for (com.duolinfo.ia.proj.entity.TeacherDocument doc : teacher.getDocuments()) {
                        if (doc.getFileName() != null && doc.getFileData() != null) {
                            entityManager.createNativeQuery(
                                "INSERT INTO teacher_documents (teacher_id, file_name, content_type, file_size, file_data) VALUES (:teacherId, :fileName, :contentType, :fileSize, :fileData)"
                            )
                            .setParameter("teacherId", userId)
                            .setParameter("fileName", doc.getFileName())
                            .setParameter("contentType", doc.getContentType())
                            .setParameter("fileSize", doc.getFileSize())
                            .setParameter("fileData", doc.getFileData())
                            .executeUpdate();
                        }
                    }
                }

                entityManager.flush();
                entityManager.clear();

                return teacherRepository.findById(userId).orElseThrow();
            }

            if (teacher.getPasswordHash() != null && !teacher.getPasswordHash().isBlank()) {
                teacher.setPasswordHash(encodePasswordIfNeeded(teacher.getPasswordHash()));
            }
        }

        teacher.setInstitution(institution);

        return teacherRepository.save(teacher);
    }

    @Transactional
    public void ensureAdminTeacherProfile(Long userId) {
        if (userId == null) {
            return;
        }

        Teacher teacher = teacherRepository.findById(userId).orElse(null);
        if (teacher != null) {
            if (!Boolean.TRUE.equals(teacher.getApproved())) {
                teacher.setApproved(true);
                teacherRepository.save(teacher);
            }
            return;
        }

        Institution institution = institutionRepository
            .findByCode(InstitutionService.DEFAULT_INSTITUTION_CODE)
            .orElseThrow(() -> new IllegalStateException(
                "A instituição padrão do CEFET não foi inicializada."));

        entityManager.flush();
        entityManager.createNativeQuery(
            "INSERT INTO teachers (id, institution_id, bibliography, approved) "
                + "VALUES (:id, :institutionId, '', true) ON CONFLICT (id) DO NOTHING")
            .setParameter("id", userId)
            .setParameter("institutionId", institution.getId())
            .executeUpdate();
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
    public Teacher update(
            Long id,
            Long institutionId,
            Teacher data) {

        Teacher current = findById(id);

        if (institutionId != null &&
                (current.getInstitution() == null ||
                !institutionId.equals(
                    current.getInstitution().getId()))) {

            Institution institution =
                institutionRepository.findById(institutionId)
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Instituição não encontrada: "
                                + institutionId
                        )
                    );

            current.setInstitution(institution);
        }

        current.setName(data.getName());
        if (data.getEmail() != null && !data.getEmail().isBlank()) {
            String email = data.getEmail().trim().toLowerCase(Locale.ROOT);
            boolean usedByAnotherAccount = userRepository.findByEmailIgnoreCase(email)
                .filter(account -> !account.getId().equals(current.getId()))
                .isPresent();
            if (usedByAnotherAccount) {
                throw new BusinessException("E-mail já cadastrado no sistema.");
            }
            current.setEmail(email);
        }
        current.setTaughtLanguages(
            data.getTaughtLanguages()
        );
        current.setSpecializationAreas(
            data.getSpecializationAreas()
        );
        current.setBibliography(
            data.getBibliography()
        );

        return teacherRepository.save(current);
    }

    @Transactional
    public Teacher updateApproval(Long id, boolean approved) {
        Teacher teacher = findById(id);
        teacher.setApproved(approved);
        return teacherRepository.save(teacher);
    }

    @Transactional
    public void delete(Teacher teacher) {
        teacherRepository.delete(teacher);
    }

    @Transactional
    public void deleteById(Long id) {
        teacherRepository.delete(findById(id));
    }

    @Transactional
    public void deleteByGoogleId(String googleId) {
        teacherRepository.findByGoogleId(googleId)
                .ifPresent(teacherRepository::delete);
    }

    @Transactional
    public void deleteByEmail(String email) {
        teacherRepository.findByEmail(email)
                .ifPresent(teacherRepository::delete);
    }

    // ==========================================
    // Busca
    // ==========================================

    @Transactional(readOnly = true)
    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Teacher> findAllById(Iterable<Long> ids) {
        return teacherRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public Teacher findById(Long id) {
        return teacherRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Professor não encontrado: " + id
            ));
    }

    @Transactional(readOnly = true)
    public Teacher findByGoogleId(String googleId) {
        return teacherRepository.findByGoogleId(googleId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Teacher findByEmail(String email) {
        return teacherRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Teacher> findByName(String name) {
        return teacherRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Teacher> findByInstitution(String institution) {
        return teacherRepository
                .findByInstitution_NameContainingIgnoreCase(institution);
    }

    @Transactional(readOnly = true)
    public List<Teacher> findByInstitutionId(Long institutionId) {
        return teacherRepository.findByInstitutionId(institutionId);
    }

    @Transactional(readOnly = true)
    public List<Teacher> findByTaughtLanguage(
            Long institutionId,
            String language) {

        return teacherRepository
                .findByInstitutionIdAndTaughtLanguagesContaining(
                        institutionId,
                        language
                );
    }

    @Transactional(readOnly = true)
    public List<Teacher> findBySpecializationArea(
            Long institutionId,
            String specializationArea) {

        return teacherRepository
                .findByInstitutionIdAndSpecializationAreasContaining(
                        institutionId,
                        specializationArea
                );
    }

    // ==========================================
    // Turmas / Histórico
    // ==========================================

    @Transactional(readOnly = true)
    public List<ClassTeacherAssignment> findClassAssignments(
            Long teacherId) {

        return assignmentRepository
                .findByTeacherIdOrderByStartDateDesc(teacherId);
    }

    @Transactional(readOnly = true)
    public boolean hasClassAssignments(Long teacherId) {
        return !assignmentRepository
                .findByTeacherId(teacherId)
                .isEmpty();
    }

    // ==========================================
    // Verificações de existência
    // ==========================================

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return teacherRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean isApproved(Long id) {
        return teacherRepository.findById(id)
            .map(teacher -> Boolean.TRUE.equals(teacher.getApproved()))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean existsByGoogleId(String googleId) {
        return teacherRepository.findByGoogleId(googleId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return teacherRepository.findByEmail(email).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return teacherRepository.existsByNameIgnoreCase(name);
    }
}
