package com.utch.productos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utch.productos.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

// Escribe errores de seguridad con el mismo formato JSON (ApiError) que usa el resto de la API.
// Los errores de seguridad ocurren antes de llegar a los controladores, por eso no los atrapa el @RestControllerAdvice.
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String codigo, String mensaje) throws IOException {
        ApiError cuerpo = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                codigo, mensaje, request.getRequestURI(), null);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
