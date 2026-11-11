package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record ClassAnnouncementResponseDTO(
    Long id,
    Long schoolClassId,
    String schoolClassName,
    Long teacherId,
    String teacherName,
    String title,
    String content,
    String priority,
    boolean pinned,
    LocalDateTime createdAt
) {}
