package com.utch.productos.security;

import com.utch.productos.model.Usuario;
import com.utch.productos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Al arrancar crea el usuario administrador, solo si se definió la variable de entorno ADMIN_PASSWORD.
// Así la contraseña del administrador nunca queda escrita en el código ni en el repositorio.
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String usuarioAdmin;
    private final String contrasenaAdmin;

    public AdminInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.usuario}") String usuarioAdmin,
                            @Value("${app.admin.contrasena:}") String contrasenaAdmin) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioAdmin = usuarioAdmin;
        this.contrasenaAdmin = contrasenaAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (contrasenaAdmin.isBlank()) {
            LOG.info("ADMIN_PASSWORD no está definida: no se crea el usuario administrador.");
            return;
        }
        if (usuarioRepository.existsByUsuario(usuarioAdmin)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsuario(usuarioAdmin);
        admin.setContrasenaHash(passwordEncoder.encode(contrasenaAdmin));
        admin.setRol(Usuario.ROL_ADMIN);
        usuarioRepository.save(admin);
        LOG.info("Usuario administrador '{}' creado.", usuarioAdmin);
    }
}
