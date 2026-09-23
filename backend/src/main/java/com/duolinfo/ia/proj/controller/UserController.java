package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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

import com.duolinfo.ia.proj.dto.request.UserRequestDTO;
import com.duolinfo.ia.proj.dto.response.AuthResponseDTO;
import com.duolinfo.ia.proj.dto.response.UserResponseDTO;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(
    name = "Users",
    description = "Gerenciamento de usuários"
)
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    // ==========================================
    // CREATE
    // ==========================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cria um usuário",
        description = """
            Cria um novo usuário e retorna um JWT.
            A senha será criptografada antes de ser armazenada.
            """
    )
    public AuthResponseDTO create(
            @Valid @RequestBody UserRequestDTO request) {

        User user = new User();

        user.setName(request.name());
        user.setEmail(request.email());
        user.setGoogleId(request.googleId());
        user.setProfilePicture(request.profilePicture());

        /*
        * A senha ainda está em texto puro neste ponto.
        * O UserService fará a criptografia.
        */
        user.setPasswordHash(request.password());

        User newUser = userService.create(user);

        String token = jwtService.generateToken(
            newUser,
            "USER"
        );

        return new AuthResponseDTO(
            true,
            "Usuário criado com sucesso.",
            token,
            DtoMapper.toUserResponse(newUser),
            "USER"
        );
    }

    // ==========================================
    // READ
    // ==========================================

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Lista todos os usuários",
        description = "Retorna todos os usuários cadastrados."
    )
    public List<UserResponseDTO> listAll() {

        return userService.findAll()
            .stream()
            .map(DtoMapper::toUserResponse)
            .toList();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuário por ID"
    )
    public UserResponseDTO findById(
            @PathVariable Long id) {

        User user = userService.findById(id);

        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado com o ID: " + id
            );
        }

        return DtoMapper.toUserResponse(user);
    }

    @GetMapping("/google/{googleId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuário por Google ID"
    )
    public UserResponseDTO findByGoogleId(
            @PathVariable String googleId) {

        User user =
            userService.findByGoogleId(googleId);

        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado com o Google ID: " + googleId
            );
        }

        return DtoMapper.toUserResponse(user);
    }

    @GetMapping("/email/{email}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuário por email"
    )
    public UserResponseDTO findByEmail(
            @PathVariable String email) {

        User user =
            userService.findByEmail(email);

        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado com o email: " + email
            );
        }

        return DtoMapper.toUserResponse(user);
    }

    @GetMapping("/name/{name}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Busca usuários por nome"
    )
    public List<UserResponseDTO> findByName(
            @PathVariable String name) {

        return userService.findByName(name)
            .stream()
            .map(DtoMapper::toUserResponse)
            .toList();
    }

    // ==========================================
    // UPDATE
    // ==========================================

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Atualiza usuário",
        description = """
            Atualiza os dados básicos do usuário.

            Caso uma nova senha seja informada,
            ela será novamente criptografada.
            """
    )
    public void update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {

        User existingUser = userService.findById(id);

        if (existingUser == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado com o ID: " + id
            );
        }

        existingUser.setName(request.name());
        existingUser.setEmail(request.email());
        existingUser.setGoogleId(request.googleId());
        existingUser.setProfilePicture(request.profilePicture());

        /*
         * Só altera a senha se uma nova senha for enviada.
         */
        if (request.password() != null
                && !request.password().isBlank()) {

            existingUser.setPasswordHash(
                request.password()
            );
        }

        userService.update(existingUser);
    }

    // ==========================================
    // DELETE
    // ==========================================

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Exclui usuário"
    )
    public void deleteById(
            @PathVariable Long id) {

        User existingUser = userService.findById(id);

        if (existingUser == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado com o ID: " + id
            );
        }

        userService.deleteById(id);
    }
}