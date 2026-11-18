package com.utch.productos;

import com.jayway.jsonpath.JsonPath;
import com.utch.productos.model.Usuario;
import com.utch.productos.repository.ProductoRepository;
import com.utch.productos.repository.UsuarioRepository;
import com.utch.productos.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de seguridad con la cadena de filtros real: rutas bloqueadas, tokens inválidos, roles, login y renovación.
// Los repositorios de MongoDB se simulan para que no se necesite un servidor encendido.
@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
@DisplayName("Seguridad de la API: autenticación y autorización con JWT")
class SeguridadIntegrationTest {

    private static final String PRODUCTOS = "/api/v1/productos";
    private static final String PRODUCTO_ID = PRODUCTOS + "/abc123";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String REGISTRO = "/api/v1/auth/registro";
    private static final String REFRESH = "/api/v1/auth/refresh";
    private static final String CODIGO = "$.codigo";
    private static final String CONTRASENA_DE_PRUEBA = "Clave-De-Prueba-1";

    private static final String JSON_PRODUCTO = """
            {"nombre":"Laptop Gamer XYZ","sku":"LAP-1001","precio":1200.00,"stock":15,"categoria":"Computadoras"}
            """;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> TestSecrets.JWT_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ProductoRepository productoRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void usuariosDeLaBaseDeDatos() {
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(crearUsuario("ana", Usuario.ROL_USER)));
        when(usuarioRepository.findByUsuario("root")).thenReturn(Optional.of(crearUsuario("root", Usuario.ROL_ADMIN)));
    }

    private Usuario crearUsuario(String nombre, String rol) {
        Usuario usuario = new Usuario();
        usuario.setId(nombre);
        usuario.setUsuario(nombre);
        usuario.setContrasenaHash(passwordEncoder.encode(CONTRASENA_DE_PRUEBA));
        usuario.setRol(rol);
        return usuario;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String credenciales(String usuario, String contrasena) {
        return "{\"usuario\":\"" + usuario + "\",\"contrasena\":\"" + contrasena + "\"}";
    }

    // ------------------------------------------------------------ rutas protegidas sin credenciales o con token inválido

    @Test
    @DisplayName("Una ruta protegida sin credenciales responde 401 NO_AUTENTICADO")
    void sinCredenciales_responde401() throws Exception {
        mockMvc.perform(get(PRODUCTOS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath(CODIGO).value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Un encabezado Authorization que no es Bearer responde 401")
    void esquemaDistintoDeBearer_responde401() throws Exception {
        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, "Basic YW5hOmNsYXZl"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Un texto que no es un token responde 401 TOKEN_INVALIDO")
    void tokenInvalido_responde401() throws Exception {
        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer("esto.no.es.un.token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("Un token firmado con otra llave responde 401 TOKEN_INVALIDO")
    void tokenFirmadoConOtraLlave_responde401() throws Exception {
        String ajeno = new JwtService(TestSecrets.OTRA_LLAVE, 60_000L, 60_000L).generarAccessToken("ana");

        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(ajeno)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("Un token expirado responde 401 TOKEN_EXPIRADO")
    void tokenExpirado_responde401() throws Exception {
        String expirado = new JwtService(TestSecrets.JWT_SECRET, -1000L, -1000L).generarAccessToken("ana");

        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(expirado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("TOKEN_EXPIRADO"));
    }

    @Test
    @DisplayName("Un refresh token no se acepta para entrar a una ruta protegida")
    void refreshTokenComoAccessToken_responde401() throws Exception {
        String refresh = jwtService.generarRefreshToken("ana");

        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(refresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("Un token de un usuario que ya no existe responde 401 TOKEN_INVALIDO")
    void tokenDeUsuarioInexistente_responde401() throws Exception {
        String token = jwtService.generarAccessToken("fantasma");

        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("TOKEN_INVALIDO"));
    }

    // ------------------------------------------------------------ autorización por roles

    @Test
    @DisplayName("Un usuario con rol USER puede consultar productos")
    void usuarioUser_puedeLeer() throws Exception {
        when(productoRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarAccessToken("ana"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Un usuario con rol USER no puede crear productos: 403 ACCESO_DENEGADO")
    void usuarioUser_noPuedeCrear() throws Exception {
        mockMvc.perform(post(PRODUCTOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarAccessToken("ana")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PRODUCTO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(CODIGO).value("ACCESO_DENEGADO"));

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un usuario con rol USER no puede eliminar productos: 403")
    void usuarioUser_noPuedeEliminar() throws Exception {
        mockMvc.perform(delete(PRODUCTO_ID).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarAccessToken("ana"))))
                .andExpect(status().isForbidden());

        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Un usuario con rol ADMIN puede eliminar productos")
    void usuarioAdmin_puedeEliminar() throws Exception {
        when(productoRepository.existsById("abc123")).thenReturn(true);

        mockMvc.perform(delete(PRODUCTO_ID).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarAccessToken("root"))))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------ login y renovación

    @Test
    @DisplayName("Login correcto devuelve tokens, y el access token permite entrar a una ruta protegida")
    void login_correcto_devuelveTokensUtilizables() throws Exception {
        when(productoRepository.findAll()).thenReturn(List.of());

        String respuesta = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("ana", CONTRASENA_DE_PRUEBA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(900))
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(respuesta, "$.accessToken");

        mockMvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login con contraseña incorrecta responde 401 CREDENCIALES_INVALIDAS")
    void login_contrasenaIncorrecta_responde401() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credenciales("ana", "otra-clave")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("Login con un usuario inexistente responde lo mismo que con contraseña incorrecta")
    void login_usuarioInexistente_responde401() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("fantasma", CONTRASENA_DE_PRUEBA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("Login con campos vacíos responde 400 VALIDACION_FALLIDA")
    void login_camposVacios_responde400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credenciales("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODIGO).value("VALIDACION_FALLIDA"));
    }

    @Test
    @DisplayName("Un refresh token válido permite obtener un nuevo access token")
    void refresh_tokenValido_devuelveNuevoAccessToken() throws Exception {
        when(usuarioRepository.existsByUsuario("ana")).thenReturn(true);
        String refresh = jwtService.generarRefreshToken("ana");

        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("Renovar con un texto que no es un token responde 401")
    void refresh_tokenInvalido_responde401() throws Exception {
        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"basura\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODIGO).value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("Renovar con un access token responde 401")
    void refresh_conAccessToken_responde401() throws Exception {
        String access = jwtService.generarAccessToken("ana");

        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + access + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ registro

    @Test
    @DisplayName("El registro crea un usuario con rol USER y nunca devuelve la contraseña")
    void registro_usuarioNuevo_responde201() throws Exception {
        when(usuarioRepository.existsByUsuario("nuevo")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).then(returnsFirstArg());

        mockMvc.perform(post(REGISTRO).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("nuevo", CONTRASENA_DE_PRUEBA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("nuevo"))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    @DisplayName("Registrar un usuario repetido responde 409")
    void registro_usuarioRepetido_responde409() throws Exception {
        when(usuarioRepository.existsByUsuario("ana")).thenReturn(true);

        mockMvc.perform(post(REGISTRO).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("ana", CONTRASENA_DE_PRUEBA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(CODIGO).value("DATO_DUPLICADO"));
    }

    @Test
    @DisplayName("Registrar con una contraseña corta responde 400")
    void registro_contrasenaCorta_responde400() throws Exception {
        mockMvc.perform(post(REGISTRO).contentType(MediaType.APPLICATION_JSON).content(credenciales("nuevo", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODIGO).value("VALIDACION_FALLIDA"));
    }
}
