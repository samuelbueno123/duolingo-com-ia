package com.duolinfo.ia.proj.controller;

import com.duolinfo.ia.proj.dto.response.RankResponseDTO;
import com.duolinfo.ia.proj.repository.RankSqlRepository;
import com.duolinfo.ia.proj.repository.RankSqlRepository.RankRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@Tag(name = "Rankings", description = "Rankings de pontuação dos alunos")
public class RankController {

    private final RankSqlRepository rankSqlRepository;

    public RankController(RankSqlRepository rankSqlRepository) {
        this.rankSqlRepository = rankSqlRepository;
    }

    @GetMapping("/students")
    @Operation(
            summary = "Ranking geral dos alunos",
            description = "Ordena pela soma das pontuações dos idiomas. "
                    + "start é inclusivo e end é exclusivo; por padrão retorna as posições 0 a 9."
    )
    public List<RankResponseDTO> rankStudents(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int end,
            @RequestParam(defaultValue = "false") boolean ascending) {
        validateRange(start, end);
        return toResponse(rankSqlRepository.findStudentRanking(ascending, start, end));
    }

    @GetMapping("/languages/{language}")
    @Operation(
            summary = "Ranking dos alunos por idioma",
            description = "Ordena pela pontuação do idioma informado. "
                    + "start é inclusivo e end é exclusivo."
    )
    public List<RankResponseDTO> rankByLanguage(
            @PathVariable String language,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int end,
            @RequestParam(defaultValue = "false") boolean ascending) {
        validateRange(start, end);
        return toResponse(rankSqlRepository.findLanguageRanking(language, ascending, start, end));
    }

    @GetMapping("/classes/{classId}")
    @Operation(
            summary = "Ranking dos alunos de uma turma",
            description = "Ordena pela soma das pontuações dos idiomas dos alunos com matrícula ativa. "
                    + "start é inclusivo e end é exclusivo."
    )
    public List<RankResponseDTO> rankByClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int end,
            @RequestParam(defaultValue = "false") boolean ascending) {
        validateRange(start, end);
        return toResponse(rankSqlRepository.findClassRanking(classId, ascending, start, end));
    }

    private static void validateRange(int start, int endExclusive) {
        if (start < 0 || endExclusive < start) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A faixa deve usar start >= 0 e end >= start (end exclusivo)."
            );
        }
    }

    private static List<RankResponseDTO> toResponse(List<RankRow> rows) {
        return rows.stream()
                .map(row -> new RankResponseDTO(
                        row.position(),
                        row.studentId(),
                        row.studentName(),
                        row.score()
                ))
                .toList();
    }
}
