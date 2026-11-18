package com.utch.productos.service;

import com.utch.productos.TestSecrets;
import com.utch.productos.dto.LoginRequest;
import com.utch.productos.dto.RefreshRequest;
import com.utch.productos.dto.RegistroRequest;
import com.utch.productos.dto.TokenResponse;
import com.utch.productos.dto.UsuarioResponse;
import com.utch.productos.exception.DatoDuplicadoException;
import com.utch.productos.model.Usuario;
import com.utch.productos.repository.UsuarioRepository;
import com.utch.productos.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService: registro, login y renovación de tokens")
class AuthServiceTest {

    private static final String CONTRASENA_DE_PRUEBA = "Clave-De-Prueba-1";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TestSecrets.JWT_SECRET, 60_000L, 120_000L);
        service = new AuthService(usuarioRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    @DisplayName("registrar guarda el hash de la contraseña y asigna el rol USER")
    void registrar_usuarioNuevo_guardaHashYRolUser() {
        when(usuarioRepository.existsByUsuario("ana")).thenReturn(false);
        when(passwordEncoder.encode(CONTRASENA_DE_PRUEBA)).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario guardado = invocacion.getArgument(0);
            guardado.setId("u1");
            return guardado;
        });

        UsuarioResponse respuesta = service.registrar(new RegistroRequest("ana", CONTRASENA_DE_PRUEBA));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getContrasenaHash()).isEqualTo("hash-bcrypt");
        assertThat(captor.getValue().getContrasenaHash()).isNotEqualTo(CONTRASENA_DE_PRUEBA);
        assertThat(respuesta.usuario()).isEqualTo("ana");
        assertThat(respuesta.rol()).isEqualTo(Usuario.ROL_USER);
    }

    @Test
    @DisplayName("registrar lanza DatoDuplicadoException si el usuario ya existe")
    void registrar_usuarioExistente_lanzaExcepcion() {
        when(usuarioRepository.existsByUsuario("ana")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new RegistroRequest("ana", CONTRASENA_DE_PRUEBA)))
                .isInstanceOf(DatoDuplicadoException.class);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("login con credenciales correctas devuelve un par de tokens válidos")
    void login_credencialesCorrectas_devuelveTokens() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("ana", null));

        TokenResponse respuesta = service.login(new LoginRequest("ana", CONTRASENA_DE_PRUEBA));

        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.expiraEnSegundos()).isEqualTo(60L);
        assertThat(jwtService.validarAccessToken(respuesta.accessToken()).getSubject()).isEqualTo("ana");
        assertThat(jwtService.validarRefreshToken(respuesta.refreshToken()).getSubject()).isEqualTo("ana");
    }

    @Test
    @DisplayName("login con credenciales incorrectas propaga BadCredentialsException")
    void login_credencialesIncorrectas_lanzaExcepcion() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("incorrectas"));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana", "otra")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refrescar con un refresh token válido devuelve tokens nuevos")
    void refrescar_tokenValido_devuelveTokensNuevos() {
        when(usuarioRepository.existsByUsuario("ana")).thenReturn(true);
        String refresh = jwtService.generarRefreshToken("ana");

        TokenResponse respuesta = service.refrescar(new RefreshRequest(refresh));

        assertThat(jwtService.validarAccessToken(respuesta.accessToken()).getSubject()).isEqualTo("ana");
    }

    @Test
    @DisplayName("refrescar rechaza un texto que no es un token")
    void refrescar_tokenInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.refrescar(new RefreshRequest("basura")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("refrescar rechaza un access token usado como refresh token")
    void refrescar_conAccessToken_lanzaExcepcion() {
        String access = jwtService.generarAccessToken("ana");

        assertThatThrownBy(() -> service.refrescar(new RefreshRequest(access)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refrescar rechaza el token de un usuario que ya no existe")
    void refrescar_usuarioEliminado_lanzaExcepcion() {
        when(usuarioRepository.existsByUsuario("ana")).thenReturn(false);
        String refresh = jwtService.generarRefreshToken("ana");

        assertThatThrownBy(() -> service.refrescar(new RefreshRequest(refresh)))
                .isInstanceOf(BadCredentialsException.class);
    }
}
