package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;

import org.springframework.http.HttpStatus;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final ClassTeacherAssignmentRepository assignmentRepository;
    private final InstitutionRepository institutionRepository;

    public TeacherService(
            TeacherRepository teacherRepository,
            ClassTeacherAssignmentRepository assignmentRepository,
            InstitutionRepository institutionRepository) {

        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.institutionRepository = institutionRepository;
    }

    // ==========================================
    // CRUD
    // ==========================================

    public Teacher create(
            Long institutionId,
            Teacher teacher) {

        Institution institution =
            institutionRepository.findById(institutionId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Instituição não encontrada: " + institutionId
                    )
                );

        teacher.setInstitution(institution);

        return teacherRepository.save(teacher);
    }

    public Teacher update(
            Long id,
            Long institutionId,
            Teacher data) {

        Teacher current = findById(id);

        if (current == null) {
            throw new ResourceNotFoundException(
                "Professor não encontrado: " + id
            );
        }

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
        current.setEmail(data.getEmail());
        current.setGoogleId(data.getGoogleId());
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

    public void delete(Teacher teacher) {
        teacherRepository.delete(teacher);
    }

    public void deleteById(Long id) {
        teacherRepository.deleteById(id);
    }

    public void deleteByGoogleId(String googleId) {
        teacherRepository.findByGoogleId(googleId)
                .ifPresent(teacherRepository::delete);
    }

    public void deleteByEmail(String email) {
        teacherRepository.findByEmail(email)
                .ifPresent(teacherRepository::delete);
    }

    // ==========================================
    // Busca
    // ==========================================

    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    public List<Teacher> findAllById(Iterable<Long> ids) {
        return teacherRepository.findAllById(ids);
    }

    public Teacher findById(Long id) {
        return teacherRepository.findById(id).orElse(null);
    }

    public Teacher findByGoogleId(String googleId) {
        return teacherRepository.findByGoogleId(googleId).orElse(null);
    }

    public Teacher findByEmail(String email) {
        return teacherRepository.findByEmail(email).orElse(null);
    }

    public List<Teacher> findByName(String name) {
        // O filtro é feito direto no banco de dados (muito mais rápido)
        return teacherRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Busca professores pelo nome da instituição.
     *
     * Antes:
     * findByInstitutionContainingIgnoreCase(...)
     *
     * Agora:
     * institution é uma entidade relacionada.
     */
    public List<Teacher> findByInstitution(String institution) {
        return teacherRepository
                .findByInstitution_NameContainingIgnoreCase(institution);
    }

    public List<Teacher> findByInstitutionId(Long institutionId) {
        return teacherRepository.findByInstitutionId(institutionId);
    }

    public List<Teacher> findByTaughtLanguage(
            Long institutionId,
            String language) {

        return teacherRepository
                .findByInstitutionIdAndTaughtLanguagesContaining(
                        institutionId,
                        language
                );
    }

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

    /**
     * Todas as atribuições de turma do professor.
     */
    public List<ClassTeacherAssignment> findClassAssignments(
            Long teacherId) {

        return assignmentRepository
                .findByTeacherIdOrderByStartDateDesc(teacherId);
    }

    /**
     * Verifica se o professor possui histórico de turmas.
     */
    public boolean hasClassAssignments(Long teacherId) {
        return !assignmentRepository
                .findByTeacherId(teacherId)
                .isEmpty();
    }

    // ==========================================
    // Verificações de existência
    // ==========================================

    public boolean existsById(Long id) {
        return teacherRepository.existsById(id);
    }

    public boolean existsByGoogleId(String googleId) {
        return teacherRepository.findByGoogleId(googleId).isPresent();
    }

    public boolean existsByEmail(String email) {
        return teacherRepository.findByEmail(email).isPresent();
    }

    public boolean existsByName(String name) {
        // O banco de dados retorna apenas um true/false de forma super otimizada
        return teacherRepository.existsByNameIgnoreCase(name);
    }
}