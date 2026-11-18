package com.utch.productos.security;

import com.utch.productos.TestSecrets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService: creación y validación de tokens")
class JwtServiceTest {

    private static final long UN_MINUTO = 60_000L;

    private final JwtService servicio = new JwtService(TestSecrets.JWT_SECRET, UN_MINUTO, 2 * UN_MINUTO);

    @Test
    @DisplayName("Un access token válido contiene el usuario, el tipo y una fecha de expiración futura")
    void accessToken_valido_contieneUsuarioYTipo() {
        String token = servicio.generarAccessToken("ana");

        Claims claims = servicio.validarAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo("ana");
        assertThat(claims.get("tipo", String.class)).isEqualTo(JwtService.TIPO_ACCESS);
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("Un refresh token válido se acepta como refresh token")
    void refreshToken_valido_seAceptaComoRefresh() {
        String token = servicio.generarRefreshToken("ana");

        Claims claims = servicio.validarRefreshToken(token);

        assertThat(claims.getSubject()).isEqualTo("ana");
        assertThat(claims.get("tipo", String.class)).isEqualTo(JwtService.TIPO_REFRESH);
    }

    @Test
    @DisplayName("Un refresh token no sirve como access token")
    void refreshToken_noSirveComoAccessToken() {
        String refresh = servicio.generarRefreshToken("ana");

        assertThatThrownBy(() -> servicio.validarAccessToken(refresh))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("access");
    }

    @Test
    @DisplayName("Un access token no sirve como refresh token")
    void accessToken_noSirveComoRefreshToken() {
        String access = servicio.generarAccessToken("ana");

        assertThatThrownBy(() -> servicio.validarRefreshToken(access))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("refresh");
    }

    @Test
    @DisplayName("Un token expirado lanza ExpiredJwtException")
    void tokenExpirado_lanzaExcepcion() {
        JwtService conExpiracionPasada = new JwtService(TestSecrets.JWT_SECRET, -1000L, -1000L);
        String expirado = conExpiracionPasada.generarAccessToken("ana");

        assertThatThrownBy(() -> servicio.validarAccessToken(expirado))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Un token firmado con otra llave se rechaza")
    void tokenFirmadoConOtraLlave_seRechaza() {
        JwtService otroServidor = new JwtService(TestSecrets.OTRA_LLAVE, UN_MINUTO, UN_MINUTO);
        String ajeno = otroServidor.generarAccessToken("ana");

        assertThatThrownBy(() -> servicio.validarAccessToken(ajeno))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un texto que no es un JWT se rechaza")
    void tokenMalFormado_seRechaza() {
        assertThatThrownBy(() -> servicio.validarAccessToken("esto.no.es.un.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("La vigencia del access token se expresa en segundos")
    void expiracionEnSegundos() {
        assertThat(servicio.getAccessExpiracionSegundos()).isEqualTo(60L);
    }

    @Test
    @DisplayName("Una llave menor a 256 bits impide crear el servicio")
    void llaveDebil_lanzaExcepcion() {
        String llaveCorta = Base64.getEncoder().encodeToString(new byte[8]);

        assertThatThrownBy(() -> new JwtService(llaveCorta, UN_MINUTO, UN_MINUTO))
                .isInstanceOf(WeakKeyException.class);
    }
}
