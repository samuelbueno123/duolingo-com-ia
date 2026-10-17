package com.duolinfo.ia.proj.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.dto.response.ClassDashboardResponseDTO;
import com.duolinfo.ia.proj.dto.response.ClassDashboardResponseDTO.ClassExerciseSummaryDTO;
import com.duolinfo.ia.proj.dto.response.ClassDashboardResponseDTO.ClassStudentSummaryDTO;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final InstitutionRepository institutionRepository;
    private final ClassTeacherAssignmentRepository assignmentRepository;
    private final LearningExerciseRepository exerciseRepository;
    private final ExerciseSubmissionRepository submissionRepository;

    @Transactional
    public SchoolClass create(
        Long schoolId,
        SchoolClass schoolClass
    ) {

        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Escola não encontrada: " + schoolId
            ));

        if (Boolean.FALSE.equals(schoolClass.getActive())) {
            // permitido criar inativa caso você queira usar
            // cadastro antes da ativação
        }

        validateUniqueClass(
            schoolId,
            schoolClass.getSchoolYear(),
            schoolClass.getName(),
            schoolClass.getShift()
        );

        schoolClass.setSchool(school);
        schoolClass.setInstitution(school.getInstitution());

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public SchoolClass createForTeacherInstitution(
        String teacherEmail,
        SchoolClass schoolClass
    ) {
        Teacher teacher = teacherRepository.findByEmailIgnoreCase(teacherEmail)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Professor não encontrado para o email: " + teacherEmail
            ));

        Institution institution = teacher.getInstitution();
        if (institution == null) {
            institution = institutionRepository.findByCode(InstitutionService.DEFAULT_INSTITUTION_CODE)
                .orElseThrow(() -> new BusinessException("O professor não possui instituição vinculada."));
            teacher.setInstitution(institution);
            teacherRepository.save(teacher);
        }

        final Institution targetInstitution = institution;
        School school = schoolRepository.findByInstitutionId(targetInstitution.getId())
            .stream()
            .findFirst()
            .orElseGet(() -> schoolRepository.save(School.builder()
                .name(targetInstitution.getName())
                .inepCode("INEP-" + targetInstitution.getId())
                .institution(targetInstitution)
                .build()));

        if (schoolClass.getSchoolYear() == null || schoolClass.getSchoolYear().isBlank()) {
            schoolClass.setSchoolYear(String.valueOf(LocalDate.now().getYear()));
        }
        if (schoolClass.getActive() == null) {
            schoolClass.setActive(true);
        }

        SchoolClass created = create(school.getId(), schoolClass);

        boolean alreadyAssigned = assignmentRepository.findByTeacherId(teacher.getId())
            .stream()
            .anyMatch(a -> a.getSchoolClass() != null
                && a.getSchoolClass().getId().equals(created.getId())
                && a.getEndDate() == null);
        if (!alreadyAssigned) {
            assignmentRepository.save(ClassTeacherAssignment.builder()
                .teacher(teacher)
                .schoolClass(created)
                .startDate(LocalDate.now())
                .reason("FEMININO".equalsIgnoreCase(teacher.getNormalizedGender())
                    ? "Professora Responsável"
                    : "Professor Responsável")
                .build());
        }

        return created;
    }

    @Transactional(readOnly = true)
    public ClassDashboardResponseDTO getClassDashboard(Long classId) {
        SchoolClass schoolClass = findById(classId);
        List<Enrollment> enrollments = enrollmentRepository.findBySchoolClassId(classId);
        List<LearningExercise> exercises = exerciseRepository.findBySchoolClass_IdOrderByCreatedAtDesc(classId);
        List<ExerciseSubmission> submissions = submissionRepository.findByExercise_SchoolClass_IdOrderBySubmittedAtDesc(classId);

        int activeCount = (int) enrollments.stream()
            .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
            .count();
        int totalExercises = exercises.size();
        int totalSubmissions = submissions.size();
        int totalCorrect = submissions.stream().mapToInt(ExerciseSubmission::getCorrectCount).sum();
        int totalQuestions = submissions.stream().mapToInt(ExerciseSubmission::getQuestionCount).sum();
        int totalXp = submissions.stream().mapToInt(ExerciseSubmission::getXpEarned).sum();

        double avgCorrect = totalSubmissions == 0
            ? 0.0
            : Math.round(((double) totalCorrect / totalSubmissions) * 100.0) / 100.0;
        double avgQuestions = totalSubmissions == 0
            ? 0.0
            : Math.round(((double) totalQuestions / totalSubmissions) * 100.0) / 100.0;
        double avgAccuracy = totalQuestions == 0
            ? 0.0
            : Math.round(((totalCorrect * 100.0) / totalQuestions) * 10.0) / 10.0;

        Map<Long, List<ExerciseSubmission>> byStudent = submissions.stream()
            .filter(s -> s.getStudent() != null && s.getStudent().getId() != null)
            .collect(Collectors.groupingBy(s -> s.getStudent().getId()));

        List<ClassStudentSummaryDTO> studentSummaries = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            if (student == null) continue;
            List<ExerciseSubmission> studentSubs = byStudent.getOrDefault(student.getId(), List.of());
            int sCount = studentSubs.size();
            int sCorrect = studentSubs.stream().mapToInt(ExerciseSubmission::getCorrectCount).sum();
            int sQuestions = studentSubs.stream().mapToInt(ExerciseSubmission::getQuestionCount).sum();
            int sXp = studentSubs.stream().mapToInt(ExerciseSubmission::getXpEarned).sum();
            double sAcc = sQuestions == 0
                ? 0.0
                : Math.round(((sCorrect * 100.0) / sQuestions) * 10.0) / 10.0;

            studentSummaries.add(new ClassStudentSummaryDTO(
                student.getId(),
                enrollment.getId(),
                student.getName(),
                student.getEmail(),
                student.getNormalizedGender(),
                enrollment.getStatus() != null ? enrollment.getStatus().name() : "ACTIVE",
                enrollment.getEnrollmentDate(),
                sCount,
                sAcc,
                sXp
            ));
        }

        Map<Long, List<ExerciseSubmission>> byExercise = submissions.stream()
            .filter(s -> s.getExercise() != null && s.getExercise().getId() != null)
            .collect(Collectors.groupingBy(s -> s.getExercise().getId()));

        List<ClassExerciseSummaryDTO> exerciseSummaries = new ArrayList<>();
        for (LearningExercise exercise : exercises) {
            List<ExerciseSubmission> exSubs = byExercise.getOrDefault(exercise.getId(), List.of());
            int exCorrect = exSubs.stream().mapToInt(ExerciseSubmission::getCorrectCount).sum();
            int exQuestions = exSubs.stream().mapToInt(ExerciseSubmission::getQuestionCount).sum();
            double exAcc = exQuestions == 0
                ? 0.0
                : Math.round(((exCorrect * 100.0) / exQuestions) * 10.0) / 10.0;
            exerciseSummaries.add(new ClassExerciseSummaryDTO(
                exercise.getId(),
                exercise.getTitle(),
                exercise.getLanguageName(),
                exercise.getProficiencyLevel(),
                exercise.getDueDate(),
                exSubs.size(),
                exAcc
            ));
        }

        Institution inst = schoolClass.getInstitution() != null
            ? schoolClass.getInstitution()
            : (schoolClass.getSchool() != null ? schoolClass.getSchool().getInstitution() : null);

        return new ClassDashboardResponseDTO(
            schoolClass.getId(),
            schoolClass.getName(),
            schoolClass.getSchoolYear(),
            schoolClass.getShift(),
            schoolClass.getGradeLevel(),
            schoolClass.getSchool() != null ? schoolClass.getSchool().getId() : null,
            schoolClass.getSchool() != null ? schoolClass.getSchool().getName() : null,
            inst != null ? inst.getId() : null,
            inst != null ? inst.getName() : null,
            Boolean.TRUE.equals(schoolClass.getActive()),
            activeCount,
            totalExercises,
            totalSubmissions,
            avgAccuracy,
            avgCorrect,
            avgQuestions,
            totalXp,
            studentSummaries,
            exerciseSummaries
        );
    }

    @Transactional(readOnly = true)
    public SchoolClass findById(Long id) {

        return schoolClassRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Turma não encontrada: " + id
            ));
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findAll() {
        return schoolClassRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findBySchool(Long schoolId) {

        validateSchoolExists(schoolId);

        return schoolClassRepository.findBySchoolId(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findActiveBySchool(Long schoolId) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndActive(schoolId, true);
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findBySchoolAndYear(
        Long schoolId,
        String schoolYear
    ) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndSchoolYearOrderByNameAsc(
                schoolId,
                schoolYear
            );
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> findActiveBySchoolAndYear(
        Long schoolId,
        String schoolYear
    ) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndSchoolYearAndActive(
                schoolId,
                schoolYear,
                true
            );
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> search(
        Long schoolId,
        String name
    ) {

        validateSchoolExists(schoolId);

        return schoolClassRepository
            .findBySchoolIdAndNameContainingIgnoreCase(
                schoolId,
                name
            );
    }

    @Transactional
    public SchoolClass update(
        Long id,
        Long schoolId,
        SchoolClass data
    ) {

        SchoolClass current = findById(id);

        Long newSchoolId =
            schoolId != null
                ? schoolId
                : current.getSchool().getId();

        boolean changedUniqueFields =
            !equals(current.getSchool().getId(), newSchoolId)
                || !equals(current.getSchoolYear(), data.getSchoolYear())
                || !equals(current.getName(), data.getName())
                || !equals(current.getShift(), data.getShift());

        if (changedUniqueFields) {
            validateUniqueClass(
                newSchoolId,
                data.getSchoolYear(),
                data.getName(),
                data.getShift(),
                id
            );
        }

        if (!current.getSchool().getId().equals(newSchoolId)) {

            School school = schoolRepository.findById(newSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Escola não encontrada: " + newSchoolId
                ));

            current.setSchool(school);
            current.setInstitution(school.getInstitution());
        } else if (current.getInstitution() == null && current.getSchool() != null) {
            current.setInstitution(current.getSchool().getInstitution());
        }

        current.setName(data.getName());
        current.setSchoolYear(data.getSchoolYear());
        current.setShift(data.getShift());
        current.setGradeLevel(data.getGradeLevel());

        if (data.getActive() != null) {
            current.setActive(data.getActive());
        }

        return schoolClassRepository.save(current);
    }

    @Transactional
    public SchoolClass activate(Long id) {

        SchoolClass schoolClass = findById(id);
        schoolClass.setActive(true);

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public SchoolClass deactivate(Long id) {

        SchoolClass schoolClass = findById(id);
        schoolClass.setActive(false);

        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public void delete(Long id) {

        SchoolClass schoolClass = findById(id);

        /*
         * Preservamos turmas que já possuem histórico acadêmico.
         */
        if (!enrollmentRepository.findBySchoolClassId(id).isEmpty()) {
            throw new BusinessException(
                "Não é possível excluir a turma porque ela possui matrículas."
            );
        }

        schoolClassRepository.delete(schoolClass);
    }

    private void validateSchoolExists(Long schoolId) {

        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException(
                "Escola não encontrada: " + schoolId
            );
        }
    }

    private void validateUniqueClass(
        Long schoolId,
        String schoolYear,
        String name,
        String shift
    ) {

        validateUniqueClass(
            schoolId,
            schoolYear,
            name,
            shift,
            null
        );
    }

    private void validateUniqueClass(
        Long schoolId,
        String schoolYear,
        String name,
        String shift,
        Long currentId
    ) {

        boolean exists =
            schoolClassRepository
                .existsBySchoolIdAndSchoolYearAndNameAndShift(
                    schoolId,
                    schoolYear,
                    name,
                    shift
                );

        if (!exists) {
            return;
        }

        if (currentId == null) {
            throw new BusinessException(
                "Já existe uma turma com essa escola, ano, nome e turno."
            );
        }

        SchoolClass existing =
            schoolClassRepository
                .findBySchoolIdAndNameContainingIgnoreCase(
                    schoolId,
                    name
                )
                .stream()
                .filter(item ->
                    item.getSchoolYear() != null
                    && item.getSchoolYear().equals(schoolYear)
                    && equals(item.getShift(), shift)
                )
                .findFirst()
                .orElse(null);

        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(
                "Já existe outra turma com essa escola, ano, nome e turno."
            );
        }
    }

    private boolean equals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}