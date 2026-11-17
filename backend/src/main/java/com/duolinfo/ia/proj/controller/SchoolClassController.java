package com.duolinfo.ia.proj.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.SchoolClassRequestDTO;
import com.duolinfo.ia.proj.dto.response.ClassDashboardResponseDTO;
import com.duolinfo.ia.proj.dto.response.SchoolClassResponseDTO;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.mapper.RequestMapper;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.ClassTeacherAssignmentService;
import com.duolinfo.ia.proj.service.EnrollmentService;
import com.duolinfo.ia.proj.service.SchoolClassService;
import com.duolinfo.ia.proj.service.StudentNotificationService;
import com.duolinfo.ia.proj.util.SupportedLanguages;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
@Tag(
    name = "Classes",
    description = "Gerenciamento de turmas"
)
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @Autowired(required = false)
    private AccessControlService accessControlService;

    @Autowired(required = false)
    private StudentRepository studentRepository;

    @Autowired(required = false)
    private TeacherRepository teacherRepository;

    @Autowired(required = false)
    private EnrollmentService enrollmentService;

    @Autowired(required = false)
    private ClassTeacherAssignmentService assignmentService;

    @Autowired(required = false)
    private StudentNotificationService studentNotificationService;

    public SchoolClassController(
            SchoolClassService schoolClassService) {

        this.schoolClassService = schoolClassService;
    }

    @PostMapping("/my-institution")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria turma automaticamente vinculada à instituição do professor autenticado")
    public SchoolClassResponseDTO createForMyInstitution(
            @Valid @RequestBody SchoolClassRequestDTO request,
            Authentication authentication) {

        return DtoMapper.toSchoolClassResponse(
            schoolClassService.createForTeacherInstitution(
                authentication.getName(),
                RequestMapper.toSchoolClass(request)
            )
        );
    }

    @PostMapping("/school/{schoolId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria turma em uma escola")
    public SchoolClassResponseDTO create(
            @PathVariable Long schoolId,
            @Valid @RequestBody SchoolClassRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchool(schoolId);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.create(
                schoolId,
                RequestMapper.toSchoolClass(request)
            )
        );
    }

    @PostMapping("/institution/{institutionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria turma vinculada a uma instituição (criando escola padrão se necessário)")
    public SchoolClassResponseDTO createForInstitution(
            @PathVariable Long institutionId,
            @Valid @RequestBody SchoolClassRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertCanManageInstitution(institutionId);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.createForInstitution(
                institutionId,
                RequestMapper.toSchoolClass(request)
            )
        );
    }

    @GetMapping
    @Operation(summary = "Lista turmas")
    public List<SchoolClassResponseDTO> findAll() {

        return filterClassesForCaller(schoolClassService.findAll())
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/{id}/dashboard")
    @Operation(summary = "Retorna o dashboard completo com estatísticas de uma turma")
    public ClassDashboardResponseDTO getDashboard(@PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanViewSchoolClass(id);
        }
        return schoolClassService.getClassDashboard(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca turma por ID")
    public SchoolClassResponseDTO findById(
            @PathVariable Long id) {
        SchoolClass schoolClass = schoolClassService.findById(id);
        if (accessControlService != null) {
            accessControlService.assertCanViewSchoolClass(schoolClass);
        }
        return DtoMapper.toSchoolClassResponse(schoolClass);
    }

    @GetMapping("/school/{schoolId}")
    @Operation(summary = "Lista turmas da escola")
    public List<SchoolClassResponseDTO> findBySchool(
            @PathVariable Long schoolId) {

        return filterClassesForCaller(schoolClassService.findBySchool(schoolId))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/active")
    @Operation(summary = "Lista turmas ativas da escola")
    public List<SchoolClassResponseDTO> findActiveBySchool(
            @PathVariable Long schoolId) {

        return filterClassesForCaller(schoolClassService.findActiveBySchool(schoolId))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/year/{schoolYear}")
    @Operation(summary = "Lista turmas de determinado ano")
    public List<SchoolClassResponseDTO> findBySchoolAndYear(
            @PathVariable Long schoolId,
            @PathVariable String schoolYear) {

        return filterClassesForCaller(schoolClassService.findBySchoolAndYear(schoolId, schoolYear))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/year/{schoolYear}/active")
    @Operation(summary = "Lista turmas ativas de determinado ano")
    public List<SchoolClassResponseDTO>
        findActiveBySchoolAndYear(
            @PathVariable Long schoolId,
            @PathVariable String schoolYear) {

        return filterClassesForCaller(schoolClassService.findActiveBySchoolAndYear(schoolId, schoolYear))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @GetMapping("/school/{schoolId}/search/{name}")
    @Operation(summary = "Pesquisa turma pelo nome")
    public List<SchoolClassResponseDTO> search(
            @PathVariable Long schoolId,
            @PathVariable String name) {

        return filterClassesForCaller(schoolClassService.search(schoolId, name))
            .stream()
            .map(DtoMapper::toSchoolClassResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza turma")
    public SchoolClassResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolClassRequestDTO request) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.update(
                id,
                request.schoolId(),
                RequestMapper.toSchoolClass(request)
            )
        );
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Ativa turma")
    public SchoolClassResponseDTO activate(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.activate(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativa turma")
    public SchoolClassResponseDTO deactivate(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        return DtoMapper.toSchoolClassResponse(
            schoolClassService.deactivate(id)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui turma")
    public void delete(
            @PathVariable Long id) {
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(id);
        }
        schoolClassService.delete(id);
    }

    @GetMapping("/{id}/invitable-users")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Lista usuários elegíveis da mesma instituição para convidar/adicionar à turma (exclui você mesmo e admins de outras instituições)")
    public List<Map<String, Object>> listInvitableUsers(@PathVariable Long id) {
        SchoolClass schoolClass = schoolClassService.findById(id);
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(schoolClass);
        }
        User caller = accessControlService != null ? accessControlService.getCurrentUserOrThrow() : null;
        Long callerId = caller != null ? caller.getId() : null;
        Long classInstId = accessControlService != null
            ? accessControlService.getSchoolClassInstitutionId(schoolClass)
            : (schoolClass.getInstitution() != null ? schoolClass.getInstitution().getId() : null);

        String classLanguage = SupportedLanguages.normalizeOrDefault(
            schoolClass.getLanguageName(),
            SupportedLanguages.inferFromClassNameOrDefault(schoolClass.getName())
        );

        Set<Long> enrolledStudentIds = enrollmentService != null
            ? enrollmentService.findByClass(id).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE && e.getStudent() != null)
                .map(e -> e.getStudent().getId())
                .collect(Collectors.toSet())
            : Set.of();

        List<Map<String, Object>> candidates = new ArrayList<>();
        if (studentRepository != null) {
            for (Student s : studentRepository.findAllWithLanguages()) {
                if (callerId != null && Objects.equals(s.getId(), callerId)) {
                    continue;
                }
                if (s.isSuperAdmin() || s.isAdmin()) {
                    continue;
                }
                Long sInstId = s.getInstitution() != null ? s.getInstitution().getId() : null;
                if (classInstId != null && sInstId != null && !Objects.equals(classInstId, sInstId)) {
                    continue;
                }
                if (enrolledStudentIds.contains(s.getId())) {
                    continue;
                }
                List<String> langs = s.getLanguages() == null
                    ? List.of()
                    : s.getLanguages().stream()
                        .map(l -> l.getLanguageName())
                        .filter(Objects::nonNull)
                        .toList();
                boolean langMatch = langs.stream().anyMatch(l -> SupportedLanguages.matches(l, classLanguage));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", s.getId());
                row.put("name", s.getName());
                row.put("email", s.getEmail());
                row.put("role", "STUDENT");
                row.put("institutionId", sInstId);
                row.put("languages", langs);
                row.put("languageEligible", langMatch);
                candidates.add(row);
            }
        }

        if (teacherRepository != null && classInstId != null) {
            for (Teacher t : teacherRepository.findWithDetailsByInstitutionId(classInstId)) {
                if (callerId != null && Objects.equals(t.getId(), callerId)) {
                    continue;
                }
                if (t.isSuperAdmin()) {
                    continue;
                }
                Long tInstId = t.getInstitution() != null ? t.getInstitution().getId() : null;
                if (!Objects.equals(classInstId, tInstId)) {
                    continue;
                }
                List<String> langs = t.getTaughtLanguages() == null
                    ? List.of()
                    : new ArrayList<>(t.getTaughtLanguages());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", t.getId());
                row.put("name", t.getName());
                row.put("email", t.getEmail());
                row.put("role", "TEACHER");
                row.put("institutionId", tInstId);
                row.put("languages", langs);
                row.put("languageEligible", true);
                candidates.add(row);
            }
        }
        return candidates;
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Convida ou adiciona um aluno/professor elegível da instituição para a turma")
    public Map<String, Object> inviteUserToClass(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        SchoolClass schoolClass = schoolClassService.findById(id);
        if (accessControlService != null) {
            accessControlService.assertCanManageSchoolClass(schoolClass);
        }
        User caller = accessControlService != null ? accessControlService.getCurrentUserOrThrow() : null;
        Object rawUserId = body.get("userId") != null ? body.get("userId") : body.get("studentId");
        if (!(rawUserId instanceof Number num)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o usuário a ser convidado para a turma.");
        }
        Long targetUserId = num.longValue();
        if (caller != null && Objects.equals(caller.getId(), targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode adicionar a si mesmo por este convite.");
        }

        String role = Objects.toString(body.getOrDefault("role", "STUDENT"), "STUDENT").trim().toUpperCase();
        String notes = Objects.toString(body.getOrDefault("notes", "Convite direto para a turma"), "Convite direto para a turma");

        if ("TEACHER".equals(role)) {
            if (assignmentService == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de professores indisponível.");
            }
            var assigned = assignmentService.assignTeacher(id, targetUserId, java.time.LocalDate.now(), notes);
            return Map.of(
                "status", "INVITED",
                "role", "TEACHER",
                "assignmentId", assigned.getId(),
                "message", "Professor convidado e atribuído à turma com sucesso!"
            );
        }

        if (enrollmentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de matrículas indisponível.");
        }
        var enrolled = enrollmentService.enrollStudent(targetUserId, id, java.time.LocalDate.now(), notes);
        if (studentNotificationService != null && enrolled.getStudent() != null) {
            studentNotificationService.notifyStudent(
                enrolled.getStudent().getId(),
                "Convite para turma: " + schoolClass.getName(),
                "Você foi adicionado(a) à turma " + schoolClass.getName() + ".",
                "CLASS_INVITE",
                "/classes"
            );
        }
        return Map.of(
            "status", "INVITED",
            "role", "STUDENT",
            "enrollmentId", enrolled.getId(),
            "message", "Estudante convidado e matriculado na turma com sucesso!"
        );
    }

    private List<SchoolClass> filterClassesForCaller(List<SchoolClass> classes) {
        if (accessControlService == null) {
            return classes;
        }
        User current = accessControlService.getCurrentUser().orElse(null);
        if (current == null || current.isSuperAdmin()) {
            return classes;
        }
        Long userInstId = accessControlService.getUserInstitutionId(current);
        return classes.stream()
            .filter(sc -> {
                Long classInstId = accessControlService.getSchoolClassInstitutionId(sc);
                return userInstId != null && Objects.equals(userInstId, classInstId);
            })
            .toList();
    }
}
