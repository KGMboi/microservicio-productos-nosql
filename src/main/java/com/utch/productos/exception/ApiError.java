package com.utch.productos.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

// Formato único de las respuestas de error
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String codigo,
        String mensaje,
        String ruta,
        List<CampoError> errores) {

    public record CampoError(String campo, String mensaje) {
    }
}
