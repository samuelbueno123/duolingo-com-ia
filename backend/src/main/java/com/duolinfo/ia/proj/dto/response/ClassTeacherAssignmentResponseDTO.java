package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Histórico de professor de uma turma")
public record ClassTeacherAssignmentResponseDTO(

    Long id,

    Long schoolClassId,

    String schoolClassName,

    Long teacherId,

    String teacherName,

    LocalDate startDate,

    LocalDate endDate,

    String reason
) {
}