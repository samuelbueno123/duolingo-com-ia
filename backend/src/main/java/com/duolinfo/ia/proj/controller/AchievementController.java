package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.request.AchievementRequestDTO;
import com.duolinfo.ia.proj.dto.response.AchievementResponseDTO;
import com.duolinfo.ia.proj.service.AchievementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
@Tag(name = "Achievements", description = "Catálogo e conquistas de alunos")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    @Operation(summary = "Lista o catálogo de conquistas")
    public List<AchievementResponseDTO> listCatalog() {
        return achievementService.findAllCatalog();
    }

    @GetMapping("/me")
    @Operation(summary = "Lista as conquistas do estudante autenticado")
    public List<AchievementResponseDTO> findMyAchievements(Authentication authentication) {
        return achievementService.findForStudentEmail(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria uma nova conquista no catálogo")
    public AchievementResponseDTO create(@Valid @RequestBody AchievementRequestDTO request) {
        return achievementService.createCatalogAchievement(request);
    }
}
