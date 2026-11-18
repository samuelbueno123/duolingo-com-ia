package com.duolinfo.ia.proj.mapper;

import java.util.ArrayList;
import java.util.Collections;

import com.duolinfo.ia.proj.dto.response.ClassTeacherAssignmentResponseDTO;
import com.duolinfo.ia.proj.dto.response.EnrollmentResponseDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionResponseDTO;
import com.duolinfo.ia.proj.dto.response.SchoolClassResponseDTO;
import com.duolinfo.ia.proj.dto.response.SchoolResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentResponseDTO;
import com.duolinfo.ia.proj.dto.response.TeacherResponseDTO;
import com.duolinfo.ia.proj.dto.response.UserResponseDTO;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.util.SupportedLanguages;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponseDTO toUserResponse(User user) {

        if (user == null) {
            return null;
        }

        boolean institutionAdmin = false;
        boolean primaryInstitutionAdmin = false;
        Long institutionId = null;
        String institutionName = null;

        if (user instanceof Teacher teacher) {
            institutionAdmin = teacher.isInstitutionAdmin();
            primaryInstitutionAdmin = teacher.isPrimaryInstitutionAdmin();
            if (teacher.getInstitution() != null) {
                try {
                    institutionId = teacher.getInstitution().getId();
                    if (org.hibernate.Hibernate.isInitialized(teacher.getInstitution())) {
                        institutionName = teacher.getInstitution().getName();
                    }
                } catch (Exception ignored) {
                    // Safe fallback against lazy initialization outside active session
                }
            }
        } else if (user instanceof Student student) {
            if (student.getInstitution() != null) {
                try {
                    institutionId = student.getInstitution().getId();
                    if (org.hibernate.Hibernate.isInitialized(student.getInstitution())) {
                        institutionName = student.getInstitution().getName();
                    }
                } catch (Exception ignored) {
                    // Safe fallback against lazy initialization outside active session
                }
            }
        }

        return new UserResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getGoogleId(),
            user.getProfilePicture(),
            user.getNormalizedGender(),
            user.isSuperAdmin(),
            institutionAdmin,
            primaryInstitutionAdmin,
            institutionId,
            institutionName
        );
    }

    public static StudentResponseDTO toStudentResponse(
            Student student) {

        if (student == null) {
            return null;
        }

        var languages =
            (student.getLanguages() == null || !org.hibernate.Hibernate.isInitialized(student.getLanguages()))
                ? Collections.<String>emptyList()
                : student.getLanguages()
                    .stream()
                    .map(language -> language.getLanguageName())
                    .toList();

        var languageLevels =
            (student.getLanguages() == null || !org.hibernate.Hibernate.isInitialized(student.getLanguages()))
                ? Collections.<String, String>emptyMap()
                : student.getLanguages().stream().collect(
                    java.util.stream.Collectors.toMap(
                        language -> language.getLanguageName(),
                        language -> language.getLevel(),
                        (first, second) -> second,
                        java.util.LinkedHashMap::new
                    )
                );

        var languageScores =
            (student.getLanguages() == null || !org.hibernate.Hibernate.isInitialized(student.getLanguages()))
                ? Collections.<String, Integer>emptyMap()
                : student.getLanguages().stream().collect(
                    java.util.stream.Collectors.toMap(
                        language -> language.getLanguageName(),
                        language -> language.getScore(),
                        (first, second) -> second,
                        java.util.LinkedHashMap::new
                    )
                );

        Long institutionId = null;
        String institutionName = null;
        String institutionPictureUrl = null;
        if (student.getInstitution() != null) {
            try {
                institutionId = student.getInstitution().getId();
                if (org.hibernate.Hibernate.isInitialized(student.getInstitution())) {
                    institutionName = student.getInstitution().getName();
                    institutionPictureUrl = student.getInstitution().getPictureUrl();
                }
            } catch (Exception ignored) {
                // Safe fallback
            }
        } else if (student.getEnrollments() != null && org.hibernate.Hibernate.isInitialized(student.getEnrollments())) {
            for (Enrollment e : student.getEnrollments()) {
                if (e.getSchoolClass() != null
                        && e.getSchoolClass().getSchool() != null
                        && e.getSchoolClass().getSchool().getInstitution() != null) {
                    try {
                        institutionId = e.getSchoolClass().getSchool().getInstitution().getId();
                        institutionName = e.getSchoolClass().getSchool().getInstitution().getName();
                        institutionPictureUrl = e.getSchoolClass().getSchool().getInstitution().getPictureUrl();
                        break;
                    } catch (Exception ignored) {}
                }
            }
        }

        return new StudentResponseDTO(
            student.getId(),
            student.getName(),
            student.getEmail(),
            student.getGoogleId(),
            languages,
            languageLevels,
            languageScores,
            student.getNormalizedGender(),
            institutionId,
            institutionName,
            institutionPictureUrl
        );
    }

    public static TeacherResponseDTO toTeacherResponse(
            Teacher teacher) {

        if (teacher == null) {
            return null;
        }

        Long institutionId = null;
        String institutionName = null;
        String institutionPictureUrl = null;

        if (teacher.getInstitution() != null) {
            try {
                institutionId = teacher.getInstitution().getId();
                if (org.hibernate.Hibernate.isInitialized(teacher.getInstitution())) {
                    institutionName = teacher.getInstitution().getName();
                    institutionPictureUrl = teacher.getInstitution().getPictureUrl();
                }
            } catch (Exception ignored) {
                // Safe fallback
            }
        }

        var taughtLanguages = (teacher.getTaughtLanguages() == null || !org.hibernate.Hibernate.isInitialized(teacher.getTaughtLanguages()))
            ? Collections.<String>emptyList()
            : new ArrayList<>(teacher.getTaughtLanguages());

        var specializationAreas = (teacher.getSpecializationAreas() == null || !org.hibernate.Hibernate.isInitialized(teacher.getSpecializationAreas()))
            ? Collections.<String>emptyList()
            : new ArrayList<>(teacher.getSpecializationAreas());

        String status = teacher.getApprovalStatus();
        if (status == null || status.isBlank()) {
            status = Boolean.TRUE.equals(teacher.getApproved()) ? "APPROVED" : "PENDING";
        }

        return new TeacherResponseDTO(
            teacher.getId(),
            teacher.getName(),
            teacher.getEmail(),
            teacher.getGoogleId(),
            institutionId,
            taughtLanguages,
            specializationAreas,
            teacher.getBibliography(),
            Boolean.TRUE.equals(teacher.getApproved()),
            teacher.getNormalizedGender(),
            teacher.isInstitutionAdmin(),
            teacher.isPrimaryInstitutionAdmin(),
            status,
            institutionName,
            institutionPictureUrl
        );
    }

    public static InstitutionResponseDTO toInstitutionResponse(
            Institution institution) {

        if (institution == null) {
            return null;
        }

        return new InstitutionResponseDTO(
            institution.getId(),
            institution.getName(),
            institution.getCode(),
            institution.getEmail(),
            institution.getPhone(),
            institution.getWebsite(),
            institution.getCity(),
            institution.getState(),
            institution.getCountry(),
            institution.getAddress(),
            institution.getDescription(),
            institution.getActive(),
            institution.getCreatedAt(),
            institution.getUpdatedAt(),
            institution.getPictureUrl()
        );
    }

    public static SchoolResponseDTO toSchoolResponse(
            School school) {

        if (school == null) {
            return null;
        }

        Long institutionId = null;

        if (school.getInstitution() != null) {
            institutionId = school.getInstitution().getId();
        }

        return new SchoolResponseDTO(
            school.getId(),
            school.getName(),
            school.getInepCode(),
            institutionId
        );
    }

    public static SchoolClassResponseDTO toSchoolClassResponse(
            SchoolClass schoolClass) {

        if (schoolClass == null) {
            return null;
        }

        Long schoolId = null;

        if (schoolClass.getSchool() != null) {
            schoolId = schoolClass.getSchool().getId();
        }

        String languageName = SupportedLanguages.normalize(schoolClass.getLanguageName())
            .orElse(SupportedLanguages.inferFromClassNameOrDefault(schoolClass.getName()));

        return new SchoolClassResponseDTO(
            schoolClass.getId(),
            schoolClass.getName(),
            schoolClass.getSchoolYear(),
            schoolClass.getShift(),
            schoolClass.getGradeLevel(),
            schoolId,
            schoolClass.getActive(),
            languageName,
            schoolClass.getCreatedAt(),
            schoolClass.getUpdatedAt(),
            schoolClass.getPictureUrl()
        );
    }

    public static EnrollmentResponseDTO toEnrollmentResponse(
            Enrollment enrollment) {

        if (enrollment == null) {
            return null;
        }

        Long studentId = null;
        String studentName = null;

        if (enrollment.getStudent() != null) {
            studentId = enrollment.getStudent().getId();
            studentName = enrollment.getStudent().getName();
        }

        Long classId = null;
        String className = null;
        String classLanguage = null;
        String classPictureUrl = null;

        if (enrollment.getSchoolClass() != null) {
            classId = enrollment.getSchoolClass().getId();
            className = enrollment.getSchoolClass().getName();
            classLanguage = SupportedLanguages.normalize(enrollment.getSchoolClass().getLanguageName())
                .orElse(SupportedLanguages.inferFromClassNameOrDefault(className));
            classPictureUrl = enrollment.getSchoolClass().getPictureUrl();
        }

        return new EnrollmentResponseDTO(
            enrollment.getId(),
            studentId,
            studentName,
            classId,
            className,
            classLanguage,
            enrollment.getEnrollmentDate(),
            enrollment.getCancellationDate(),
            enrollment.getStatus(),
            enrollment.getNotes(),
            classPictureUrl
        );
    }

    public static ClassTeacherAssignmentResponseDTO toAssignmentResponse(
            ClassTeacherAssignment assignment) {

        if (assignment == null) {
            return null;
        }

        Long classId = null;
        String className = null;
        String classPictureUrl = null;

        if (assignment.getSchoolClass() != null) {
            classId = assignment.getSchoolClass().getId();
            className = assignment.getSchoolClass().getName();
            classPictureUrl = assignment.getSchoolClass().getPictureUrl();
        }

        Long teacherId = null;
        String teacherName = null;

        if (assignment.getTeacher() != null) {
            teacherId = assignment.getTeacher().getId();
            teacherName = assignment.getTeacher().getName();
        }

        return new ClassTeacherAssignmentResponseDTO(
            assignment.getId(),
            classId,
            className,
            teacherId,
            teacherName,
            assignment.getStartDate(),
            assignment.getEndDate(),
            assignment.getReason(),
            classPictureUrl
        );
    }
}
