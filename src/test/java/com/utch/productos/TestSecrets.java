package com.utch.productos;

import java.security.SecureRandom;
import java.util.Base64;

// Llaves aleatorias para las pruebas: así no hay secretos escritos en el código
public final class TestSecrets {

    public static final String JWT_SECRET = generar();
    public static final String OTRA_LLAVE = generar();

    private TestSecrets() {
    }

    private static String generar() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
