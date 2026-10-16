package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.StudentRequestDTO;
import com.duolinfo.ia.proj.dto.response.EnrollmentResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.mapper.RequestMapper;
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
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A senha é obrigatória para criar uma conta de estudante.");
        }
        return DtoMapper.toStudentResponse(
            studentService.create(RequestMapper.toNewStudent(request))
        );
    }

    @GetMapping
    @Operation(summary = "Lista estudantes")
    public List<StudentResponseDTO> listAll() {

        return studentService.findAll()
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

    @GetMapping("/{id}")
    @Operation(summary = "Busca estudante por ID")
    public StudentResponseDTO findById(
            @PathVariable Long id) {

        return DtoMapper.toStudentResponse(studentService.findById(id));
    }

    @GetMapping("/google/{googleId}")
    @Operation(summary = "Busca estudante por Google ID")
    public StudentResponseDTO findByGoogleId(
            @PathVariable String googleId) {

        return DtoMapper.toStudentResponse(
            requireStudent(studentService.findByGoogleId(googleId))
        );
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Busca estudante por email")
    public StudentResponseDTO findByEmail(
            @PathVariable String email) {

        return DtoMapper.toStudentResponse(
            requireStudent(studentService.findByEmail(email))
        );
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Busca estudantes por nome")
    public List<StudentResponseDTO> findByName(
            @PathVariable String name) {

        return studentService.findByName(name)
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Busca estudantes por idioma")
    public List<StudentResponseDTO> findByPreferredLanguage(
            @PathVariable String language) {

        return studentService.findByPreferredLanguage(language)
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @GetMapping("/{studentId}/enrollments")
    @Operation(summary = "Lista matrículas do estudante")
    public List<EnrollmentResponseDTO> findEnrollments(
            @PathVariable Long studentId) {

        verifyStudent(studentId);

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

        verifyStudentAccess(studentId, authentication);

        return studentService.findActiveEnrollments(studentId)
            .stream()
            .map(DtoMapper::toEnrollmentResponse)
            .toList();
    }

    @GetMapping("/class/{schoolClassId}")
    @Operation(summary = "Lista estudantes da turma")
    public List<StudentResponseDTO> findByClass(
            @PathVariable Long schoolClassId) {

        return studentService.findByClass(schoolClassId)
            .stream()
            .map(DtoMapper::toStudentResponse)
            .toList();
    }

    @GetMapping("/class/{schoolClassId}/active")
    @Operation(summary = "Lista estudantes ativos da turma")
    public List<StudentResponseDTO> findActiveByClass(
            @PathVariable Long schoolClassId) {

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

        studentService.update(RequestMapper.toStudentUpdate(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui estudante")
    public void deleteById(
            @PathVariable Long id) {

        studentService.deleteById(id);
    }

    private void verifyStudent(Long id) {
        studentService.findById(id);
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
