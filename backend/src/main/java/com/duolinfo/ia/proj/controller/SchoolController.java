package com.duolinfo.ia.proj.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.mapper.RequestMapper;
import com.duolinfo.ia.proj.service.AccessControlService;
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

    @Autowired(required = false)
    private AccessControlService accessControlService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping("/institution/{institutionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Cria escola em uma instituição")
    public SchoolResponseDTO create(
            @PathVariable Long institutionId,
            @Valid @RequestBody SchoolRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertCanManageInstitution(institutionId);
        }
        return DtoMapper.toSchoolResponse(
            schoolService.create(
                institutionId,
                RequestMapper.toSchool(request)
            )
        );
    }

    @GetMapping
    @Operation(summary = "Lista escolas")
    public List<SchoolResponseDTO> findAll() {
        return filterSchoolsForCaller(schoolService.findAll())
            .stream()
            .map(DtoMapper::toSchoolResponse)
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca escola por ID")
    public SchoolResponseDTO findById(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanViewSchool(id);
        }
        return DtoMapper.toSchoolResponse(
            schoolService.findById(id)
        );
    }

    @GetMapping("/institution/{institutionId}")
    @Operation(summary = "Lista escolas da instituição")
    public List<SchoolResponseDTO> findByInstitution(
            @PathVariable Long institutionId) {
        if (accessControlService != null) {
            var current = accessControlService.getCurrentUserOrThrow();
            if (!current.isSuperAdmin()) {
                Long userInstId = accessControlService.getUserInstitutionId(current);
                if (userInstId != null && !Objects.equals(userInstId, institutionId)) {
                    return List.of();
                }
            }
        }
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
        if (accessControlService != null) {
            var current = accessControlService.getCurrentUserOrThrow();
            if (!current.isSuperAdmin()) {
                Long userInstId = accessControlService.getUserInstitutionId(current);
                if (userInstId != null && !Objects.equals(userInstId, institutionId)) {
                    return List.of();
                }
            }
        }
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
        return filterSchoolsForCaller(schoolService.searchByName(name))
            .stream()
            .map(DtoMapper::toSchoolResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Atualiza escola")
    public SchoolResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchool(id);
            if (request.institutionId() != null) {
                accessControlService.assertCanManageInstitution(request.institutionId());
            }
        }
        return DtoMapper.toSchoolResponse(
            schoolService.update(
                id,
                request.institutionId(),
                RequestMapper.toSchool(request)
            )
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclui escola")
    public void delete(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchool(id);
        }
        schoolService.delete(id);
    }

    private List<School> filterSchoolsForCaller(List<School> schools) {
        if (accessControlService == null) {
            return schools;
        }
        User current = accessControlService.getCurrentUser().orElse(null);
        if (current == null || current.isSuperAdmin()) {
            return schools;
        }
        Long userInstId = accessControlService.getUserInstitutionId(current);
        return schools.stream()
            .filter(s -> {
                Long sInstId = s.getInstitution() != null ? s.getInstitution().getId() : null;
                return userInstId != null && Objects.equals(userInstId, sInstId);
            })
            .toList();
    }
}
