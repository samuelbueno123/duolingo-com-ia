package com.duolingo.ia.proj.service;

/** Dados confiáveis extraídos de um ID Token já validado pelo Google. */
public record GoogleUserData(
        String googleId,
        String email,
        String name,
        String picture,
        String hostedDomain) {
}
