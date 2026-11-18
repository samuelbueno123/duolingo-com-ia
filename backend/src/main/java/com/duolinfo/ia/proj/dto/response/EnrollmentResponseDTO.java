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

    String schoolClassLanguage,

    LocalDate enrollmentDate,

    LocalDate cancellationDate,

    EnrollmentStatus status,

    String notes,

    String schoolClassPictureUrl
) {
    public EnrollmentResponseDTO(
            Long id,
            Long studentId,
            String studentName,
            Long schoolClassId,
            String schoolClassName,
            String schoolClassLanguage,
            LocalDate enrollmentDate,
            LocalDate cancellationDate,
            EnrollmentStatus status,
            String notes) {
        this(id, studentId, studentName, schoolClassId, schoolClassName, schoolClassLanguage,
                enrollmentDate, cancellationDate, status, notes, null);
    }
}