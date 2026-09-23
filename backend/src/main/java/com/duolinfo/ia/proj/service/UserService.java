package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }



    // ==========================================
    // Métodos de CRUD (Create, Read, Update, Delete), no caso é só CUD kkkk
    // ==========================================

    public User create(User user) {

        if (userRepository
                .findByEmail(user.getEmail())
                .isPresent()) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "E-mail já cadastrado no sistema."
            );
        }

        /*
        * Usuários Google podem não possuir senha.
        */
        if (user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank()) {

            user.setPasswordHash(
                passwordEncoder.encode(
                    user.getPasswordHash()
                )
            );
        }

        return userRepository.save(user);
    }
    
    public User update(User user) {

        User existingUser =
            userRepository.findById(user.getId())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                    )
                );

        existingUser.setName(
            user.getName()
        );

        existingUser.setEmail(
            user.getEmail()
        );

        existingUser.setGoogleId(
            user.getGoogleId()
        );

        existingUser.setProfilePicture(
            user.getProfilePicture()
        );

        /*
        * Só altera a senha quando uma nova senha
        * realmente foi informada.
        */
        if (user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank()) {

            existingUser.setPasswordHash(
                passwordEncoder.encode(
                    user.getPasswordHash()
                )
            );
        }

        return userRepository.save(existingUser);
    }
    
    public void delete(User user) {
        userRepository.delete(user);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public void deleteByGoogleId(String googleId) {
        userRepository.findByGoogleId(googleId).ifPresent(userRepository::delete);
    }

    public void deleteByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
    }


    // ==========================================
    // Métodos de busca
    // ==========================================

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAllById(Iterable<Long> ids) {
        return userRepository.findAllById(ids);
    }
    
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // Adicionei esse método para buscar usuários pelo nome, e pode retornar uma lista de usuários que contenham o mesmo nome buscado, ignorando maiúsculas e minúsculas
    public List<User> findByName(String name) {
        return userRepository.findAll().stream()
                .filter(user -> user.getName() != null && user.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }


    // ==========================================
    // Métodos de verificação de existência
    // ==========================================

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public boolean existsByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId).isPresent();
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public boolean existsByName(String name) {
        return userRepository.findAll().stream()
                .anyMatch(user -> user.getName() != null && user.getName().equalsIgnoreCase(name));
    }
}
