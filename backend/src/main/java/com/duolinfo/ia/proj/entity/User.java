package com.duolinfo.ia.proj.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
@Inheritance(strategy = jakarta.persistence.InheritanceType.JOINED)
public class User {

    // ==========================================
    // ID INTERNO DO NOSSO SISTEMA
    // ==========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ==========================================
    // ID DO USUÁRIO NO GOOGLE
    // ==========================================

    @Column(name = "google_id", unique = true)
    private String googleId;

    // ==========================================
    // NOME DO USUÁRIO
    // ==========================================

    @Column(nullable = false)
    private String name; 
    /* 
        Por favor, tratem o nome do usuário do jeito que vcs quiserem no front, 
        apenas faça um método para juntar nome e sobrenome e que vcs consiguam saber separar isso depois.
    */

    // ==========================================
    // EMAIL
    // ==========================================

    @Column(nullable = false, unique = true)
    private String email;


    // ==========================================
    // FOTO DE PERFIL
    // ==========================================

    @Column(name = "profile_picture", length = 1000)
    private String profilePicture;


    // ==========================================
    // Password-Hash do usuário
    // ==========================================

    @Column(name = "password_hash") // Não é nullable porque o usuário pode logar com o Google, então não precisa de senha
    private String passwordHash;
	}