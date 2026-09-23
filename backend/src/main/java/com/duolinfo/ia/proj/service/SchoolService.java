package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final InstitutionRepository institutionRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public School create(Long institutionId, School school) {

        Institution institution = institutionRepository.findById(institutionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Instituição não encontrada: " + institutionId
            ));

        if (school.getInepCode() != null) {

            schoolRepository.findByInepCode(school.getInepCode())
                .ifPresent(existing -> {
                    throw new BusinessException(
                        "Já existe uma escola com o código INEP: "
                            + school.getInepCode()
                    );
                });
        }

        school.setInstitution(institution);

        return schoolRepository.save(school);
    }

    @Transactional(readOnly = true)
    public School findById(Long id) {

        return schoolRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Escola não encontrada: " + id
            ));
    }

    @Transactional(readOnly = true)
    public List<School> findAll() {
        return schoolRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<School> findByInstitution(Long institutionId) {

        if (!institutionRepository.existsById(institutionId)) {
            throw new ResourceNotFoundException(
                "Instituição não encontrada: " + institutionId
            );
        }

        return schoolRepository.findByInstitutionId(institutionId);
    }

    @Transactional(readOnly = true)
    public List<School> searchByName(String name) {
        return schoolRepository
            .findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<School> searchByNameInInstitution(
        Long institutionId,
        String name
    ) {

        return schoolRepository
            .findByInstitutionIdAndNameContainingIgnoreCase(
                institutionId,
                name
            );
    }

    @Transactional
    public School update(
        Long id,
        Long institutionId,
        School data
    ) {

        School school = findById(id);

        if (data.getInepCode() != null
                && !data.getInepCode().equals(school.getInepCode())) {

            schoolRepository.findByInepCode(data.getInepCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException(
                            "Já existe uma escola com o código INEP: "
                                + data.getInepCode()
                        );
                    }
                });
        }

        if (institutionId != null
                && !school.getInstitution().getId().equals(institutionId)) {

            Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Instituição não encontrada: " + institutionId
                ));

            school.setInstitution(institution);
        }

        school.setName(data.getName());
        school.setInepCode(data.getInepCode());

        return schoolRepository.save(school);
    }

    @Transactional
    public void delete(Long id) {

        School school = findById(id);

        /*
         * Não apagamos a escola com turmas existentes,
         * porque as turmas possuem matrícula e histórico.
         */
        if (!schoolClassRepository.findBySchoolId(id).isEmpty()) {
            throw new BusinessException(
                "Não é possível excluir a escola porque ela possui turmas."
            );
        }

        schoolRepository.delete(school);
    }
}