package com.duolinfo.ia.proj.controller;

import java.util.List;

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
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.TeacherRequestDTO;
import com.duolinfo.ia.proj.dto.response.ClassTeacherAssignmentResponseDTO;
import com.duolinfo.ia.proj.dto.response.TeacherResponseDTO;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.mapper.DtoMapper;
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

        Teacher teacher = new Teacher();

        teacher.setName(request.name());
        teacher.setEmail(request.email());
        teacher.setGoogleId(request.googleId());
        teacher.setTaughtLanguages(request.taughtLanguages());
        teacher.setSpecializationAreas(request.specializationAreas());
        teacher.setBibliography(request.bibliography());

        return DtoMapper.toTeacherResponse(
            teacherService.create(
                request.institutionId(),
                teacher
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

    @GetMapping("/{id}")
    @Operation(summary = "Busca professor por ID")
    public TeacherResponseDTO findById(
            @PathVariable Long id) {

        Teacher teacher = teacherService.findById(id);

        if (teacher == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Professor não encontrado."
            );
        }

        return DtoMapper.toTeacherResponse(teacher);
    }

    @GetMapping("/google/{googleId}")
    @Operation(summary = "Busca professor por Google ID")
    public TeacherResponseDTO findByGoogleId(
            @PathVariable String googleId) {

        Teacher teacher =
            teacherService.findByGoogleId(googleId);

        if (teacher == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Professor não encontrado."
            );
        }

        return DtoMapper.toTeacherResponse(teacher);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Busca professor por email")
    public TeacherResponseDTO findByEmail(
            @PathVariable String email) {

        Teacher teacher =
            teacherService.findByEmail(email);

        if (teacher == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Professor não encontrado."
            );
        }

        return DtoMapper.toTeacherResponse(teacher);
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
    @PreAuthorize("hasRole('TEACHER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza professor")
    public void update(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequestDTO request) {

        Teacher existing = teacherService.findById(id);

        if (existing == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Professor não encontrado."
            );
        }

        Teacher teacher = new Teacher();

        teacher.setId(id);
        teacher.setName(request.name());
        teacher.setEmail(request.email());
        teacher.setGoogleId(request.googleId());
        teacher.setTaughtLanguages(
            request.taughtLanguages()
        );
        teacher.setSpecializationAreas(
            request.specializationAreas()
        );
        teacher.setBibliography(
            request.bibliography()
        );

        teacherService.update(
            id,
            request.institutionId(),
            teacher
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Exclui professor")
    public void deleteById(
            @PathVariable Long id) {

        Teacher existing = teacherService.findById(id);

        if (existing == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Professor não encontrado."
            );
        }

        teacherService.deleteById(id);
    }
}