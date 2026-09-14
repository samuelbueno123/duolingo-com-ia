package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/users", "/User"})
@Tag(name = "Users", description = "API para gerenciamento de usuários")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo usuário", description = "Cria um novo usuário com os dados fornecidos.")
    public User create(@RequestBody User user) {
        return userService.create(user);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Lista todos os usuários", description = "Retorna todos os usuários cadastrados.")
    public List<User> listAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca um usuário por ID", description = "Retorna os dados de um usuário com base no ID informado.")
    public User findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/google/{googleId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca um usuário por Google ID", description = "Retorna o usuário associado ao Google ID informado.")
    public User findByGoogleId(@PathVariable String googleId) {
        return userService.findByGoogleId(googleId);
    }

    @GetMapping("/email/{email}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca um usuário por email", description = "Retorna o usuário associado ao email informado.")
    public User findByEmail(@PathVariable String email) {
        return userService.findByEmail(email);
    }

    @GetMapping("/name/{name}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Busca usuários por nome", description = "Retorna usuários cujo nome contenha o valor informado.")
    public List<User> findByName(@PathVariable String name) {
        return userService.findByName(name);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Atualiza um usuário por ID", description = "Atualiza os dados de um usuário existente com base no ID informado.")
    public void update(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com o ID: " + id);
        }

        user.setId(id);
        userService.update(user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deleta um usuário por ID", description = "Deleta um usuário específico com base no ID informado.")
    public void deleteById(@PathVariable Long id) {
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com o ID: " + id);
        }

        userService.deleteById(id);
    }
}