package com.duolinfo.ia.proj.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.ClassAnalyticsResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentRiskDTO;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassAnalyticsService {

    private final SchoolClassRepository schoolClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningExerciseRepository exerciseRepository;
    private final ExerciseSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public ClassAnalyticsResponseDTO getClassAnalytics(Long classId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));

        List<Enrollment> activeEnrollments = enrollmentRepository.findActiveWithStudentAndLanguagesBySchoolClassId(classId, EnrollmentStatus.ACTIVE);
        if (activeEnrollments == null || activeEnrollments.isEmpty()) {
            activeEnrollments = enrollmentRepository.findBySchoolClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        }

        int totalStudents = activeEnrollments.size();

        List<LearningExercise> classExercises = exerciseRepository.findBySchoolClass_IdOrderByCreatedAtDesc(classId);
        Set<Long> exerciseIds = classExercises.stream().map(LearningExercise::getId).collect(Collectors.toSet());

        List<ExerciseSubmission> allSubmissions = submissionRepository.findByExercise_SchoolClass_IdOrderBySubmittedAtDesc(classId);

        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Set<Long> activeStudentIdsPastWeek = allSubmissions.stream()
            .filter(s -> s.getSubmittedAt() != null && s.getSubmittedAt().isAfter(oneWeekAgo))
            .map(s -> s.getStudent().getId())
            .collect(Collectors.toSet());

        int pendingGrading = (int) allSubmissions.stream()
            .filter(s -> "PENDING_REVIEW".equalsIgnoreCase(s.getReviewStatus()))
            .count();

        double totalScorePercent = 0.0;
        int scoredCount = 0;
        Map<Long, List<ExerciseSubmission>> studentSubmissionsMap = new HashMap<>();

        for (ExerciseSubmission s : allSubmissions) {
            studentSubmissionsMap.computeIfAbsent(s.getStudent().getId(), k -> new ArrayList<>()).add(s);
            if (s.getQuestionCount() > 0) {
                totalScorePercent += ((double) s.getCorrectCount() / s.getQuestionCount()) * 100.0;
                scoredCount++;
            }
        }

        double averageAccuracy = scoredCount > 0 ? (totalScorePercent / scoredCount) : 0.0;

        // CEFR Distribution
        Map<String, Long> cefrDistribution = new HashMap<>();
        for (Enrollment e : activeEnrollments) {
            Student s = e.getStudent();
            if (s.getLanguages() != null && !s.getLanguages().isEmpty()) {
                for (StudentLanguage lang : s.getLanguages()) {
                    String lvl = lang.getLevel() != null ? lang.getLevel().trim().toUpperCase() : "A1";
                    cefrDistribution.put(lvl, cefrDistribution.getOrDefault(lvl, 0L) + 1);
                }
            } else {
                cefrDistribution.put("A1", cefrDistribution.getOrDefault("A1", 0L) + 1);
            }
        }

        // Students Needing Attention (0 submissions or < 50% accuracy)
        List<StudentRiskDTO> studentsAtRisk = new ArrayList<>();
        for (Enrollment e : activeEnrollments) {
            Student s = e.getStudent();
            List<ExerciseSubmission> subs = studentSubmissionsMap.getOrDefault(s.getId(), List.of());
            if (subs.isEmpty()) {
                studentsAtRisk.add(new StudentRiskDTO(
                    s.getId(),
                    s.getName(),
                    s.getEmail(),
                    0,
                    0.0,
                    "Nenhuma atividade enviada nesta turma"
                ));
            } else {
                double stTotal = 0;
                int stScored = 0;
                for (ExerciseSubmission sub : subs) {
                    if (sub.getQuestionCount() > 0) {
                        stTotal += ((double) sub.getCorrectCount() / sub.getQuestionCount()) * 100.0;
                        stScored++;
                    }
                }
                double stAvg = stScored > 0 ? (stTotal / stScored) : 0.0;
                if (stAvg < 50.0) {
                    studentsAtRisk.add(new StudentRiskDTO(
                        s.getId(),
                        s.getName(),
                        s.getEmail(),
                        subs.size(),
                        Math.round(stAvg * 10.0) / 10.0,
                        "Aproveitamento abaixo de 50% nas atividades"
                    ));
                }
            }
        }

        return new ClassAnalyticsResponseDTO(
            classId,
            schoolClass.getName(),
            totalStudents,
            activeStudentIdsPastWeek.size(),
            Math.round(averageAccuracy * 10.0) / 10.0,
            allSubmissions.size(),
            pendingGrading,
            cefrDistribution,
            studentsAtRisk
        );
    }
}
