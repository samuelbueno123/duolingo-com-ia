package com.duolinfo.ia.proj.dto.response;

import java.util.Map;

public record AdminSystemHealthDTO(
    String status,
    Map<String, Object> database,
    Map<String, Object> externalAi,
    Map<String, Object> systemResources,
    Map<String, Object> application
) {
}
