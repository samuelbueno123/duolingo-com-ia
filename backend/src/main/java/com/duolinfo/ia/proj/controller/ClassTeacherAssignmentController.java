package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.request.ClassTeacherAssignmentFinishDTO;
import com.duolinfo.ia.proj.dto.request.ClassTeacherAssignmentRequestDTO;
import com.duolinfo.ia.proj.dto.response.ClassTeacherAssignmentResponseDTO;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.ClassTeacherAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/class-teacher-assignments")
@Tag(
    name = "Class Teacher Assignments",
    description = "Histórico de professores das turmas"
)
public class ClassTeacherAssignmentController {

    private final ClassTeacherAssignmentService assignmentService;

    public ClassTeacherAssignmentController(
            ClassTeacherAssignmentService assignmentService) {

        this.assignmentService = assignmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Atribui professor à turma",
        description = """
            Atribui um professor a uma turma.
            Caso já exista um professor atual, a atribuição anterior
            será encerrada automaticamente.
            """
    )
    public ClassTeacherAssignmentResponseDTO assignTeacher(
            @Valid
            @RequestBody
            ClassTeacherAssignmentRequestDTO request) {

        return DtoMapper.toAssignmentResponse(
            assignmentService.assignTeacher(
                request.schoolClassId(),
                request.teacherId(),
                request.startDate(),
                request.reason()
            )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca atribuição por ID")
    public ClassTeacherAssignmentResponseDTO findById(
            @PathVariable Long id) {

        return DtoMapper.toAssignmentResponse(
            assignmentService.findById(id)
        );
    }

    @GetMapping("/class/{schoolClassId}")
    @Operation(summary = "Lista histórico de professores da turma")
    public List<ClassTeacherAssignmentResponseDTO> findByClass(
            @PathVariable Long schoolClassId) {

        return assignmentService
            .findByClass(schoolClassId)
            .stream()
            .map(DtoMapper::toAssignmentResponse)
            .toList();
    }

    @GetMapping("/class/{schoolClassId}/current")
    @Operation(summary = "Busca professor atual da turma")
    public ClassTeacherAssignmentResponseDTO findCurrentTeacher(
            @PathVariable Long schoolClassId) {

        return DtoMapper.toAssignmentResponse(
            assignmentService.findCurrentTeacher(
                schoolClassId
            )
        );
    }

    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "Lista turmas do professor")
    public List<ClassTeacherAssignmentResponseDTO> findByTeacher(
            @PathVariable Long teacherId) {

        return assignmentService
            .findByTeacher(teacherId)
            .stream()
            .map(DtoMapper::toAssignmentResponse)
            .toList();
    }

    @PostMapping("/class/{schoolClassId}/finish")
    @Operation(summary = "Encerra professor atual da turma")
    public ClassTeacherAssignmentResponseDTO finishCurrentAssignment(
            @PathVariable Long schoolClassId,
            @RequestBody(required = false)
            ClassTeacherAssignmentFinishDTO request) {

        if (request == null) {
            request =
                new ClassTeacherAssignmentFinishDTO(null);
        }

        return DtoMapper.toAssignmentResponse(
            assignmentService.finishCurrentAssignment(
                schoolClassId,
                request.endDate()
            )
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma atribuição")
    public void delete(
            @PathVariable Long id) {

        assignmentService.delete(id);
    }
}