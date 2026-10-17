package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ClassDashboardResponseDTO(
    Long id,
    String name,
    String schoolYear,
    String shift,
    String gradeLevel,
    Long schoolId,
    String schoolName,
    Long institutionId,
    String institutionName,
    boolean active,
    int totalStudents,
    int totalExercises,
    int totalSubmissions,
    double averageAccuracyPercent,
    double averageCorrectAnswers,
    double averageQuestionsCount,
    int totalXpEarned,
    List<ClassStudentSummaryDTO> students,
    List<ClassExerciseSummaryDTO> exercises
) {
    public record ClassStudentSummaryDTO(
        Long studentId,
        Long enrollmentId,
        String name,
        String email,
        String gender,
        String status,
        LocalDate enrollmentDate,
        int submissionsCount,
        double averageAccuracyPercent,
        int xpEarned
    ) {
    }

    public record ClassExerciseSummaryDTO(
        Long exerciseId,
        String title,
        String languageName,
        String proficiencyLevel,
        LocalDate dueDate,
        int submissionsCount,
        double averageAccuracyPercent
    ) {
    }
}
