package com.duolinfo.ia.proj.dto.request;

import java.util.Map;

public record AdminRowUpdateRequestDTO(
    Map<String, Object> keys,
    Map<String, Object> values
) {
}
