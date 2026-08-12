// Não preciso explicar

package com.duolingo.ia.proj.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolingo.ia.proj.entity.User;
import com.duolingo.ia.proj.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    // ==========================================
    // LISTAR USUÁRIOS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {

        List<User> users = userRepository.findAll();

        return ResponseEntity.ok(users);
    }
}