package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDate;

import com.duolinfo.ia.proj.entity.EnrollmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma matrícula")
public record EnrollmentResponseDTO(

    Long id,

    Long studentId,

    String studentName,

    Long schoolClassId,

    String schoolClassName,

    LocalDate enrollmentDate,

    LocalDate cancellationDate,

    EnrollmentStatus status,

    String notes
) {
}