// Não preciso dar detalhes sobre essa parte, claro kkk

package com.duolingo.ia.proj.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
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
    // NOME
    // ==========================================

    @Column(nullable = false)
    private String name;


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
    // IDADE
    // ==========================================

    @Column(name = "age")
    private Integer age;

    // ==========================================
    // INSTITUIÇÃO
    // ==========================================

    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;

    // ==========================================
    // CONSTRUTOR VAZIO
    // NECESSÁRIO PELO JPA
    // ==========================================

    public User() {
    }


    // ==========================================
    // CONSTRUTOR COMPLETO
    // ==========================================

    public User(
            String googleId,
            String name,
            String email,
            String profilePicture) {

        this.googleId = googleId;
        this.name = name;
        this.email = email;
        this.profilePicture = profilePicture;
    }


    // ==========================================
    // GETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }


    // ==========================================
    // SETTERS
    // ==========================================

    public void setId(Long id) {
        this.id = id;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
