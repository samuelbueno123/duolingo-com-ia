package com.duolinfo.ia.proj.dto.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Idioma e nível de proficiência do estudante")
public record StudentLanguageRequestDTO(

    @Schema(description = "Nome do idioma", example = "Inglês")
    String languageName,

    @Schema(description = "Nível de proficiência", example = "Intermediário")
    String level,

    @Schema(description = "Pontuação inicial (opcional)", example = "0")
    Integer score
) {

    public StudentLanguageRequestDTO(String languageName, String level) {
        this(languageName, level, 0);
    }

    @JsonCreator
    public static StudentLanguageRequestDTO fromJson(Object input) {
        if (input instanceof String str) {
            if (str.contains(":")) {
                String[] parts = str.split(":", 2);
                return new StudentLanguageRequestDTO(parts[0].trim(), parts[1].trim(), 0);
            }
            return new StudentLanguageRequestDTO(str.trim(), "Iniciante", 0);
        } else if (input instanceof Map<?, ?> map) {
            String name = (String) map.get("languageName");
            if (name == null) name = (String) map.get("language");
            if (name == null) name = (String) map.get("name");

            String level = (String) map.get("level");
            if (level == null || level.isBlank()) level = "Iniciante";

            Integer score = 0;
            if (map.get("score") instanceof Number num) {
                score = num.intValue();
            }

            return new StudentLanguageRequestDTO(
                name != null ? name.trim() : "",
                level.trim(),
                score
            );
        }
        return new StudentLanguageRequestDTO("", "Iniciante", 0);
    }
}
