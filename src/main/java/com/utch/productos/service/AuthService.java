package com.utch.productos.service;

import com.utch.productos.dto.LoginRequest;
import com.utch.productos.dto.RefreshRequest;
import com.utch.productos.dto.RegistroRequest;
import com.utch.productos.dto.TokenResponse;
import com.utch.productos.dto.UsuarioResponse;
import com.utch.productos.exception.DatoDuplicadoException;
import com.utch.productos.model.Usuario;
import com.utch.productos.repository.UsuarioRepository;
import com.utch.productos.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Registro público: siempre crea usuarios con rol USER
    public UsuarioResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByUsuario(request.usuario())) {
            throw new DatoDuplicadoException("usuario", request.usuario());
        }
        Usuario usuario = new Usuario();
        usuario.setUsuario(request.usuario());
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(Usuario.ROL_USER);
        Usuario guardado = usuarioRepository.save(usuario);
        return new UsuarioResponse(guardado.getId(), guardado.getUsuario(), guardado.getRol());
    }

    // Spring Security compara la contraseña con el hash; si no coincide lanza BadCredentialsException
    public TokenResponse login(LoginRequest request) {
        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usuario(), request.contrasena()));
        return emitirTokens(autenticacion.getName());
    }

    // Renovación: un refresh token válido permite obtener un nuevo par de tokens sin volver a escribir la contraseña
    public TokenResponse refrescar(RefreshRequest request) {
        String usuario;
        try {
            usuario = jwtService.validarRefreshToken(request.refreshToken()).getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("El refresh token no es válido o expiró.", ex);
        }
        if (!usuarioRepository.existsByUsuario(usuario)) {
            throw new BadCredentialsException("El usuario del token ya no existe.");
        }
        return emitirTokens(usuario);
    }

    private TokenResponse emitirTokens(String usuario) {
        return new TokenResponse(
                jwtService.generarAccessToken(usuario),
                jwtService.generarRefreshToken(usuario),
                TIPO_TOKEN,
                jwtService.getAccessExpiracionSegundos());
    }
}
