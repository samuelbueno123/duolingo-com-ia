package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.StudentMistakeResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentMistake;
import com.duolinfo.ia.proj.repository.StudentMistakeRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentMistakeService {

    private final StudentMistakeRepository mistakeRepository;
    private final StudentRepository studentRepository;
    private final StudentStreakService streakService;

    @Transactional
    public StudentMistakeResponseDTO recordMistake(
            Long studentId,
            Long exerciseId,
            String question,
            String wrongAnswer,
            String correctAnswer,
            String explanation) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        StudentMistake mistake = StudentMistake.builder()
            .student(student)
            .exerciseId(exerciseId)
            .question(question != null ? question.trim() : "")
            .userWrongAnswer(wrongAnswer != null ? wrongAnswer.trim() : "")
            .correctAnswer(correctAnswer != null ? correctAnswer.trim() : "")
            .explanation(explanation)
            .timesReviewed(0)
            .consecutiveCorrect(0)
            .resolved(false)
            .createdAt(LocalDateTime.now())
            .build();

        mistake = mistakeRepository.save(mistake);
        return mapToDTO(mistake);
    }

    @Transactional(readOnly = true)
    public List<StudentMistakeResponseDTO> getUnresolvedMistakes(Long studentId) {
        return mistakeRepository.findByStudent_IdAndResolvedFalseOrderByCreatedAtDesc(studentId)
            .stream()
            .map(this::mapToDTO)
            .toList();
    }

    @Transactional
    public StudentMistakeResponseDTO reviewMistake(Long studentId, Long mistakeId, boolean isCorrect) {
        StudentMistake mistake = mistakeRepository.findByIdAndStudent_Id(mistakeId, studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Erro não encontrado para este estudante."));

        mistake.setTimesReviewed(mistake.getTimesReviewed() + 1);
        mistake.setLastReviewedAt(LocalDateTime.now());

        if (isCorrect) {
            int consecutive = mistake.getConsecutiveCorrect() + 1;
            mistake.setConsecutiveCorrect(consecutive);
            if (consecutive >= 2) {
                mistake.setResolved(true);
                // Award streak XP for mastering a mistake
                streakService.recordActivity(studentId, 10);
            }
        } else {
            mistake.setConsecutiveCorrect(0);
        }

        mistake = mistakeRepository.save(mistake);
        return mapToDTO(mistake);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(Long studentId) {
        long unresolved = mistakeRepository.countByStudent_IdAndResolvedFalse(studentId);
        List<StudentMistake> all = mistakeRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        long resolved = all.stream().filter(StudentMistake::isResolved).count();

        return Map.of(
            "unresolvedCount", unresolved,
            "resolvedCount", resolved,
            "totalMistakes", all.size()
        );
    }

    private StudentMistakeResponseDTO mapToDTO(StudentMistake m) {
        return new StudentMistakeResponseDTO(
            m.getId(),
            m.getExerciseId(),
            m.getQuestion(),
            m.getUserWrongAnswer(),
            m.getCorrectAnswer(),
            m.getExplanation(),
            m.getTimesReviewed(),
            m.getConsecutiveCorrect(),
            m.isResolved(),
            m.getLastReviewedAt(),
            m.getCreatedAt()
        );
    }
}
