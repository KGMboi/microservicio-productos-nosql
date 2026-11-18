package com.utch.productos.dto;

// Respuesta de login y de renovación: el cliente usa accessToken en cada petición
public record TokenResponse(String accessToken, String refreshToken, String tipo, long expiraEnSegundos) {
}
