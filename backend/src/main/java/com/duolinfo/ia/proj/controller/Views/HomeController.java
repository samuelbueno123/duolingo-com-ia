package com.duolinfo.ia.proj.controller.Views;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/home")
@Tag(
        name = "Home",
        description = "Dados iniciais necessários pela tela principal"
)
public class HomeController {

    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "TEACHER", "STUDENT", "USER");

    @GetMapping
    @Operation(summary = "Carrega os dados básicos da Home")
    public ResponseEntity<Map<String, Object>> home(
            Authentication authentication) {

        List<String> normalizedRoles = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replaceFirst("^ROLE_", ""))
                .toList();

        String profileType = ROLE_PRIORITY.stream()
                .filter(normalizedRoles::contains)
                .findFirst()
                .orElseGet(() -> normalizedRoles.stream().findFirst().orElse("USER"));

        Object principal = authentication.getPrincipal();
        Object userPayload = (principal instanceof User domainUser)
                ? DtoMapper.toUserResponse(domainUser)
                : principal;

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("user", userPayload);
        response.put("profileType", profileType);
        response.put(
                "profileCompleted",
                !"USER".equals(profileType)
        );

        return ResponseEntity.ok(response);
    }
}