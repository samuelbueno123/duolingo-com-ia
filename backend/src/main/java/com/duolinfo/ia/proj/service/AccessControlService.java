package com.duolinfo.ia.proj.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;

@Service
public class AccessControlService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassTeacherAssignmentRepository assignmentRepository;

    public AccessControlService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            SchoolClassRepository schoolClassRepository,
            EnrollmentRepository enrollmentRepository,
            ClassTeacherAssignmentRepository assignmentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(auth.getName().trim());
    }

    public User getCurrentUserOrThrow() {
        return getCurrentUser().orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuário não autenticado."
        ));
    }

    public boolean isAuthenticatedForEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return getCurrentUser()
                .map(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(email.trim()))
                .orElse(false);
    }

    public boolean isSuperAdmin(User user) {
        return user != null && user.isSuperAdmin();
    }

    public boolean isTeacher(User user) {
        if (user == null) {
            return false;
        }
        return user instanceof Teacher || teacherRepository.existsById(user.getId());
    }

    public Optional<Teacher> asTeacher(User user) {
        if (user == null) {
            return Optional.empty();
        }
        if (user instanceof Teacher teacher) {
            return Optional.of(teacher);
        }
        return teacherRepository.findById(user.getId());
    }

    public Optional<Student> asStudent(User user) {
        if (user == null) {
            return Optional.empty();
        }
        if (user instanceof Student student) {
            return Optional.of(student);
        }
        return studentRepository.findById(user.getId());
    }

    public Long getUserInstitutionId(User user) {
        if (user == null) {
            return null;
        }
        Optional<Teacher> teacherOpt = asTeacher(user);
        if (teacherOpt.isPresent() && teacherOpt.get().getInstitution() != null) {
            return teacherOpt.get().getInstitution().getId();
        }
        Optional<Student> studentOpt = asStudent(user);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            if (student.getInstitution() != null) {
                return student.getInstitution().getId();
            }
            if (student.getEnrollments() != null) {
                for (Enrollment e : student.getEnrollments()) {
                    Long classInstId = getSchoolClassInstitutionId(e.getSchoolClass());
                    if (classInstId != null) {
                        return classInstId;
                    }
                }
            }
        }
        return null;
    }

    public Long getSchoolClassInstitutionId(SchoolClass schoolClass) {
        if (schoolClass == null || schoolClass.getSchool() == null
                || schoolClass.getSchool().getInstitution() == null) {
            return null;
        }
        return schoolClass.getSchool().getInstitution().getId();
    }

    public boolean isInstitutionAdminFor(User user, Long institutionId) {
        if (isSuperAdmin(user)) {
            return true;
        }
        if (institutionId == null) {
            return false;
        }
        return asTeacher(user)
                .map(t -> t.isInstitutionAdmin()
                        && t.getInstitution() != null
                        && Objects.equals(t.getInstitution().getId(), institutionId))
                .orElse(false);
    }

    public void assertSelfOrSuperAdmin(Long targetUserId) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin() || Objects.equals(current.getId(), targetUserId)) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado: você só pode alterar ou acessar seus próprios dados de conta."
        );
    }

    public void assertCanReadUser(Long targetUserId) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin() || Objects.equals(current.getId(), targetUserId)) {
            return;
        }
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target != null) {
            Long currentInstId = getUserInstitutionId(current);
            Long targetInstId = getUserInstitutionId(target);
            if (currentInstId != null && Objects.equals(currentInstId, targetInstId) && isTeacher(current)) {
                return;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado aos dados deste usuário."
        );
    }

    public void assertCanReadStudent(Student targetStudent) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin() || Objects.equals(current.getId(), targetStudent.getId())) {
            return;
        }
        Optional<Teacher> teacherOpt = asTeacher(current);
        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            Long teacherInstId = teacher.getInstitution() != null ? teacher.getInstitution().getId() : null;
            Long studentInstId = getUserInstitutionId(targetStudent);
            if (teacherInstId != null && (studentInstId == null || Objects.equals(teacherInstId, studentInstId))) {
                return;
            }
            boolean inTeacherClass = enrollmentRepository.findByStudentId(targetStudent.getId()).stream()
                    .anyMatch(e -> e.getSchoolClass() != null && canTeacherAccessClass(teacher, e.getSchoolClass()));
            if (inTeacherClass) {
                return;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado aos dados deste estudante."
        );
    }

    public void assertCanModifyStudent(Student targetStudent) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin() || Objects.equals(current.getId(), targetStudent.getId())) {
            return;
        }
        Long studentInstId = getUserInstitutionId(targetStudent);
        if (studentInstId != null && isInstitutionAdminFor(current, studentInstId)) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado: apenas o próprio estudante ou a administração da instituição pode alterar este cadastro."
        );
    }

    public void assertCanModifyTeacher(Teacher targetTeacher) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin() || Objects.equals(current.getId(), targetTeacher.getId())) {
            return;
        }
        Long teacherInstId = targetTeacher.getInstitution() != null ? targetTeacher.getInstitution().getId() : null;
        if (teacherInstId != null && isInstitutionAdminFor(current, teacherInstId)
                && !targetTeacher.isPrimaryInstitutionAdmin()) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado: você não possui permissão para alterar este professor."
        );
    }

    public boolean canTeacherAccessClass(Teacher teacher, SchoolClass schoolClass) {
        if (teacher == null || schoolClass == null) {
            return false;
        }
        Long teacherInstId = teacher.getInstitution() != null ? teacher.getInstitution().getId() : null;
        Long classInstId = getSchoolClassInstitutionId(schoolClass);
        if (teacherInstId != null && classInstId != null && Objects.equals(teacherInstId, classInstId)) {
            return true;
        }
        return assignmentRepository.findByTeacherId(teacher.getId()).stream()
                .filter(a -> a.getEndDate() == null)
                .anyMatch(a -> a.getSchoolClass() != null
                        && Objects.equals(a.getSchoolClass().getId(), schoolClass.getId()));
    }

    public void assertCanManageSchoolClass(Long schoolClassId) {
        SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
        assertCanManageSchoolClass(schoolClass);
    }

    public void assertCanManageSchoolClass(SchoolClass schoolClass) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin()) {
            return;
        }
        Optional<Teacher> teacherOpt = asTeacher(current);
        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            if (canTeacherAccessClass(teacher, schoolClass)) {
                return;
            }
            Long teacherInstId = teacher.getInstitution() != null ? teacher.getInstitution().getId() : null;
            Long classInstId = getSchoolClassInstitutionId(schoolClass);
            if (teacherInstId == null && classInstId == null) {
                return;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado: apenas professores vinculados à instituição desta turma ou administradores podem gerenciá-la."
        );
    }

    public void assertCanViewSchoolClass(SchoolClass schoolClass) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin()) {
            return;
        }
        Optional<Teacher> teacherOpt = asTeacher(current);
        if (teacherOpt.isPresent() && canTeacherAccessClass(teacherOpt.get(), schoolClass)) {
            return;
        }
        Optional<Student> studentOpt = asStudent(current);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            Long studentInstId = getUserInstitutionId(student);
            Long classInstId = getSchoolClassInstitutionId(schoolClass);
            if (studentInstId != null && Objects.equals(studentInstId, classInstId)) {
                return;
            }
            boolean enrolled = enrollmentRepository.findByStudentId(student.getId()).stream()
                    .anyMatch(e -> e.getSchoolClass() != null
                            && Objects.equals(e.getSchoolClass().getId(), schoolClass.getId()));
            if (enrolled) {
                return;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado a esta turma."
        );
    }

    public void assertCanReadEnrollment(Enrollment enrollment) {
        User current = getCurrentUserOrThrow();
        if (current.isSuperAdmin()) {
            return;
        }
        if (enrollment.getStudent() != null && Objects.equals(current.getId(), enrollment.getStudent().getId())) {
            return;
        }
        Optional<Teacher> teacherOpt = asTeacher(current);
        if (teacherOpt.isPresent() && enrollment.getSchoolClass() != null
                && canTeacherAccessClass(teacherOpt.get(), enrollment.getSchoolClass())) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado a esta matrícula."
        );
    }
}
