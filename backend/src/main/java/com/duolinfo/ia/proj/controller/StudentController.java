package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.service.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/User/students")
@Tag(name = "Students", description = "API para gerenciamento de estudantes")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo estudante", description = "Cria um novo estudante com os dados fornecidos.")
    public Student create(@RequestBody Student student) {
        return studentService.create(student);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Lista todos os estudantes", description = "Retorna todos os estudantes cadastrados.")
    public List<Student> listAll() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca um estudante por ID", description = "Retorna os dados de um estudante com base no ID informado.")
    public Student findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @GetMapping("/google/{googleId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca um estudante por Google ID", description = "Retorna o estudante associado ao Google ID informado.")
    public Student findByGoogleId(@PathVariable String googleId) {
        return studentService.findByGoogleId(googleId);
    }

    @GetMapping("/email/{email}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca um estudante por email", description = "Retorna o estudante associado ao email informado.")
    public Student findByEmail(@PathVariable String email) {
        return studentService.findByEmail(email);
    }

    @GetMapping("/name/{name}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca estudantes por nome", description = "Retorna estudantes cujo nome contenha o valor informado.")
    public List<Student> findByName(@PathVariable String name) {
        return studentService.findByName(name);
    }

    @GetMapping("/language/{language}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca estudantes por idioma", description = "Retorna estudantes que tenham o idioma informado em suas preferências.")
    public List<Student> findByPreferredLanguage(@PathVariable String language) {
        return studentService.findByPreferredLanguage(language);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Atualiza um estudante por ID", description = "Atualiza os dados de um estudante existente com base no ID informado.")
    public void update(@PathVariable Long id, @RequestBody Student student) {
        Student existingStudent = studentService.findById(id);
        if (existingStudent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado com o ID: " + id);
        }

        student.setId(id);
        studentService.update(student);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deleta um estudante por ID", description = "Deleta um estudante específico com base no ID informado.")
    public void deleteById(@PathVariable Long id) {
        Student existingStudent = studentService.findById(id);
        if (existingStudent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado com o ID: " + id);
        }

        studentService.deleteById(id);
    }
}
