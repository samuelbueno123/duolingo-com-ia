package com.duolinfo.ia.proj.dto.response;

public record CatalogOptionResponseDTO(
    Long id,
    String catalogType,
    String value,
    boolean active
) {
}
