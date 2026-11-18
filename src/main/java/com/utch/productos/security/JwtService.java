package com.utch.productos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

// Crea y valida los JSON Web Tokens. Se firman con HMAC-SHA256 usando una llave secreta del servidor.
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";
    private static final String CLAIM_TIPO = "tipo";

    private final SecretKey llave;
    private final long accessExpiracionMs;
    private final long refreshExpiracionMs;

    public JwtService(@Value("${jwt.secret}") String secretoBase64,
                      @Value("${jwt.access-expiration-ms}") long accessExpiracionMs,
                      @Value("${jwt.refresh-expiration-ms}") long refreshExpiracionMs) {
        // Falla al arrancar si la llave mide menos de 256 bits
        this.llave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.accessExpiracionMs = accessExpiracionMs;
        this.refreshExpiracionMs = refreshExpiracionMs;
    }

    public String generarAccessToken(String usuario) {
        return construir(usuario, TIPO_ACCESS, accessExpiracionMs);
    }

    public String generarRefreshToken(String usuario) {
        return construir(usuario, TIPO_REFRESH, refreshExpiracionMs);
    }

    public Claims validarAccessToken(String token) {
        return validar(token, TIPO_ACCESS);
    }

    public Claims validarRefreshToken(String token) {
        return validar(token, TIPO_REFRESH);
    }

    public long getAccessExpiracionSegundos() {
        return accessExpiracionMs / 1000;
    }

    private String construir(String usuario, String tipo, long duracionMs) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario)
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMs))
                .signWith(llave)
                .compact();
    }

    // Verifica firma y fecha de expiración, y que el token sea del tipo esperado
    private Claims validar(String token, String tipoEsperado) {
        Claims claims = Jwts.parser()
                .verifyWith(llave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!tipoEsperado.equals(claims.get(CLAIM_TIPO, String.class))) {
            throw new JwtException("El token no es de tipo " + tipoEsperado + ".");
        }
        return claims;
    }
}
