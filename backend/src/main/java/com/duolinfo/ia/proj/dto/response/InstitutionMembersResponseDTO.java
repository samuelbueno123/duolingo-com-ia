package com.duolinfo.ia.proj.dto.response;

import java.util.List;

public record InstitutionMembersResponseDTO(
    Long institutionId,
    String institutionName,
    List<MemberTeacherDTO> teachers,
    List<MemberStudentDTO> students
) {
    public record MemberTeacherDTO(
        Long id,
        String name,
        String email,
        String profilePicture,
        String gender,
        List<String> taughtLanguages,
        List<String> specializationAreas
    ) {
    }

    public record MemberStudentDTO(
        Long id,
        String name,
        String email,
        String profilePicture,
        String gender,
        List<String> languages
    ) {
    }
}
