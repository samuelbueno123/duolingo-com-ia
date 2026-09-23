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

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponseDTO toUserResponse(User user) {

        if (user == null) {
            return null;
        }

        return new UserResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getGoogleId(),
            user.getProfilePicture()
        );
    }

    public static StudentResponseDTO toStudentResponse(
            Student student) {

        if (student == null) {
            return null;
        }

        var languages =
            student.getLanguages() == null
                ? Collections.<String>emptyList()
                : student.getLanguages()
                    .stream()
                    .map(language -> language.getLanguageName())
                    .toList();

        return new StudentResponseDTO(
            student.getId(),
            student.getName(),
            student.getEmail(),
            student.getGoogleId(),
            languages
        );
    }

    public static TeacherResponseDTO toTeacherResponse(
            Teacher teacher) {

        if (teacher == null) {
            return null;
        }

        Long institutionId = null;

        if (teacher.getInstitution() != null) {
            institutionId = teacher.getInstitution().getId();
        }

        return new TeacherResponseDTO(
            teacher.getId(),
            teacher.getName(),
            teacher.getEmail(),
            teacher.getGoogleId(),
            institutionId,
            teacher.getTaughtLanguages(),
            teacher.getSpecializationAreas(),
            teacher.getBibliography()
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
            institution.getUpdatedAt()
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

        return new SchoolClassResponseDTO(
            schoolClass.getId(),
            schoolClass.getName(),
            schoolClass.getSchoolYear(),
            schoolClass.getShift(),
            schoolClass.getGradeLevel(),
            schoolId,
            schoolClass.getActive(),
            schoolClass.getCreatedAt(),
            schoolClass.getUpdatedAt()
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

        if (enrollment.getSchoolClass() != null) {
            classId = enrollment.getSchoolClass().getId();
            className = enrollment.getSchoolClass().getName();
        }

        return new EnrollmentResponseDTO(
            enrollment.getId(),
            studentId,
            studentName,
            classId,
            className,
            enrollment.getEnrollmentDate(),
            enrollment.getCancellationDate(),
            enrollment.getStatus(),
            enrollment.getNotes()
        );
    }

    public static ClassTeacherAssignmentResponseDTO toAssignmentResponse(
            ClassTeacherAssignment assignment) {

        if (assignment == null) {
            return null;
        }

        Long classId = null;
        String className = null;

        if (assignment.getSchoolClass() != null) {
            classId = assignment.getSchoolClass().getId();
            className = assignment.getSchoolClass().getName();
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
            assignment.getReason()
        );
    }
}