package com.utch.productos.security;

import com.utch.productos.model.Usuario;
import com.utch.productos.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioDetailsService: carga de usuarios para Spring Security")
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService service;

    @Test
    @DisplayName("Un usuario existente se convierte a UserDetails con su rol")
    void usuarioExistente_devuelveDetallesConRol() {
        Usuario usuario = new Usuario();
        usuario.setUsuario("ana");
        usuario.setContrasenaHash("hash-bcrypt");
        usuario.setRol(Usuario.ROL_ADMIN);
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(usuario));

        UserDetails detalles = service.loadUserByUsername("ana");

        assertThat(detalles.getUsername()).isEqualTo("ana");
        assertThat(detalles.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Un usuario inexistente lanza UsernameNotFoundException")
    void usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByUsuario("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nadie");
    }
}
