package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.mapper.DtoMapper;
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

        Student student = new Student();

        student.setName(request.name());
        student.setEmail(request.email());
        student.setGoogleId(request.googleId());

        if (request.languages() != null) {
            for (String languageName : request.languages()) {

                if (languageName == null || languageName.isBlank()) {
                    continue;
                }

                StudentLanguage language = new StudentLanguage();

                language.setLanguageName(languageName.trim());

                student.addLanguage(language);
            }
        }

        return DtoMapper.toStudentResponse(
            studentService.create(student)
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

    @GetMapping("/{id}")
    @Operation(summary = "Busca estudante por ID")
    public StudentResponseDTO findById(
            @PathVariable Long id) {

        Student student = studentService.findById(id);

        if (student == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Estudante não encontrado."
            );
        }

        return DtoMapper.toStudentResponse(student);
    }

    @GetMapping("/google/{googleId}")
    @Operation(summary = "Busca estudante por Google ID")
    public StudentResponseDTO findByGoogleId(
            @PathVariable String googleId) {

        Student student =
            studentService.findByGoogleId(googleId);

        if (student == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Estudante não encontrado."
            );
        }

        return DtoMapper.toStudentResponse(student);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Busca estudante por email")
    public StudentResponseDTO findByEmail(
            @PathVariable String email) {

        Student student =
            studentService.findByEmail(email);

        if (student == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Estudante não encontrado."
            );
        }

        return DtoMapper.toStudentResponse(student);
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
            @PathVariable Long studentId) {

        verifyStudent(studentId);

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

        Student existing = studentService.findById(id);

        if (existing == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Estudante não encontrado."
            );
        }

        existing.setName(request.name());
        existing.setEmail(request.email());
        existing.setGoogleId(request.googleId());

        studentService.update(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui estudante")
    public void deleteById(
            @PathVariable Long id) {

        verifyStudent(id);

        studentService.deleteById(id);
    }

    private void verifyStudent(Long id) {

        if (studentService.findById(id) == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Estudante não encontrado."
            );
        }
    }
}