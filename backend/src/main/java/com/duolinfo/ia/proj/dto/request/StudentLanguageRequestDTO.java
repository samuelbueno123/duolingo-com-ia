package com.duolinfo.ia.proj.dto.request;

import java.util.Map;
import java.util.Objects;

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
                String parsedLevel = parts[1].trim();
                return new StudentLanguageRequestDTO(
                    parts[0].trim(),
                    parsedLevel.isEmpty() ? "Básico" : parsedLevel,
                    0
                );
            }
            return new StudentLanguageRequestDTO(str.trim(), "Básico", 0);
        } else if (input instanceof Map<?, ?> map) {
            String name = firstNonBlank(map, "languageName", "language", "name");
            String level = firstNonBlank(map, "level", "proficiencyLevel", "proficiency");
            if (level == null || level.isBlank()) {
                level = "Básico";
            }

            Integer score = 0;
            Object rawScore = map.get("score");
            if (rawScore instanceof Number num) {
                score = num.intValue();
            } else if (rawScore instanceof String strScore && !strScore.isBlank()) {
                try {
                    score = Integer.parseInt(strScore.trim());
                } catch (NumberFormatException ignored) {
                    score = 0;
                }
            }

            return new StudentLanguageRequestDTO(
                name != null ? name.trim() : "",
                level.trim(),
                score
            );
        }
        return new StudentLanguageRequestDTO("", "Básico", 0);
    }

    private static String firstNonBlank(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            String value = Objects.toString(map.get(key), null);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
