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

import com.duolinfo.ia.proj.dto.request.SchoolClassRequestDTO;
import com.duolinfo.ia.proj.dto.response.SchoolClassResponseDTO;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.SchoolClassService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
@Tag(
    name = "Classes",
    description = "Gerenciamento de turmas"
)
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(
            SchoolClassService schoolClassService) {

        this.schoolClassService = schoolClassService;
    }

    @PostMapping("/school/{schoolId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria turma em uma escola")
    public SchoolClassResponseDTO create(
            @PathVariable Long schoolId,
            @Valid @RequestBody SchoolClassRequestDTO request) {

        SchoolClass schoolClass = toEntity(request);

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.create(
                schoolId,
                schoolClass
            )
        );
    }

    @GetMapping
    @Operation(summary = "Lista turmas")
    public List<SchoolClassResponseDTO> findAll() {

        return schoolClassService.findAll()
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca turma por ID")
    public SchoolClassResponseDTO findById(
            @PathVariable Long id) {

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.findById(id)
        );
    }

    @GetMapping("/school/{schoolId}")
    @Operation(summary = "Lista turmas da escola")
    public List<SchoolClassResponseDTO> findBySchool(
            @PathVariable Long schoolId) {

        return schoolClassService.findBySchool(schoolId)
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/active")
    @Operation(summary = "Lista turmas ativas da escola")
    public List<SchoolClassResponseDTO> findActiveBySchool(
            @PathVariable Long schoolId) {

        return schoolClassService.findActiveBySchool(schoolId)
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/year/{schoolYear}")
    @Operation(summary = "Lista turmas de determinado ano")
    public List<SchoolClassResponseDTO> findBySchoolAndYear(
            @PathVariable Long schoolId,
            @PathVariable String schoolYear) {

        return schoolClassService
            .findBySchoolAndYear(
                schoolId,
                schoolYear
            )
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/year/{schoolYear}/active")
    @Operation(summary = "Lista turmas ativas de determinado ano")
    public List<SchoolClassResponseDTO>
        findActiveBySchoolAndYear(
            @PathVariable Long schoolId,
            @PathVariable String schoolYear) {

        return schoolClassService
            .findActiveBySchoolAndYear(
                schoolId,
                schoolYear
            )
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/search/{name}")
    @Operation(summary = "Pesquisa turma pelo nome")
    public List<SchoolClassResponseDTO> search(
            @PathVariable Long schoolId,
            @PathVariable String name) {

        return schoolClassService.search(
                schoolId,
                name
            )
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza turma")
    public SchoolClassResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolClassRequestDTO request) {

        SchoolClass schoolClass = toEntity(request);

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.update(
                id,
                request.schoolId(),
                schoolClass
            )
        );
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Ativa turma")
    public SchoolClassResponseDTO activate(
            @PathVariable Long id) {

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.activate(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativa turma")
    public SchoolClassResponseDTO deactivate(
            @PathVariable Long id) {

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.deactivate(id)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui turma")
    public void delete(
            @PathVariable Long id) {

        schoolClassService.delete(id);
    }

    private SchoolClass toEntity(
            SchoolClassRequestDTO request) {

        SchoolClass schoolClass =
            new SchoolClass();

        schoolClass.setName(request.name());
        schoolClass.setSchoolYear(
            request.schoolYear()
        );
        schoolClass.setShift(request.shift());
        schoolClass.setGradeLevel(
            request.gradeLevel()
        );

        if (request.active() != null) {
            schoolClass.setActive(
                request.active()
            );
        }

        return schoolClass;
    }
}