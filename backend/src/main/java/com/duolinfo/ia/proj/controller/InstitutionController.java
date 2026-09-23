package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.request.InstitutionRequestDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionResponseDTO;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.InstitutionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/institutions")
@Tag(
    name = "Institutions",
    description = "Gerenciamento de instituições"
)
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(
            InstitutionService institutionService) {

        this.institutionService = institutionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria instituição")
    public InstitutionResponseDTO create(
            @Valid @RequestBody InstitutionRequestDTO request) {

        Institution institution = toEntity(request);

        return DtoMapper.toInstitutionResponse(
            institutionService.create(institution)
        );
    }

    @GetMapping
    @Operation(summary = "Lista instituições")
    public List<InstitutionResponseDTO> findAll() {

        return institutionService.findAll()
            .stream()
            .map(DtoMapper::toInstitutionResponse)
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca instituição por ID")
    public InstitutionResponseDTO findById(
            @PathVariable Long id) {

        return DtoMapper.toInstitutionResponse(
            institutionService.findById(id)
        );
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Busca instituição por código")
    public InstitutionResponseDTO findByCode(
            @PathVariable String code) {

        return DtoMapper.toInstitutionResponse(
            institutionService.findByCode(code)
        );
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Busca instituições por nome")
    public List<InstitutionResponseDTO> findByName(
            @PathVariable String name) {

        return institutionService.searchByName(name)
            .stream()
            .map(DtoMapper::toInstitutionResponse)
            .toList();
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Busca instituições por cidade")
    public List<InstitutionResponseDTO> findByCity(
            @PathVariable String city) {

        return institutionService.findByCity(city)
            .stream()
            .map(DtoMapper::toInstitutionResponse)
            .toList();
    }

    @GetMapping("/state/{state}")
    @Operation(summary = "Busca instituições por estado")
    public List<InstitutionResponseDTO> findByState(
            @PathVariable String state) {

        return institutionService.findByState(state)
            .stream()
            .map(DtoMapper::toInstitutionResponse)
            .toList();
    }

    @GetMapping("/active")
    @Operation(summary = "Lista instituições ativas")
    public List<InstitutionResponseDTO> findActive() {

        return institutionService.findActive()
            .stream()
            .map(DtoMapper::toInstitutionResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza instituição")
    public InstitutionResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionRequestDTO request) {

        return DtoMapper.toInstitutionResponse(
            institutionService.update(
                id,
                toEntity(request)
            )
        );
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Ativa instituição")
    public InstitutionResponseDTO activate(
            @PathVariable Long id) {

        return DtoMapper.toInstitutionResponse(
            institutionService.activate(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativa instituição")
    public InstitutionResponseDTO deactivate(
            @PathVariable Long id) {

        return DtoMapper.toInstitutionResponse(
            institutionService.deactivate(id)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui instituição")
    public void delete(
            @PathVariable Long id) {

        institutionService.delete(id);
    }

    private Institution toEntity(
            InstitutionRequestDTO request) {

        Institution institution =
            new Institution();

        institution.setName(request.name());
        institution.setCode(request.code());
        institution.setEmail(request.email());
        institution.setPhone(request.phone());
        institution.setWebsite(request.website());
        institution.setCity(request.city());
        institution.setState(request.state());
        institution.setCountry(request.country());
        institution.setAddress(request.address());
        institution.setDescription(request.description());

        if (request.active() != null) {
            institution.setActive(request.active());
        }

        return institution;
    }
}