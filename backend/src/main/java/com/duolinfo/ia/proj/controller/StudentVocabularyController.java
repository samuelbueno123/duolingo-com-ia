package com.duolinfo.ia.proj.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.VocabularyRequestDTO;
import com.duolinfo.ia.proj.dto.response.VocabularyResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.StudentVocabularyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/me/vocabulary")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Vocabulary Vault", description = "Cofre pessoal de vocabulário, termos aprendidos, exemplos e níveis de domínio")
public class StudentVocabularyController {

    private final StudentVocabularyService vocabularyService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Principal principal) {
        return studentRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));
    }

    @GetMapping
    @Operation(summary = "Lista palavras e expressões salvas no cofre pessoal de vocabulário")
    public List<VocabularyResponseDTO> getVocabulary(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String masteryLevel,
            Principal principal) {
        Student student = resolveStudent(principal);
        return vocabularyService.getVocabulary(student.getId(), language, search, masteryLevel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona uma nova palavra ou expressão ao vocabulário pessoal")
    public VocabularyResponseDTO addWord(
            @Valid @RequestBody VocabularyRequestDTO request,
            Principal principal) {
        Student student = resolveStudent(principal);
        return vocabularyService.addWord(student.getId(), request);
    }

    @PatchMapping("/{id}/mastery")
    @Operation(summary = "Atualiza o nível de domínio (LEARNING, REVIEWING, MASTERED) da palavra")
    public VocabularyResponseDTO updateMastery(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String masteryLevel,
            Principal principal) {
        Student student = resolveStudent(principal);
        String level = masteryLevel != null ? masteryLevel : (body != null ? body.get("masteryLevel") : "LEARNING");
        return vocabularyService.updateMastery(student.getId(), id, level);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma palavra do cofre de vocabulário")
    public void deleteWord(@PathVariable Long id, Principal principal) {
        Student student = resolveStudent(principal);
        vocabularyService.deleteWord(student.getId(), id);
    }
}
