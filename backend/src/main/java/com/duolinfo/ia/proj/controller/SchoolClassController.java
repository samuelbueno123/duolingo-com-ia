package com.duolinfo.ia.proj.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
import com.duolinfo.ia.proj.dto.response.ClassDashboardResponseDTO;
import com.duolinfo.ia.proj.dto.response.SchoolClassResponseDTO;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.mapper.RequestMapper;
import com.duolinfo.ia.proj.service.AccessControlService;
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

    @Autowired(required = false)
    private AccessControlService accessControlService;

    public SchoolClassController(
            SchoolClassService schoolClassService) {

        this.schoolClassService = schoolClassService;
    }

    @PostMapping("/my-institution")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria turma automaticamente vinculada à instituição do professor autenticado")
    public SchoolClassResponseDTO createForMyInstitution(
            @Valid @RequestBody SchoolClassRequestDTO request,
            Authentication authentication) {

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.createForTeacherInstitution(
                authentication.getName(),
                RequestMapper.toSchoolClass(request)
            )
        );
    }

    @PostMapping("/school/{schoolId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria turma em uma escola")
    public SchoolClassResponseDTO create(
            @PathVariable Long schoolId,
            @Valid @RequestBody SchoolClassRequestDTO request) {

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.create(
                schoolId,
                RequestMapper.toSchoolClass(request)
            )
        );
    }

    @GetMapping
    @Operation(summary = "Lista turmas")
    public List<SchoolClassResponseDTO> findAll() {

        return filterClassesForCaller(schoolClassService.findAll())
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/{id}/dashboard")
    @Operation(summary = "Retorna o dashboard completo com estatísticas de uma turma")
    public ClassDashboardResponseDTO getDashboard(@PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        return schoolClassService.getClassDashboard(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca turma por ID")
    public SchoolClassResponseDTO findById(
            @PathVariable Long id) {
        SchoolClass schoolClass = schoolClassService.findById(id);
        if (accessControlService != null) {
            accessControlService.assertCanViewSchoolClass(schoolClass);
        }
        return DtoMapper.toSchoolClassResponse(schoolClass);
    }

    @GetMapping("/school/{schoolId}")
    @Operation(summary = "Lista turmas da escola")
    public List<SchoolClassResponseDTO> findBySchool(
            @PathVariable Long schoolId) {

        return filterClassesForCaller(schoolClassService.findBySchool(schoolId))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/active")
    @Operation(summary = "Lista turmas ativas da escola")
    public List<SchoolClassResponseDTO> findActiveBySchool(
            @PathVariable Long schoolId) {

        return filterClassesForCaller(schoolClassService.findActiveBySchool(schoolId))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/year/{schoolYear}")
    @Operation(summary = "Lista turmas de determinado ano")
    public List<SchoolClassResponseDTO> findBySchoolAndYear(
            @PathVariable Long schoolId,
            @PathVariable String schoolYear) {

        return filterClassesForCaller(schoolClassService.findBySchoolAndYear(schoolId, schoolYear))
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

        return filterClassesForCaller(schoolClassService.findActiveBySchoolAndYear(schoolId, schoolYear))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/search/{name}")
    @Operation(summary = "Pesquisa turma pelo nome")
    public List<SchoolClassResponseDTO> search(
            @PathVariable Long schoolId,
            @PathVariable String name) {

        return filterClassesForCaller(schoolClassService.search(schoolId, name))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza turma")
    public SchoolClassResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolClassRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.update(
                id,
                request.schoolId(),
                RequestMapper.toSchoolClass(request)
            )
        );
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Ativa turma")
    public SchoolClassResponseDTO activate(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.activate(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativa turma")
    public SchoolClassResponseDTO deactivate(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.deactivate(id)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui turma")
    public void delete(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        schoolClassService.delete(id);
    }

    private List<SchoolClass> filterClassesForCaller(List<SchoolClass> classes) {
        if (accessControlService == null) {
            return classes;
        }
        User current = accessControlService.getCurrentUser().orElse(null);
        if (current == null || current.isSuperAdmin()) {
            return classes;
        }
        Long userInstId = accessControlService.getUserInstitutionId(current);
        return classes.stream()
            .filter(sc -> {
                Long classInstId = accessControlService.getSchoolClassInstitutionId(sc);
                return userInstId != null && Objects.equals(userInstId, classInstId);
            })
            .toList();
    }
}
