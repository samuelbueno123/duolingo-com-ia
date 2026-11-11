package com.duolinfo.ia.proj.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.response.ClassAnalyticsResponseDTO;
import com.duolinfo.ia.proj.service.ClassAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Class Analytics", description = "Métricas pedagógicas, taxas de conclusão, precisão e alertas de estudantes em risco")
public class ClassAnalyticsController {

    private final ClassAnalyticsService analyticsService;

    @GetMapping("/{classId}/analytics")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Obtém analytics consolidado da turma com taxa de acerto, engajamento e alertas de risco")
    public ClassAnalyticsResponseDTO getClassAnalytics(@PathVariable Long classId) {
        return analyticsService.getClassAnalytics(classId);
    }
}
