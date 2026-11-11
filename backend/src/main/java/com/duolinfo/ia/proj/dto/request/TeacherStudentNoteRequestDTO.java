package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeacherStudentNoteRequestDTO(
    @NotNull(message = "O ID do estudante é obrigatório.")
    Long studentId,

    Long schoolClassId,

    @NotBlank(message = "O conteúdo da anotação é obrigatório.")
    String note
) {}
