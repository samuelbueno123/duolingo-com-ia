// Aqui tbm nada de mais

package com.duolingo.ia.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duolingo.ia.proj.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);
}