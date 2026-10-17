package com.duolinfo.ia.proj.mapper;

import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.duolinfo.ia.proj.dto.request.InstitutionRequestDTO;
import com.duolinfo.ia.proj.dto.request.SchoolClassRequestDTO;
import com.duolinfo.ia.proj.dto.request.SchoolRequestDTO;
import com.duolinfo.ia.proj.dto.request.StudentLanguageRequestDTO;
import com.duolinfo.ia.proj.dto.request.StudentRequestDTO;
import com.duolinfo.ia.proj.dto.request.TeacherDocumentRequestDTO;
import com.duolinfo.ia.proj.dto.request.TeacherRequestDTO;
import com.duolinfo.ia.proj.dto.request.UserRequestDTO;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.TeacherDocument;
import com.duolinfo.ia.proj.entity.User;

/** Converts validated request DTOs into domain entities. */
public final class RequestMapper {

    private static final int MAX_TEACHER_DOCUMENT_BYTES = 10 * 1024 * 1024;

    private RequestMapper() {
    }

    public static String normalizeGender(String gender) {
        if (gender != null && "FEMININO".equalsIgnoreCase(gender.trim())) {
            return "FEMININO";
        }
        return "MASCULINO";
    }

    public static User toNewUser(UserRequestDTO request) {
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setProfilePicture(request.profilePicture());
        user.setPasswordHash(request.password());
        user.setGender(normalizeGender(request.gender()));
        return user;
    }

    public static Student toNewStudent(StudentRequestDTO request) {
        Student student = toStudent(request);
        student.setPasswordHash(request.password());
        return student;
    }

    public static Student toStudentUpdate(Long id, StudentRequestDTO request) {
        Student student = toStudent(request);
        student.setId(id);
        if (request.languages() == null) {
            student.setLanguages(null);
        }
        return student;
    }

    private static Student toStudent(StudentRequestDTO request) {
        Student student = new Student();
        student.setName(request.name());
        student.setEmail(request.email());
        if (request.gender() != null && !request.gender().isBlank()) {
            student.setGender(normalizeGender(request.gender()));
        }

        if (request.languages() != null) {
            request.languages().stream()
                .filter(RequestMapper::hasLanguageName)
                .map(RequestMapper::toStudentLanguage)
                .forEach(student::addLanguage);
        }

        return student;
    }

    private static boolean hasLanguageName(StudentLanguageRequestDTO request) {
        return request != null
            && request.languageName() != null
            && !request.languageName().isBlank();
    }

    private static StudentLanguage toStudentLanguage(
            StudentLanguageRequestDTO request) {
        StudentLanguage language = new StudentLanguage();
        language.setLanguageName(request.languageName().trim());
        language.setLevel(cleanOrDefault(request.level(), "Básico"));
        if (request.score() != null) {
            language.setScore(request.score());
        }
        return language;
    }

    public static Teacher toNewTeacher(TeacherRequestDTO request) {
        Teacher teacher = toTeacher(request);
        teacher.setPasswordHash(request.password());

        if (request.documents() != null) {
            request.documents().stream()
                .map(RequestMapper::toTeacherDocument)
                .forEach(teacher::addDocument);
        }

        return teacher;
    }

    public static Teacher toTeacherUpdate(Long id, TeacherRequestDTO request) {
        Teacher teacher = toTeacher(request);
        teacher.setId(id);
        return teacher;
    }

    private static Teacher toTeacher(TeacherRequestDTO request) {
        Teacher teacher = new Teacher();
        teacher.setName(request.name());
        teacher.setEmail(request.email());
        if (request.gender() != null && !request.gender().isBlank()) {
            teacher.setGender(normalizeGender(request.gender()));
        }
        teacher.setTaughtLanguages(request.taughtLanguages() == null
            ? new ArrayList<>()
            : new ArrayList<>(request.taughtLanguages()));
        teacher.setSpecializationAreas(request.specializationAreas() == null
            ? new ArrayList<>()
            : new ArrayList<>(request.specializationAreas()));
        teacher.setBibliography(request.bibliography());
        return teacher;
    }

    private static TeacherDocument toTeacherDocument(
            TeacherDocumentRequestDTO request) {
        if (request == null
                || request.fileName() == null
                || request.fileName().isBlank()
                || request.fileData() == null
                || request.fileData().isBlank()) {
            throw new IllegalArgumentException(
                "Cada documento precisa de nome e conteúdo Base64."
            );
        }

        byte[] fileData = decodeDocument(request.fileData());
        if (fileData.length > MAX_TEACHER_DOCUMENT_BYTES) {
            throw new IllegalArgumentException(
                "Cada documento pode ter no máximo 10 MB."
            );
        }

        String contentType = cleanOrDefault(request.contentType(), "application/pdf");
        if (!isSupportedDocumentType(contentType)) {
            throw new IllegalArgumentException(
                "Os documentos aceitos são PDF, DOC e DOCX."
            );
        }

        TeacherDocument document = new TeacherDocument();
        document.setFileName(request.fileName().trim());
        document.setContentType(contentType);
        document.setFileSize((long) fileData.length);
        document.setFileData(fileData);
        return document;
    }

    private static byte[] decodeDocument(String value) {
        int dataSeparator = value.indexOf(',');
        String encodedData = dataSeparator >= 0
            ? value.substring(dataSeparator + 1)
            : value;

        try {
            return Base64.getDecoder().decode(encodedData);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "O conteúdo do documento não é um Base64 válido.",
                exception
            );
        }
    }

    private static boolean isSupportedDocumentType(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "application/octet-stream" -> true;
            default -> false;
        };
    }

    private static String cleanOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public static Institution toInstitution(InstitutionRequestDTO request) {
        Institution institution = new Institution();
        institution.setName(request.name());
        institution.setCode(request.code());
        institution.setEmail(request.email());
        institution.setPhone(request.phone());
        institution.setWebsite(request.website());
        institution.setCity(request.city());
        institution.setState(request.state());
        institution.setCountry(request.country());
        institution.setAddress(request.address());
        institution.setDescription(request.description());
        if (request.active() != null) {
            institution.setActive(request.active());
        }
        return institution;
    }

    public static School toSchool(SchoolRequestDTO request) {
        School school = new School();
        school.setName(request.name());
        school.setInepCode(request.inepCode());
        return school;
    }

    public static SchoolClass toSchoolClass(SchoolClassRequestDTO request) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(request.name());
        schoolClass.setSchoolYear(request.schoolYear());
        schoolClass.setShift(request.shift());
        schoolClass.setGradeLevel(request.gradeLevel());
        if (request.active() != null) {
            schoolClass.setActive(request.active());
        }
        return schoolClass;
    }
}
