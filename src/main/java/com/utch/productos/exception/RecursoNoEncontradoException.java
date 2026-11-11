package com.utch.productos.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String recurso, String id) {
        super("No se encontró el recurso " + recurso + " con id " + id + ".");
    }
}
