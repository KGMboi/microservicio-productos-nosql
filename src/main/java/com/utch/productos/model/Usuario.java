package com.utch.productos.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

// Usuario del sistema. La contraseña nunca se guarda en texto plano, solo su hash BCrypt.
@Document(collection = "usuarios")
public class Usuario {

    public static final String ROL_USER = "USER";
    public static final String ROL_ADMIN = "ADMIN";

    @Id
    private String id;

    @Indexed(unique = true)
    private String usuario;

    private String contrasenaHash;

    private String rol;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
