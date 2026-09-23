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

import com.duolinfo.ia.proj.dto.request.EnrollmentActionDTO;
import com.duolinfo.ia.proj.dto.request.EnrollmentRequestDTO;
import com.duolinfo.ia.proj.dto.request.EnrollmentTransferDTO;
import com.duolinfo.ia.proj.dto.response.EnrollmentResponseDTO;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.EnrollmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/enrollments")
@Tag(
    name = "Enrollments",
    description = "Gerenciamento de matrículas"
)
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(
            EnrollmentService enrollmentService) {

        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Matricula estudante em turma"
    )
    public EnrollmentResponseDTO enrollStudent(
            @Valid @RequestBody EnrollmentRequestDTO request) {

        return DtoMapper.toEnrollmentResponse(
            enrollmentService.enrollStudent(
                request.studentId(),
                request.schoolClassId(),
                request.enrollmentDate(),
                request.notes()
            )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca matrícula por ID")
    public EnrollmentResponseDTO findById(
            @PathVariable Long id) {

        return DtoMapper.toEnrollmentResponse(
            enrollmentService.findById(id)
        );
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Lista matrículas do estudante")
    public List<EnrollmentResponseDTO> findByStudent(
            @PathVariable Long studentId) {

        return enrollmentService.findByStudent(studentId)
            .stream()
            .map(DtoMapper::toEnrollmentResponse)
            .toList();
    }

    @GetMapping("/student/{studentId}/active")
    @Operation(summary = "Lista matrículas ativas do estudante")
    public List<EnrollmentResponseDTO> findActiveByStudent(
            @PathVariable Long studentId) {

        return enrollmentService
            .findActiveByStudent(studentId)
            .stream()
            .map(DtoMapper::toEnrollmentResponse)
            .toList();
    }

    @GetMapping("/class/{schoolClassId}")
    @Operation(summary = "Lista matrículas da turma")
    public List<EnrollmentResponseDTO> findByClass(
            @PathVariable Long schoolClassId) {

        return enrollmentService
            .findByClass(schoolClassId)
            .stream()
            .map(DtoMapper::toEnrollmentResponse)
            .toList();
    }

    @GetMapping("/class/{schoolClassId}/active")
    @Operation(summary = "Lista matrículas ativas da turma")
    public List<EnrollmentResponseDTO> findActiveByClass(
            @PathVariable Long schoolClassId) {

        return enrollmentService
            .findActiveByClass(schoolClassId)
            .stream()
            .map(DtoMapper::toEnrollmentResponse)
            .toList();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela matrícula")
    public EnrollmentResponseDTO cancel(
            @PathVariable Long id,
            @RequestBody(required = false)
            EnrollmentActionDTO request) {

        if (request == null) {
            request = new EnrollmentActionDTO(
                null,
                null
            );
        }

        return DtoMapper.toEnrollmentResponse(
            enrollmentService.cancel(
                id,
                request.date(),
                request.notes()
            )
        );
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Conclui matrícula")
    public EnrollmentResponseDTO complete(
            @PathVariable Long id,
            @RequestBody(required = false)
            EnrollmentActionDTO request) {

        if (request == null) {
            request = new EnrollmentActionDTO(
                null,
                null
            );
        }

        return DtoMapper.toEnrollmentResponse(
            enrollmentService.complete(
                id,
                request.date(),
                request.notes()
            )
        );
    }

    @PostMapping("/student/{studentId}/transfer")
    @Operation(summary = "Transfere estudante de turma")
    public EnrollmentResponseDTO transfer(
            @PathVariable Long studentId,
            @Valid @RequestBody EnrollmentTransferDTO request) {

        return DtoMapper.toEnrollmentResponse(
            enrollmentService.transfer(
                studentId,
                request.newSchoolClassId(),
                request.transferDate(),
                request.notes()
            )
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui matrícula")
    public void delete(
            @PathVariable Long id) {

        enrollmentService.delete(id);
    }
}