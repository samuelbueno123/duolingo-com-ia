package com.duolinfo.ia.proj.controller.Views;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/home")
@Tag(
        name = "Home",
        description = "Dados iniciais necessários pela tela principal"
)
public class HomeController {

    @GetMapping
    @Operation(summary = "Carrega os dados básicos da Home")
    public ResponseEntity<Map<String, Object>> home(
            Authentication authentication) {

        String profileType = authentication
                .getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority
                                .getAuthority()
                                .replaceFirst("^ROLE_", "")
                )
                .orElse("USER");

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("user", authentication.getPrincipal());
        response.put("profileType", profileType);
        response.put(
                "profileCompleted",
                !"USER".equals(profileType)
        );

        return ResponseEntity.ok(response);
    }
}