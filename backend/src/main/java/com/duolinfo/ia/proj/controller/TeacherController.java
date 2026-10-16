package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.TeacherApprovalRequestDTO;
import com.duolinfo.ia.proj.dto.request.TeacherRequestDTO;
import com.duolinfo.ia.proj.dto.response.ClassTeacherAssignmentResponseDTO;
import com.duolinfo.ia.proj.dto.response.TeacherResponseDTO;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.mapper.RequestMapper;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.service.TeacherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teachers")
@Tag(
    name = "Teachers",
    description = "Gerenciamento de professores"
)
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cria um novo professor",
        description = "Cria um professor associado a uma instituição."
    )
    public TeacherResponseDTO create(
            @Valid @RequestBody TeacherRequestDTO request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A senha é obrigatória para criar uma conta de professor.");
        }
        return DtoMapper.toTeacherResponse(
            teacherService.create(
                request.institutionId(),
                RequestMapper.toNewTeacher(request)
            )
        );
    }

    @GetMapping
    @Operation(summary = "Lista professores")
    public List<TeacherResponseDTO> listAll() {

        return teacherService.findAll()
            .stream()
            .map(DtoMapper::toTeacherResponse)
            .toList();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Busca o perfil do professor autenticado")
    public TeacherResponseDTO findMyProfile(Authentication authentication) {
        return DtoMapper.toTeacherResponse(
            requireTeacher(teacherService.findByEmail(authentication.getName()))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca professor por ID")
    public TeacherResponseDTO findById(
            @PathVariable Long id) {

        return DtoMapper.toTeacherResponse(teacherService.findById(id));
    }

    @GetMapping("/google/{googleId}")
    @Operation(summary = "Busca professor por Google ID")
    public TeacherResponseDTO findByGoogleId(
            @PathVariable String googleId) {

        return DtoMapper.toTeacherResponse(
            requireTeacher(teacherService.findByGoogleId(googleId))
        );
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Busca professor por email")
    public TeacherResponseDTO findByEmail(
            @PathVariable String email) {

        return DtoMapper.toTeacherResponse(
            requireTeacher(teacherService.findByEmail(email))
        );
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Busca professores por nome")
    public List<TeacherResponseDTO> findByName(
            @PathVariable String name) {

        return teacherService.findByName(name)
            .stream()
            .map(DtoMapper::toTeacherResponse)
            .toList();
    }

    @GetMapping("/institution/name/{institution}")
    @Operation(summary = "Busca professores pelo nome da instituição")
    public List<TeacherResponseDTO> findByInstitution(
            @PathVariable String institution) {

        return teacherService.findByInstitution(institution)
            .stream()
            .map(DtoMapper::toTeacherResponse)
            .toList();
    }

    @GetMapping("/institution/{institutionId}")
    @Operation(summary = "Lista professores da instituição")
    public List<TeacherResponseDTO> findByInstitutionId(
            @PathVariable Long institutionId) {

        return teacherService.findByInstitutionId(institutionId)
            .stream()
            .map(DtoMapper::toTeacherResponse)
            .toList();
    }

    @GetMapping("/institution/{institutionId}/language/{language}")
    @Operation(summary = "Busca professores por idioma")
    public List<TeacherResponseDTO> findByTaughtLanguage(
            @PathVariable Long institutionId,
            @PathVariable String language) {

        return teacherService.findByTaughtLanguage(
                institutionId,
                language
            )
            .stream()
            .map(DtoMapper::toTeacherResponse)
            .toList();
    }

    @GetMapping(
        "/institution/{institutionId}/specialization/{specializationArea}"
    )
    @Operation(summary = "Busca professores por especialização")
    public List<TeacherResponseDTO> findBySpecializationArea(
            @PathVariable Long institutionId,
            @PathVariable String specializationArea) {

        return teacherService.findBySpecializationArea(
                institutionId,
                specializationArea
            )
            .stream()
            .map(DtoMapper::toTeacherResponse)
            .toList();
    }

    @GetMapping("/{teacherId}/assignments")
    @Operation(summary = "Lista histórico de turmas do professor")
    public List<ClassTeacherAssignmentResponseDTO>
        findClassAssignments(
            @PathVariable Long teacherId) {

        return teacherService.findClassAssignments(teacherId)
            .stream()
            .map(DtoMapper::toAssignmentResponse)
            .toList();
    }

    @GetMapping("/{teacherId}/assignments/exists")
    @Operation(summary = "Verifica se professor possui histórico")
    public boolean hasClassAssignments(
            @PathVariable Long teacherId) {

        return teacherService.hasClassAssignments(teacherId);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza professor")
    public void update(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody TeacherRequestDTO request) {

        ensureCanUpdateTeacher(id, authentication, request);
        teacherService.update(
            id,
            request.institutionId(),
            RequestMapper.toTeacherUpdate(id, request)
        );
    }

    @PatchMapping("/{id}/approval")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Aprova ou revoga o acesso de um professor")
    public TeacherResponseDTO updateApproval(
            @PathVariable Long id,
            @Valid @RequestBody TeacherApprovalRequestDTO request) {
        return DtoMapper.toTeacherResponse(
            teacherService.updateApproval(id, request.approved())
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Exclui professor")
    public void deleteById(
            @PathVariable Long id) {

        teacherService.deleteById(id);
    }

    private void ensureCanUpdateTeacher(
            Long teacherId,
            Authentication authentication,
            TeacherRequestDTO request) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (isAdmin) {
            return;
        }

        Teacher authenticatedTeacher = teacherService.findByEmail(authentication.getName());
        if (authenticatedTeacher == null
                || !teacherId.equals(authenticatedTeacher.getId())) {
            throw new AccessDeniedException(
                "Você só pode atualizar o seu próprio perfil de professor.");
        }

        if (request.email() != null
                && !request.email().isBlank()
                && !request.email().trim().equalsIgnoreCase(authenticatedTeacher.getEmail())) {
            throw new AccessDeniedException(
                "Somente um administrador pode alterar o email de um professor.");
        }

        Long currentInstitutionId = authenticatedTeacher.getInstitution() == null
            ? null
            : authenticatedTeacher.getInstitution().getId();
        if (request.institutionId() != null
                && !request.institutionId().equals(currentInstitutionId)) {
            throw new AccessDeniedException(
                "Somente um administrador pode alterar a instituição de um professor.");
        }
    }

    private static Teacher requireTeacher(Teacher teacher) {
        if (teacher == null) {
            throw new ResourceNotFoundException("Professor não encontrado.");
        }
        return teacher;
    }
}
