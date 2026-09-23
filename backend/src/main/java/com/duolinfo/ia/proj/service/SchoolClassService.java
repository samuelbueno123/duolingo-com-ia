package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public SchoolClass create(
        Long schoolId,
        SchoolClass schoolClass
    ) {

        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Escola não encontrada: " + schoolId
            ));

        if (Boolean.FALSE.equals(schoolClass.getActive())) {
            // permitido criar inativa caso você queira usar
            // cadastro antes da ativação
        }

        validateUniqueClass(
            schoolId,
            schoolClass.getSchoolYear(),
            schoolClass.getName(),
            schoolClass.getShift()
        );

        schoolClass.setSchool(school);

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional(readOnly = true)
    public SchoolClass findById(Long id) {

        return schoolClassRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Turma não encontrada: " + id
            ));
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findAll() {
        return schoolClassRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findBySchool(Long schoolId) {

        validateSchoolExists(schoolId);

        return schoolClassRepository.findBySchoolId(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findActiveBySchool(Long schoolId) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndActive(schoolId, true);
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findBySchoolAndYear(
        Long schoolId,
        String schoolYear
    ) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndSchoolYearOrderByNameAsc(
                schoolId,
                schoolYear
            );
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findActiveBySchoolAndYear(
        Long schoolId,
        String schoolYear
    ) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndSchoolYearAndActive(
                schoolId,
                schoolYear,
                true
            );
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> search(
        Long schoolId,
        String name
    ) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndNameContainingIgnoreCase(
                schoolId,
                name
            );
    }

    @Transactional
    public SchoolClass update(
        Long id,
        Long schoolId,
        SchoolClass data
    ) {

        SchoolClass current = findById(id);

        Long newSchoolId =
            schoolId != null
                ? schoolId
                : current.getSchool().getId();

        boolean changedUniqueFields =
            !equals(current.getSchool().getId(), newSchoolId)
                || !equals(current.getSchoolYear(), data.getSchoolYear())
                || !equals(current.getName(), data.getName())
                || !equals(current.getShift(), data.getShift());

        if (changedUniqueFields) {
            validateUniqueClass(
                newSchoolId,
                data.getSchoolYear(),
                data.getName(),
                data.getShift(),
                id
            );
        }

        if (!current.getSchool().getId().equals(newSchoolId)) {

            School school = schoolRepository.findById(newSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Escola não encontrada: " + newSchoolId
                ));

            current.setSchool(school);
        }

        current.setName(data.getName());
        current.setSchoolYear(data.getSchoolYear());
        current.setShift(data.getShift());
        current.setGradeLevel(data.getGradeLevel());

        if (data.getActive() != null) {
            current.setActive(data.getActive());
        }

        return schoolClassRepository.save(current);
    }

    @Transactional
    public SchoolClass activate(Long id) {

        SchoolClass schoolClass = findById(id);
        schoolClass.setActive(true);

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public SchoolClass deactivate(Long id) {

        SchoolClass schoolClass = findById(id);
        schoolClass.setActive(false);

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public void delete(Long id) {

        SchoolClass schoolClass = findById(id);

        /*
         * Preservamos turmas que já possuem histórico acadêmico.
         */
        if (!enrollmentRepository.findBySchoolClassId(id).isEmpty()) {
            throw new BusinessException(
                "Não é possível excluir a turma porque ela possui matrículas."
            );
        }

        schoolClassRepository.delete(schoolClass);
    }

    private void validateSchoolExists(Long schoolId) {

        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException(
                "Escola não encontrada: " + schoolId
            );
        }
    }

    private void validateUniqueClass(
        Long schoolId,
        String schoolYear,
        String name,
        String shift
    ) {

        validateUniqueClass(
            schoolId,
            schoolYear,
            name,
            shift,
            null
        );
    }

    private void validateUniqueClass(
        Long schoolId,
        String schoolYear,
        String name,
        String shift,
        Long currentId
    ) {

        boolean exists =
            schoolClassRepository
                .existsBySchoolIdAndSchoolYearAndNameAndShift(
                    schoolId,
                    schoolYear,
                    name,
                    shift
                );

        if (!exists) {
            return;
        }

        if (currentId == null) {
            throw new BusinessException(
                "Já existe uma turma com essa escola, ano, nome e turno."
            );
        }

        SchoolClass existing =
            schoolClassRepository
                .findBySchoolIdAndNameContainingIgnoreCase(
                    schoolId,
                    name
                )
                .stream()
                .filter(item ->
                    item.getSchoolYear() != null
                    && item.getSchoolYear().equals(schoolYear)
                    && equals(item.getShift(), shift)
                )
                .findFirst()
                .orElse(null);

        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(
                "Já existe outra turma com essa escola, ano, nome e turno."
            );
        }
    }

    private boolean equals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}