package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Documento/Anexo do professor (diploma, currículo, etc.)")
public record TeacherDocumentRequestDTO(

    @Schema(description = "Nome do arquivo", example = "diploma.pdf")
    String fileName,

    @Schema(description = "Tipo MIME do arquivo", example = "application/pdf")
    String contentType,

    @Schema(description = "Tamanho do arquivo em bytes", example = "102450")
    Long fileSize,

    @Schema(description = "Dados do arquivo em Base64")
    String fileData
) {
}
