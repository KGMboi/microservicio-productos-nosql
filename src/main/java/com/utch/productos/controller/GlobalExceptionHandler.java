package com.utch.productos.controller;

import com.utch.productos.exception.ApiError;
import com.utch.productos.exception.DatoDuplicadoException;
import com.utch.productos.exception.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

// Intercepta las excepciones de toda la API y las convierte en respuestas JSON con el código HTTP adecuado
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> manejarNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DatoDuplicadoException.class)
    public ResponseEntity<ApiError> manejarDuplicado(DatoDuplicadoException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, "DATO_DUPLICADO", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.CampoError(error.getField(), error.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA",
                "La solicitud contiene datos inválidos. Revisa los campos indicados.", request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "SOLICITUD_MAL_FORMADA",
                "El cuerpo de la solicitud no es un JSON válido.", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> manejarMetodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO",
                "El método HTTP no está permitido para esta ruta.", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> manejarTipoMedio(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TIPO_NO_SOPORTADO",
                "El Content-Type debe ser application/json.", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> manejarRutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA",
                "La ruta solicitada no existe.", request, null);
    }

    // Cualquier otro error: el detalle solo se registra en el log, nunca se envía al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarErrorInesperado(Exception ex, HttpServletRequest request) {
        LOG.error("Error inesperado en {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error interno. Intenta de nuevo más tarde.", request, null);
    }

    private ResponseEntity<ApiError> responder(HttpStatus status, String codigo, String mensaje,
                                               HttpServletRequest request, List<ApiError.CampoError> errores) {
        ApiError cuerpo = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                codigo, mensaje, request.getRequestURI(), errores);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
