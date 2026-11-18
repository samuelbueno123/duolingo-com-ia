package com.duolinfo.ia.proj.util;

import java.util.Locale;
import java.util.Map;

/**
 * Resolve automaticamente códigos de bandeira a partir do nome do idioma ou código manual,
 * além de sanitizar URLs opcionais fornecidas pelo administrador.
 */
public final class CatalogFlagResolver {

    private static final Map<String, String> KEYWORD_TO_FLAG = Map.ofEntries(
        Map.entry("alem", "de"),
        Map.entry("german", "de"),
        Map.entry("deutsch", "de"),
        Map.entry("de-de", "de"),
        Map.entry("espan", "es"),
        Map.entry("spanish", "es"),
        Map.entry("mexic", "mx"),
        Map.entry("argent", "ar"),
        Map.entry("franc", "fr"),
        Map.entry("french", "fr"),
        Map.entry("ingle", "us"),
        Map.entry("english", "us"),
        Map.entry("britan", "gb"),
        Map.entry("reino unido", "gb"),
        Map.entry("ital", "it"),
        Map.entry("japon", "jp"),
        Map.entry("japan", "jp"),
        Map.entry("nihon", "jp"),
        Map.entry("portugu", "br"),
        Map.entry("brasil", "br"),
        Map.entry("brazil", "br"),
        Map.entry("portugal", "pt"),
        Map.entry("corean", "kr"),
        Map.entry("korean", "kr"),
        Map.entry("hangul", "kr"),
        Map.entry("chines", "cn"),
        Map.entry("mandari", "cn"),
        Map.entry("chinese", "cn"),
        Map.entry("russ", "ru"),
        Map.entry("arab", "sa"),
        Map.entry("hindi", "in"),
        Map.entry("indian", "in"),
        Map.entry("turc", "tr"),
        Map.entry("turk", "tr"),
        Map.entry("holand", "nl"),
        Map.entry("neerland", "nl"),
        Map.entry("dutch", "nl"),
        Map.entry("polon", "pl"),
        Map.entry("polish", "pl"),
        Map.entry("suec", "se"),
        Map.entry("swed", "se"),
        Map.entry("norueg", "no"),
        Map.entry("norweg", "no"),
        Map.entry("greg", "gr"),
        Map.entry("greek", "gr"),
        Map.entry("hebra", "il"),
        Map.entry("hebr", "il"),
        Map.entry("ucran", "ua"),
        Map.entry("ukrain", "ua"),
        Map.entry("tailand", "th"),
        Map.entry("thai", "th"),
        Map.entry("vietnam", "vn"),
        Map.entry("esperant", "eo")
    );

    private CatalogFlagResolver() {
    }

    public static String resolveAutoFlagCode(String languageName) {
        String norm = SupportedLanguages.stripAccentsLower(languageName);
        if (norm.isEmpty()) {
            return "globe";
        }
        if (norm.equals("de")) return "de";
        if (norm.equals("es")) return "es";
        if (norm.equals("fr")) return "fr";
        if (norm.equals("en") || norm.equals("us")) return "us";
        if (norm.equals("gb") || norm.equals("uk")) return "gb";
        if (norm.equals("it")) return "it";
        if (norm.equals("ja") || norm.equals("jp")) return "jp";
        if (norm.equals("pt") || norm.equals("br") || norm.equals("pt-br")) return "br";
        if (norm.equals("pt-pt")) return "pt";
        if (norm.equals("ko") || norm.equals("kr")) return "kr";
        if (norm.equals("zh") || norm.equals("cn")) return "cn";
        if (norm.equals("ru")) return "ru";
        if (norm.equals("ar")) return "ar";
        if (norm.equals("mx")) return "mx";
        if (norm.equals("eo")) return "eo";

        if (norm.contains("portugal") || norm.contains("portugues europeu")) {
            return "pt";
        }
        if (norm.contains("britan")) {
            return "gb";
        }

        for (Map.Entry<String, String> entry : KEYWORD_TO_FLAG.entrySet()) {
            if (norm.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "globe";
    }

    public static String normalizeFlagMode(String rawMode) {
        return "MANUAL".equalsIgnoreCase(rawMode != null ? rawMode.trim() : "")
            ? "MANUAL"
            : "AUTO";
    }

    public static String normalizeManualFlagCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        String trimmed = rawCode.trim();
        if (trimmed.length() <= 10 && trimmed.matches("^[a-zA-Z0-9_-]+$")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed.substring(0, Math.min(trimmed.length(), 32));
    }

    public static String sanitizeFlagUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String trimmed = rawUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return trimmed.substring(0, Math.min(trimmed.length(), 500));
        }
        return null;
    }
}
