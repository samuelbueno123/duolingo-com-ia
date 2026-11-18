package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ClassDashboardResponseDTO(
    Long id,
    String name,
    String schoolYear,
    String shift,
    String gradeLevel,
    String languageName,
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
    List<ClassExerciseSummaryDTO> exercises,
    List<ClassPerformancePointDTO> performanceSeries,
    String pictureUrl,
    String institutionPictureUrl
) {
    public ClassDashboardResponseDTO(
        Long id,
        String name,
        String schoolYear,
        String shift,
        String gradeLevel,
        String languageName,
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
        this(
            id,
            name,
            schoolYear,
            shift,
            gradeLevel,
            languageName,
            schoolId,
            schoolName,
            institutionId,
            institutionName,
            active,
            totalStudents,
            totalExercises,
            totalSubmissions,
            averageAccuracyPercent,
            averageCorrectAnswers,
            averageQuestionsCount,
            totalXpEarned,
            students,
            exercises,
            List.of(),
            null,
            null
        );
    }

    public ClassDashboardResponseDTO(
        Long id,
        String name,
        String schoolYear,
        String shift,
        String gradeLevel,
        String languageName,
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
        List<ClassExerciseSummaryDTO> exercises,
        List<ClassPerformancePointDTO> performanceSeries
    ) {
        this(
            id,
            name,
            schoolYear,
            shift,
            gradeLevel,
            languageName,
            schoolId,
            schoolName,
            institutionId,
            institutionName,
            active,
            totalStudents,
            totalExercises,
            totalSubmissions,
            averageAccuracyPercent,
            averageCorrectAnswers,
            averageQuestionsCount,
            totalXpEarned,
            students,
            exercises,
            performanceSeries,
            null,
            null
        );
    }

    public record ClassPerformancePointDTO(
        String label,
        double accuracyPercent,
        double participationPercent,
        double xpProgressPercent,
        double overallScorePercent,
        int submissionsCount,
        int cumulativeXp
    ) {
    }
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
        int xpEarned,
        String profilePicture
    ) {
        public ClassStudentSummaryDTO(
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
            this(
                studentId,
                enrollmentId,
                name,
                email,
                gender,
                status,
                enrollmentDate,
                submissionsCount,
                averageAccuracyPercent,
                xpEarned,
                null
            );
        }
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
