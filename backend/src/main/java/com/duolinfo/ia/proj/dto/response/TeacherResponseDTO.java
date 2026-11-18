package com.duolinfo.ia.proj.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um professor")
public record TeacherResponseDTO(

    @Schema(example = "5")
    Long id,

    @Schema(example = "Carlos Mendes")
    String name,

    @Schema(example = "carlos@escola.com")
    String email,

    @Schema(example = "123456789")
    String googleId,

    @Schema(example = "1")
    Long institutionId,

    @Schema(example = "[\"Inglês\", \"Espanhol\"]")
    List<String> taughtLanguages,

    @Schema(example = "[\"Conversação\", \"Gramática\"]")
    List<String> specializationAreas,

    String bibliography,

    @Schema(example = "false")
    boolean approved,

    String gender,

    boolean institutionAdmin,

    boolean primaryInstitutionAdmin,

    String approvalStatus,

    String institutionName,

    String institutionPictureUrl
) {
    public TeacherResponseDTO(
            Long id,
            String name,
            String email,
            String googleId,
            Long institutionId,
            List<String> taughtLanguages,
            List<String> specializationAreas,
            String bibliography,
            boolean approved,
            String gender,
            boolean institutionAdmin,
            boolean primaryInstitutionAdmin,
            String approvalStatus,
            String institutionName) {
        this(id, name, email, googleId, institutionId, taughtLanguages, specializationAreas,
                bibliography, approved, gender, institutionAdmin, primaryInstitutionAdmin,
                approvalStatus, institutionName, null);
    }
}