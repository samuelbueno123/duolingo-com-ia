package com.duolinfo.ia.proj.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.duolinfo.ia.proj.exception.BusinessException;

/**
 * Define e valida os exatamente 6 idiomas oficiais suportados pelo Duolinfo.
 * Português é utilizado apenas como idioma de interface e explicações pedagógicas,
 * nunca como idioma-alvo cadastrado.
 */
public final class SupportedLanguages {

    public static final String DEFAULT = "Inglês";

    public static final List<String> ALL = List.of(
        "Alemão",
        "Espanhol",
        "Francês",
        "Inglês",
        "Italiano",
        "Japonês"
    );

    private static final Map<String, String> NORMALIZED_LOOKUP;

    static {
        Map<String, String> lookup = new LinkedHashMap<>();
        for (String canonical : ALL) {
            lookup.put(stripAccentsLower(canonical), canonical);
        }
        NORMALIZED_LOOKUP = Map.copyOf(lookup);
    }

    private SupportedLanguages() {
    }

    public static String stripAccentsLower(String raw) {
        if (raw == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD);
        return decomposed
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT);
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(NORMALIZED_LOOKUP.get(stripAccentsLower(raw)));
    }

    public static boolean isSupported(String raw) {
        return normalize(raw).isPresent();
    }

    public static String normalizeOrThrow(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(
                "O idioma é obrigatório. Idiomas disponíveis: " + String.join(", ", ALL) + "."
            );
        }
        return normalize(raw).orElseThrow(() -> new BusinessException(
            "Idioma '" + raw.trim() + "' não é suportado pela plataforma. Idiomas disponíveis: "
                + String.join(", ", ALL) + "."
        ));
    }

    public static String normalizeOrDefault(String raw, String defaultCanonical) {
        if (raw == null || raw.isBlank()) {
            return normalize(defaultCanonical).orElse(DEFAULT);
        }
        return normalizeOrThrow(raw);
    }

    public static List<String> normalizeListOrThrow(Collection<String> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }
        Map<String, String> deduplicated = new LinkedHashMap<>();
        for (String item : rawList) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String canonical = normalizeOrThrow(item);
            deduplicated.putIfAbsent(stripAccentsLower(canonical), canonical);
        }
        return new ArrayList<>(deduplicated.values());
    }

    public static boolean matches(String first, String second) {
        Optional<String> left = normalize(first);
        Optional<String> right = normalize(second);
        return left.isPresent() && right.isPresent() && left.get().equals(right.get());
    }

    public static String inferFromClassNameOrDefault(String className) {
        if (className == null || className.isBlank()) {
            return DEFAULT;
        }
        String normalizedName = stripAccentsLower(className);
        if (normalizedName.contains("alem")) return "Alemão";
        if (normalizedName.contains("espanh")) return "Espanhol";
        if (normalizedName.contains("franc")) return "Francês";
        if (normalizedName.contains("ital")) return "Italiano";
        if (normalizedName.contains("japon")) return "Japonês";
        return DEFAULT;
    }
}
