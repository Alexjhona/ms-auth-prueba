package com.example.ms_auth.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException() {
        super("No se encontró el recurso solicitado");
    }

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
