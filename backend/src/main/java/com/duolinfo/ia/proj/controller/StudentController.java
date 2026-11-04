package com.duolinfo.ia.proj.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.StudentJoinClassRequestDTO;
import com.duolinfo.ia.proj.dto.request.StudentRequestDTO;
import com.duolinfo.ia.proj.dto.response.SchoolClassResponseDTO;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.service.EnrollmentService;
import com.duolinfo.ia.proj.service.SchoolClassService;
import com.duolinfo.ia.proj.dto.response.EnrollmentResponseDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionMembersResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.mapper.RequestMapper;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
@Tag(
    name = "Students",
    description = "Gerenciamento de estudantes"
)
public class StudentController {

    private final StudentService studentService;

    @Autowired(required = false)
    private AccessControlService accessControlService;

    @Autowired(required = false)
    private EnrollmentService enrollmentService;

    @Autowired(required = false)
    private SchoolClassService schoolClassService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cria um novo estudante",
        description = "Cria um novo estudante."
    )
    public StudentResponseDTO create(
            @Valid @RequestBody StudentRequestDTO request) {
        boolean authenticatedGoogleUser = accessControlService != null
                && accessControlService.isAuthenticatedForEmail(request.email());
        if ((request.password() == null || request.password().isBlank()) && !authenticatedGoogleUser) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A senha é obrigatória para criar uma conta de estudante.");
        }
        return DtoMapper.toStudentResponse(
            studentService.create(
                RequestMapper.toNewStudent(request),
                request.institutionId()
            )
        );
    }

    @GetMapping
    @Operation(summary = "Lista estudantes")
    public List<StudentResponseDTO> listAll() {
        return filterStudentsForCaller(studentService.findAll())
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @GetMapping("/me")
    @Operation(summary = "Busca o perfil do estudante autenticado")
    public StudentResponseDTO findMyProfile(Authentication authentication) {
        return DtoMapper.toStudentResponse(
            requireStudent(studentService.findByEmail(authentication.getName()))
        );
    }

    @GetMapping("/me/institution-members")
    @Operation(summary = "Lista professores e alunos da instituição do estudante autenticado")
    public InstitutionMembersResponseDTO findMyInstitutionMembers(
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return studentService.findInstitutionMembers(authentication.getName(), search);
    }

    @PostMapping("/me/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Matricula o estudante autenticado em uma turma")
    public EnrollmentResponseDTO joinClass(
            @Valid @RequestBody StudentJoinClassRequestDTO request,
            Authentication authentication) {
        Student student = requireStudent(studentService.findByEmail(authentication.getName()));
        if (enrollmentService == null || schoolClassService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de turmas indisponível.");
        }
        SchoolClass schoolClass = schoolClassService.findById(request.schoolClassId());
        if (!Boolean.TRUE.equals(schoolClass.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível entrar em uma turma inativa.");
        }
        return DtoMapper.toEnrollmentResponse(
            enrollmentService.enrollStudent(
                student.getId(),
                request.schoolClassId(),
                java.time.LocalDate.now(),
                request.notes() != null && !request.notes().isBlank()
                    ? request.notes()
                    : "Matrícula realizada pelo próprio estudante"
            )
        );
    }

    @DeleteMapping("/me/enrollments/{enrollmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela a matrícula do estudante autenticado")
    public void leaveClass(
            @PathVariable Long enrollmentId,
            Authentication authentication) {
        Student student = requireStudent(studentService.findByEmail(authentication.getName()));
        if (enrollmentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de turmas indisponível.");
        }
        Enrollment enrollment = enrollmentService.findById(enrollmentId);
        if (!Objects.equals(enrollment.getStudent().getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta matrícula não pertence a você.");
        }
        enrollmentService.cancel(enrollmentId, java.time.LocalDate.now(), "Cancelado pelo estudante");
    }

    @GetMapping("/me/available-classes")
    @Operation(summary = "Lista turmas ativas disponíveis para o estudante se matricular")
    public List<SchoolClassResponseDTO> listAvailableClasses(Authentication authentication) {
        Student student = requireStudent(studentService.findByEmail(authentication.getName()));
        if (schoolClassService == null) {
            return List.of();
        }
        Long instId = student.getInstitution() != null ? student.getInstitution().getId() : null;
        return schoolClassService.findAll().stream()
            .filter(sc -> Boolean.TRUE.equals(sc.getActive()))
            .filter(sc -> {
                if (instId == null) return true;
                Long classInstId = sc.getInstitution() != null ? sc.getInstitution().getId() :
                    (sc.getSchool() != null && sc.getSchool().getInstitution() != null ? sc.getSchool().getInstitution().getId() : null);
                return classInstId == null || Objects.equals(classInstId, instId);
            })
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca estudante por ID")
    public StudentResponseDTO findById(
            @PathVariable Long id) {
        Student student = studentService.findById(id);
        if (accessControlService != null) {
            accessControlService.assertCanReadStudent(student);
        }
        return DtoMapper.toStudentResponse(student);
    }

    @GetMapping("/google/{googleId}")
    @Operation(summary = "Busca estudante por Google ID")
    public StudentResponseDTO findByGoogleId(
            @PathVariable String googleId) {
        Student student = requireStudent(studentService.findByGoogleId(googleId));
        if (accessControlService != null) {
            accessControlService.assertCanReadStudent(student);
        }
        return DtoMapper.toStudentResponse(student);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Busca estudante por email")
    public StudentResponseDTO findByEmail(
            @PathVariable String email) {
        Student student = requireStudent(studentService.findByEmail(email));
        if (accessControlService != null) {
            accessControlService.assertCanReadStudent(student);
        }
        return DtoMapper.toStudentResponse(student);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Busca estudantes por nome")
    public List<StudentResponseDTO> findByName(
            @PathVariable String name) {
        return filterStudentsForCaller(studentService.findByName(name))
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Busca estudantes por idioma")
    public List<StudentResponseDTO> findByPreferredLanguage(
            @PathVariable String language) {
        return filterStudentsForCaller(studentService.findByPreferredLanguage(language))
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @GetMapping("/{studentId}/enrollments")
    @Operation(summary = "Lista matrículas do estudante")
    public List<EnrollmentResponseDTO> findEnrollments(
            @PathVariable Long studentId) {
        Student student = studentService.findById(studentId);
        if (accessControlService != null) {
            accessControlService.assertCanReadStudent(student);
        }
        return studentService.findEnrollments(studentId)
            .stream()
            .map(DtoMapper::toEnrollmentResponse)
            .toList();
    }

    @GetMapping("/{studentId}/enrollments/active")
    @Operation(summary = "Lista matrículas ativas")
    public List<EnrollmentResponseDTO> findActiveEnrollments(
            @PathVariable Long studentId,
            Authentication authentication) {
        Student student = studentService.findById(studentId);
        if (accessControlService != null) {
            accessControlService.assertCanReadStudent(student);
        } else {
            verifyStudentAccess(studentId, authentication);
        }
        return studentService.findActiveEnrollments(studentId)
            .stream()
            .map(DtoMapper::toEnrollmentResponse)
            .toList();
    }

    @GetMapping("/class/{schoolClassId}")
    @Operation(summary = "Lista estudantes da turma")
    public List<StudentResponseDTO> findByClass(
            @PathVariable Long schoolClassId) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(schoolClassId);
        }
        return studentService.findByClass(schoolClassId)
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @GetMapping("/class/{schoolClassId}/active")
    @Operation(summary = "Lista estudantes ativos da turma")
    public List<StudentResponseDTO> findActiveByClass(
            @PathVariable Long schoolClassId) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(schoolClassId);
        }
        return studentService.findActiveByClass(schoolClassId)
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Atualiza estudante")
    public void update(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDTO request) {
        Student target = studentService.findById(id);
        if (accessControlService != null) {
            accessControlService.assertCanModifyStudent(target);
        }
        studentService.update(
            RequestMapper.toStudentUpdate(id, request),
            request.institutionId()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui estudante")
    public void deleteById(
            @PathVariable Long id) {
        Student target = studentService.findById(id);
        if (accessControlService != null) {
            accessControlService.assertCanModifyStudent(target);
        }
        studentService.deleteById(id);
    }

    private List<Student> filterStudentsForCaller(List<Student> candidates) {
        if (accessControlService == null) {
            return candidates;
        }
        User current = accessControlService.getCurrentUserOrThrow();
        if (current.isSuperAdmin()) {
            return candidates;
        }
        if (accessControlService.isTeacher(current)) {
            Long teacherInstId = accessControlService.getUserInstitutionId(current);
            if (teacherInstId == null) {
                return List.of();
            }
            return candidates.stream()
                .filter(s -> {
                    Long sInstId = accessControlService.getUserInstitutionId(s);
                    return sInstId == null || Objects.equals(teacherInstId, sInstId);
                })
                .toList();
        }
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Acesso negado: estudantes não têm permissão para listar o diretório completo de alunos."
        );
    }

    private void verifyStudentAccess(Long id, Authentication authentication) {
        Student student = studentService.findById(id);
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin && !student.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Você só pode consultar suas próprias matrículas."
            );
        }
    }

    private static Student requireStudent(Student student) {
        if (student == null) {
            throw new ResourceNotFoundException("Estudante não encontrado.");
        }
        return student;
    }
}
