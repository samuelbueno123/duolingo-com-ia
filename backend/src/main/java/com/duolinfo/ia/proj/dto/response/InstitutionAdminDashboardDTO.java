package com.duolinfo.ia.proj.dto.response;

import java.util.List;

public record InstitutionAdminDashboardDTO(
    InstitutionResponseDTO institution,
    int totalTeachers,
    int pendingTeachers,
    int totalStudents,
    int totalClasses,
    int totalExercises,
    List<TeacherResponseDTO> teachers,
    List<StudentResponseDTO> students,
    List<SchoolClassResponseDTO> classes,
    List<LearningExerciseResponseDTO> exercises,
    List<AdminNotificationResponseDTO> notifications
) {
}
