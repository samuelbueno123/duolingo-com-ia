package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.request.SchoolRequestDTO;
import com.duolinfo.ia.proj.dto.response.SchoolResponseDTO;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.SchoolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schools")
@Tag(
    name = "Schools",
    description = "Gerenciamento de escolas"
)
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping("/institution/{institutionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria escola em uma instituição")
    public SchoolResponseDTO create(
            @PathVariable Long institutionId,
            @Valid @RequestBody SchoolRequestDTO request) {

        School school = new School();

        school.setName(request.name());
        school.setInepCode(request.inepCode());

        return DtoMapper.toSchoolResponse(
            schoolService.create(
                institutionId,
                school
            )
        );
    }

    @GetMapping
    @Operation(summary = "Lista escolas")
    public List<SchoolResponseDTO> findAll() {

        return schoolService.findAll()
            .stream()
            .map(DtoMapper::toSchoolResponse)
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca escola por ID")
    public SchoolResponseDTO findById(
            @PathVariable Long id) {

        return DtoMapper.toSchoolResponse(
            schoolService.findById(id)
        );
    }

    @GetMapping("/institution/{institutionId}")
    @Operation(summary = "Lista escolas da instituição")
    public List<SchoolResponseDTO> findByInstitution(
            @PathVariable Long institutionId) {

        return schoolService.findByInstitution(institutionId)
            .stream()
            .map(DtoMapper::toSchoolResponse)
            .toList();
    }

    @GetMapping("/institution/{institutionId}/name/{name}")
    @Operation(summary = "Busca escola por nome dentro da instituição")
    public List<SchoolResponseDTO> searchByName(
            @PathVariable Long institutionId,
            @PathVariable String name) {

        return schoolService
            .searchByNameInInstitution(
                institutionId,
                name
            )
            .stream()
            .map(DtoMapper::toSchoolResponse)
            .toList();
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Busca escolas por nome")
    public List<SchoolResponseDTO> searchByName(
            @PathVariable String name) {

        return schoolService.searchByName(name)
            .stream()
            .map(DtoMapper::toSchoolResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza escola")
    public SchoolResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolRequestDTO request) {

        School school = new School();

        school.setName(request.name());
        school.setInepCode(request.inepCode());

        return DtoMapper.toSchoolResponse(
            schoolService.update(
                id,
                request.institutionId(),
                school
            )
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui escola")
    public void delete(
            @PathVariable Long id) {

        schoolService.delete(id);
    }
}