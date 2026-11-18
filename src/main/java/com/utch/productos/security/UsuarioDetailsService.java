package com.utch.productos.security;

import com.utch.productos.model.Usuario;
import com.utch.productos.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Le dice a Spring Security cómo cargar un usuario desde MongoDB
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByUsuario(nombreUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario " + nombreUsuario + "."));
        return User.withUsername(usuario.getUsuario())
                .password(usuario.getContrasenaHash())
                .roles(usuario.getRol())
                .build();
    }
}
