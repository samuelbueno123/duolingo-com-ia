package com.duolinfo.ia.proj.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String email, String rawPassword) {
        String normalizedEmail = (email != null) ? email.trim().toLowerCase() : "";

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .or(() -> userRepository.findByEmail(normalizedEmail))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciais inválidas. Email ou senha incorretos."
            ));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            if (user.getGoogleId() != null && !user.getGoogleId().isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Esta conta foi criada via Google. Por favor, entre usando o botão do Google."
                );
            }
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciais inválidas. Senha não cadastrada."
            );
        }

        // Compara a senha informada no login com o hash BCrypt gravado no banco
        boolean matches = passwordEncoder.matches(rawPassword, user.getPasswordHash());

        // Suporte e upgrade automático para registros legados em texto puro
        if (!matches && rawPassword.equals(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            matches = true;
        }

        if (!matches) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciais inválidas. Email ou senha incorretos."
            );
        }

        return user;
    }
}
