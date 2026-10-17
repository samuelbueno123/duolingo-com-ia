package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.repository.CatalogOptionRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Catalog", description = "Catálogos usados pelos formulários")
public class CatalogController {

    private final CatalogOptionRepository catalogOptionRepository;

    public CatalogController(CatalogOptionRepository catalogOptionRepository) {
        this.catalogOptionRepository = catalogOptionRepository;
    }

    @GetMapping("/languages")
    @Operation(summary = "Lista idiomas ativos para cadastro")
    public List<String> findLanguages() {
        return catalogOptionRepository
            .findByCatalogTypeAndActiveTrueOrderByValueAsc("language")
            .stream()
            .map(option -> option.getValue())
            .toList();
    }

    @GetMapping("/proficiency-levels")
    @Operation(summary = "Lista níveis de proficiência para cadastro")
    public List<String> findProficiencyLevels() {
        return List.of("Básico", "Intermediário", "Avançado");
    }
}
