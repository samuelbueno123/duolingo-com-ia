package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record TeacherStudentNoteResponseDTO(
    Long id,
    Long teacherId,
    String teacherName,
    Long studentId,
    String studentName,
    Long schoolClassId,
    String schoolClassName,
    String note,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
