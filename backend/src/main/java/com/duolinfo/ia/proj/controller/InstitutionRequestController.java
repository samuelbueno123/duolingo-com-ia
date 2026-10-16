package com.duolinfo.ia.proj.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.request.InstitutionRequestCreateDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionRequestResponseDTO;
import com.duolinfo.ia.proj.service.InstitutionRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/institutions/requests")
@RequiredArgsConstructor
public class InstitutionRequestController {

    private final InstitutionRequestService institutionRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionRequestResponseDTO create(
            @Valid @RequestBody InstitutionRequestCreateDTO request) {
        return institutionRequestService.create(request);
    }
}
