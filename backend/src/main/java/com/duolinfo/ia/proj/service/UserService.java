package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }



    // ==========================================
    // Métodos de CRUD (Create, Read, Update, Delete), no caso é só CUD kkkk
    // ==========================================

    public User create(User user) {
        return userRepository.save(user);
    }
    
    public User update(User user) {
        return userRepository.save(user);
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
