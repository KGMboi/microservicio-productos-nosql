package com.utch.productos.security;

import com.utch.productos.model.Usuario;
import com.utch.productos.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializer: creación del usuario administrador al arrancar")
class AdminInitializerTest {

    private static final String CONTRASENA_DE_PRUEBA = "Clave-De-Prueba-1";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Sin contraseña definida no se crea ningún usuario")
    void sinContrasena_noCreaUsuario() {
        new AdminInitializer(usuarioRepository, passwordEncoder, "admin", "").run(null);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Con contraseña definida crea el administrador con el hash y el rol ADMIN")
    void conContrasena_creaAdministrador() {
        when(usuarioRepository.existsByUsuario("admin")).thenReturn(false);
        when(passwordEncoder.encode(CONTRASENA_DE_PRUEBA)).thenReturn("hash-bcrypt");

        new AdminInitializer(usuarioRepository, passwordEncoder, "admin", CONTRASENA_DE_PRUEBA).run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo("admin");
        assertThat(captor.getValue().getContrasenaHash()).isEqualTo("hash-bcrypt");
        assertThat(captor.getValue().getRol()).isEqualTo(Usuario.ROL_ADMIN);
    }

    @Test
    @DisplayName("Si el administrador ya existe no se vuelve a crear")
    void administradorExistente_noSeDuplica() {
        when(usuarioRepository.existsByUsuario("admin")).thenReturn(true);

        new AdminInitializer(usuarioRepository, passwordEncoder, "admin", CONTRASENA_DE_PRUEBA).run(null);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
