package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.service.TeacherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/teachers")
@Tag(name = "Teachers", description = "API para gerenciamento de professores")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo professor", description = "Cria um professor com os dados do usuário e os campos específicos do perfil.")
    public Teacher create(@RequestBody Teacher teacher) {
        return teacherService.create(teacher);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Lista todos os professores", description = "Retorna todos os professores cadastrados.")
    public List<Teacher> listAll() {
        return teacherService.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca um professor por ID", description = "Retorna os dados de um professor com base no ID informado.")
    public Teacher findById(@PathVariable Long id) {
        Teacher teacher = teacherService.findById(id);
        if (teacher == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found with id: " + id);
        }
        return teacher;
    }

    @GetMapping("/google/{googleId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca professor por Google ID", description = "Retorna o professor associado ao Google ID informado.")
    public Teacher findByGoogleId(@PathVariable String googleId) {
        Teacher teacher = teacherService.findByGoogleId(googleId);
        if (teacher == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found with googleId: " + googleId);
        }
        return teacher;
    }

    @GetMapping("/email/{email}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca professor por email", description = "Retorna o professor associado ao email informado.")
    public Teacher findByEmail(@PathVariable String email) {
        Teacher teacher = teacherService.findByEmail(email);
        if (teacher == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found with email: " + email);
        }
        return teacher;
    }

    @GetMapping("/name/{name}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca professores por nome", description = "Retorna professores cujo nome contenha o valor informado.")
    public List<Teacher> findByName(@PathVariable String name) {
        return teacherService.findByName(name);
    }

    @GetMapping("/institution/{institution}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca professores por instituição", description = "Retorna professores que pertençam à instituição informada.")
    public List<Teacher> findByInstitution(@PathVariable String institution) {
        return teacherService.findByInstitution(institution);
    }

    @GetMapping("/language/{language}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca professores por idioma lecionado", description = "Retorna professores que lecionam o idioma informado.")
    public List<Teacher> findByTaughtLanguage(@PathVariable String language) {
        return teacherService.findByTaughtLanguage(language);
    }

    @GetMapping("/specialization/{specializationArea}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca professores por área de especialização", description = "Retorna professores que possuem a área de especialização informada.")
    public List<Teacher> findBySpecializationArea(@PathVariable String specializationArea) {
        return teacherService.findBySpecializationArea(specializationArea);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Atualiza um professor por ID", description = "Atualiza os dados de um professor existente com base no ID informado.")
    public void update(@PathVariable Long id, @RequestBody Teacher teacher) {
        Teacher existingTeacher = teacherService.findById(id);
        if (existingTeacher == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found with id: " + id);
        }

        teacher.setId(id);
        teacherService.update(teacher);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deleta um professor por ID", description = "Deleta um professor específico com base no ID informado.")
    public void deleteById(@PathVariable Long id) {
        Teacher existingTeacher = teacherService.findById(id);
        if (existingTeacher == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found with id: " + id);
        }

        teacherService.deleteById(id);
    }
}
