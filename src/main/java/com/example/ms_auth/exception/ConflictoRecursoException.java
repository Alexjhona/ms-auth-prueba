package com.example.ms_auth.exception;

public class ConflictoRecursoException extends RuntimeException {

    public ConflictoRecursoException() {
        super("El registro ya existe o genera conflicto");
    }

    public ConflictoRecursoException(String message) {
        super(message);
    }
}
