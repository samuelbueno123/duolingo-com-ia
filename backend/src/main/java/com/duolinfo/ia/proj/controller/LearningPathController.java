package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.learningpath.DiagnosticAnswerDTO;
import com.duolinfo.ia.proj.dto.learningpath.DiagnosticQuestionDTO;
import com.duolinfo.ia.proj.dto.learningpath.DiagnosticResultDTO;
import com.duolinfo.ia.proj.dto.learningpath.LearningGoalUpdateDTO;
import com.duolinfo.ia.proj.dto.learningpath.LearningPathDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.LearningPathService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/learning-path")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Learning Path", description = "Trilhas de estudo personalizadas baseadas no CEFR com teste de nivelamento diagnóstico")
public class LearningPathController {

    private final LearningPathService learningPathService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Authentication auth) {
        return studentRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudante não encontrado."));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna a trilha de estudo personalizada do estudante, metas e radar de competências")
    public LearningPathDTO getMyPath(
            @RequestParam(required = false) String language,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return learningPathService.getStudentLearningPath(student.getId(), language);
    }

    @GetMapping("/diagnostic")
    @Operation(summary = "Retorna o questionário diagnóstico para aferição do nível CEFR")
    public List<DiagnosticQuestionDTO> getDiagnosticQuestions(
            @RequestParam(required = false) String language) {
        return learningPathService.getDiagnosticQuestions(language);
    }

    @GetMapping("/modules/{moduleId}/quiz")
    @Operation(summary = "Retorna as questões práticas obrigatórias para concluir um módulo da trilha")
    public List<DiagnosticQuestionDTO> getModuleQuiz(@PathVariable Long moduleId) {
        return learningPathService.getModuleQuiz(moduleId);
    }

    @PostMapping("/modules/{moduleId}/quiz-submit")
    @Operation(summary = "Avalia o questionário do módulo e só conclui se o aluno atingir aproveitamento mínimo")
    public java.util.Map<String, Object> submitModuleQuiz(
            @PathVariable Long moduleId,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return learningPathService.submitModuleQuizPayload(student.getId(), moduleId, body);
    }

    @PostMapping("/diagnostic")
    @Operation(summary = "Submete as respostas do teste diagnóstico e calibra o nível CEFR e módulos recomendados")
    public DiagnosticResultDTO submitDiagnostic(
            @Valid @RequestBody DiagnosticAnswerDTO dto,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return learningPathService.submitDiagnostic(student.getId(), dto);
    }

    @PutMapping("/goals")
    @Operation(summary = "Atualiza a meta diária de estudos em minutos e nível CEFR alvo")
    public LearningPathDTO updateGoals(
            @Valid @RequestBody LearningGoalUpdateDTO dto,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return learningPathService.updateGoals(student.getId(), dto);
    }

    @PostMapping("/modules/{moduleId}/complete")
    @Operation(summary = "Registra a conclusão de um módulo na trilha e desbloqueia a etapa seguinte")
    public LearningPathDTO completeModule(
            @PathVariable Long moduleId,
            @RequestParam(defaultValue = "100.0") double scorePercent,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return learningPathService.completeModule(student.getId(), moduleId, scorePercent);
    }
}
