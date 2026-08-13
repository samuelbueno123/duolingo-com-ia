package com.duolingo.ia.proj.service;

/** Indica que a credencial enviada pelo navegador não pôde ser autenticada. */
public class InvalidGoogleCredentialException extends RuntimeException {

    public InvalidGoogleCredentialException(String message) {
        super(message);
    }
}
