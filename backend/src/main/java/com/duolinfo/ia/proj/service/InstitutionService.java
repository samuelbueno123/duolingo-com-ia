package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final SchoolRepository schoolRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    public Institution create(Institution institution) {

        if (institutionRepository.existsByCode(institution.getCode())) {
            throw new BusinessException(
                "Já existe uma instituição com o código: "
                    + institution.getCode()
            );
        }

        if (institutionRepository.existsByEmail(institution.getEmail())) {
            throw new BusinessException(
                "Já existe uma instituição com o email: "
                    + institution.getEmail()
            );
        }

        if (institution.getPhone() != null
                && institutionRepository.existsByPhone(institution.getPhone())) {

            throw new BusinessException(
                "Já existe uma instituição com o telefone: "
                    + institution.getPhone()
            );
        }

        return institutionRepository.save(institution);
    }

    @Transactional(readOnly = true)
    public Institution findById(Long id) {

        return institutionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Instituição não encontrada: " + id
            ));
    }

    @Transactional(readOnly = true)
    public List<Institution> findAll() {
        return institutionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Institution> findActive() {
        return institutionRepository.findByActive(true);
    }

    @Transactional(readOnly = true)
    public List<Institution> searchByName(String name) {
        return institutionRepository
            .findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Institution> findByCity(String city) {
        return institutionRepository
            .findByCityContainingIgnoreCase(city);
    }

    @Transactional(readOnly = true)
    public List<Institution> findByState(String state) {
        return institutionRepository
            .findByStateContainingIgnoreCase(state);
    }

    @Transactional(readOnly = true)
    public Institution findByCode(String code) {

        return institutionRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Instituição não encontrada pelo código: " + code
            ));
    }

    @Transactional
    public Institution update(Long id, Institution data) {

        Institution institution = findById(id);

        if (!institution.getCode().equals(data.getCode())) {

            institutionRepository.findByCode(data.getCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException(
                            "Já existe uma instituição com o código: "
                                + data.getCode()
                        );
                    }
                });
        }

        if (!institution.getEmail().equals(data.getEmail())) {

            institutionRepository.findByEmail(data.getEmail())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException(
                            "Já existe uma instituição com o email: "
                                + data.getEmail()
                        );
                    }
                });
        }

        if (data.getPhone() != null
                && !data.getPhone().equals(institution.getPhone())) {

            institutionRepository.findByPhone(data.getPhone())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException(
                            "Já existe uma instituição com o telefone: "
                                + data.getPhone()
                        );
                    }
                });
        }

        institution.setName(data.getName());
        institution.setCode(data.getCode());
        institution.setEmail(data.getEmail());
        institution.setPhone(data.getPhone());
        institution.setWebsite(data.getWebsite());
        institution.setCity(data.getCity());
        institution.setState(data.getState());
        institution.setCountry(data.getCountry());
        institution.setAddress(data.getAddress());
        institution.setDescription(data.getDescription());

        if (data.getActive() != null) {
            institution.setActive(data.getActive());
        }

        return institutionRepository.save(institution);
    }

    @Transactional
    public Institution activate(Long id) {

        Institution institution = findById(id);
        institution.setActive(true);

        return institutionRepository.save(institution);
    }

    @Transactional
    public Institution deactivate(Long id) {

        Institution institution = findById(id);
        institution.setActive(false);

        return institutionRepository.save(institution);
    }

    @Transactional
    public void delete(Long id) {

        Institution institution = findById(id);

        /*
         * Não apagamos uma instituição que ainda possua escolas
         * ou professores. Isso evita excluir toda a estrutura
         * acadêmica por acidente.
         */
        if (!schoolRepository.findByInstitutionId(id).isEmpty()) {
            throw new BusinessException(
                "Não é possível excluir a instituição porque ela possui escolas."
            );
        }

        if (!teacherRepository.findByInstitutionId(id).isEmpty()) {
            throw new BusinessException(
                "Não é possível excluir a instituição porque ela possui professores."
            );
        }

        institutionRepository.delete(institution);
    }
}