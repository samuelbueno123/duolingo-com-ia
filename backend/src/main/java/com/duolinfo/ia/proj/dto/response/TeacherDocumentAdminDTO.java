package com.duolinfo.ia.proj.dto.response;

public record TeacherDocumentAdminDTO(
    Long id,
    String fileName,
    String contentType,
    Long fileSize,
    Long teacherId,
    String teacherName,
    String teacherEmail,
    String institutionName,
    boolean approved
) {
}
