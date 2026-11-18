package com.utch.productos.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Responde 401 cuando se intenta entrar a una ruta protegida sin credenciales válidas
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String NO_AUTENTICADO = "NO_AUTENTICADO";

    private final ErrorResponseWriter writer;

    public RestAuthenticationEntryPoint(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object atributo = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR);
        String codigo = atributo != null ? atributo.toString() : NO_AUTENTICADO;

        String mensaje = switch (codigo) {
            case JwtAuthenticationFilter.ERROR_EXPIRADO ->
                    "El token expiró. Renuévalo en /api/v1/auth/refresh o inicia sesión de nuevo.";
            case JwtAuthenticationFilter.ERROR_INVALIDO ->
                    "El token no es válido.";
            default ->
                    "Se requiere autenticación. Envía el encabezado Authorization: Bearer <token>.";
        };

        writer.escribir(request, response, HttpStatus.UNAUTHORIZED, codigo, mensaje);
    }
}
