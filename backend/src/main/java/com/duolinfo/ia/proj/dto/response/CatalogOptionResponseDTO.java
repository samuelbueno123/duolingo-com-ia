package com.duolinfo.ia.proj.dto.response;

public record CatalogOptionResponseDTO(
    Long id,
    String catalogType,
    String value,
    boolean active,
    String flagMode,
    String flagCode,
    String flagUrl
) {
    public CatalogOptionResponseDTO(Long id, String catalogType, String value, boolean active) {
        this(id, catalogType, value, active, "AUTO", null, null);
    }
}
