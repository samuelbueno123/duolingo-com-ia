package com.duolingo.ia.proj.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolingo.ia.proj.entities.User;
import com.duolingo.ia.proj.repository.UserRepository;

/** Serviço especializado em manter o usuário local sincronizado com o Google. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createOrUpdateFromGoogle(GoogleUserData googleUser) {
        // O identificador imutável do Google é a chave principal da sincronização.
        User user = userRepository.findByGoogleId(googleUser.googleId())
                .orElseGet(() -> userRepository.findByEmail(googleUser.email())
                        .orElseGet(User::new));

        user.setGoogleId(googleUser.googleId());
        user.setEmail(googleUser.email());
        user.setName(googleUser.name() == null || googleUser.name().isBlank()
                ? googleUser.email()
                : googleUser.name());
        user.setProfilePicture(googleUser.picture());

        return userRepository.save(user);
    }
}
