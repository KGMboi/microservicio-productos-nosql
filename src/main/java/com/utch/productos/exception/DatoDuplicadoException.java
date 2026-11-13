package com.utch.productos.exception;

public class DatoDuplicadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatoDuplicadoException(String campo, String valor) {
        super("Ya existe un registro con " + campo + " " + valor + ".");
    }
}
