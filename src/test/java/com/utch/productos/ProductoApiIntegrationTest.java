package com.utch.productos;

import com.jayway.jsonpath.JsonPath;
import com.utch.productos.model.Producto;
import com.utch.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba de integración del flujo completo: controlador, validación, servicio y manejador de errores.
// El repositorio de MongoDB se sustituye por una base en memoria para que la prueba no dependa de un servidor.
@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
@DisplayName("API de productos: flujo CRUD completo")
class ProductoApiIntegrationTest {

    private static final String BASE = "/api/v1/productos";

    private static final String LAPTOP = """
            {"nombre":"Laptop Gamer XYZ","sku":"LAP-1001","precio":1200.00,"stock":15,"categoria":"Computadoras"}
            """;

    private static final String LAPTOP_ACTUALIZADA = """
            {"nombre":"Laptop Gamer XYZ Pro","sku":"LAP-1001","precio":1350.50,"stock":10,"categoria":"Computadoras"}
            """;

    private static final String MOUSE_CON_SKU_DE_LAPTOP = """
            {"nombre":"Mouse Inalámbrico","sku":"LAP-1001","precio":25.50,"stock":120,"categoria":"Accesorios"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoRepository repository;

    // Base de datos en memoria que reemplaza a MongoDB
    private final Map<String, Producto> bd = new HashMap<>();

    @BeforeEach
    void configurarRepositorioEnMemoria() {
        bd.clear();

        when(repository.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto producto = invocacion.getArgument(0);
            if (producto.getId() == null) {
                producto.setId(UUID.randomUUID().toString());
            }
            bd.put(producto.getId(), producto);
            return producto;
        });
        when(repository.findById(anyString()))
                .thenAnswer(invocacion -> Optional.ofNullable(bd.get(invocacion.<String>getArgument(0))));
        when(repository.findAll()).thenAnswer(invocacion -> new ArrayList<>(bd.values()));
        when(repository.existsById(anyString()))
                .thenAnswer(invocacion -> bd.containsKey(invocacion.<String>getArgument(0)));
        when(repository.existsBySku(anyString()))
                .thenAnswer(invocacion -> bd.values().stream()
                        .anyMatch(p -> p.getSku().equals(invocacion.<String>getArgument(0))));
        when(repository.existsBySkuAndIdNot(anyString(), anyString()))
                .thenAnswer(invocacion -> bd.values().stream()
                        .anyMatch(p -> p.getSku().equals(invocacion.<String>getArgument(0))
                                && !p.getId().equals(invocacion.<String>getArgument(1))));
        doAnswer(invocacion -> {
            bd.remove(invocacion.<String>getArgument(0));
            return null;
        }).when(repository).deleteById(anyString());
    }

    private String crearLaptop() throws Exception {
        String respuesta = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(LAPTOP))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    @DisplayName("Crear, consultar, actualizar y eliminar un producto")
    void flujoCrud_completo() throws Exception {
        // Crear
        String id = crearLaptop();

        // Leer uno y leer todos
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("LAP-1001"))
                .andExpect(jsonPath("$.stock").value(15))
                .andExpect(jsonPath("$.creadoEn").exists());
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Actualizar
        mockMvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content(LAPTOP_ACTUALIZADA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laptop Gamer XYZ Pro"))
                .andExpect(jsonPath("$.precio").value(1350.5))
                .andExpect(jsonPath("$.stock").value(10));

        // Eliminar y comprobar que ya no existe
        mockMvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Crear dos productos con el mismo SKU responde 409")
    void crear_skuRepetido_responde409() throws Exception {
        crearLaptop();

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(LAPTOP))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DATO_DUPLICADO"));
    }

    @Test
    @DisplayName("Actualizar con el SKU de otro producto responde 409")
    void actualizar_skuDeOtroProducto_responde409() throws Exception {
        crearLaptop();
        String respuesta = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mouse Inalámbrico","sku":"MOU-0001","precio":25.50,"stock":120,"categoria":"Accesorios"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String idMouse = JsonPath.read(respuesta, "$.id");

        mockMvc.perform(put(BASE + "/" + idMouse).contentType(MediaType.APPLICATION_JSON)
                        .content(MOUSE_CON_SKU_DE_LAPTOP))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DATO_DUPLICADO"));
    }

    @Test
    @DisplayName("Un producto con datos inválidos no se guarda y responde 400")
    void crear_datosInvalidos_responde400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","sku":"123","precio":0,"stock":-3,"categoria":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));

        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Eliminar o actualizar un producto inexistente responde 404")
    void operacionesSobreProductoInexistente_responden404() throws Exception {
        mockMvc.perform(delete(BASE + "/no-existe"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(BASE + "/no-existe").contentType(MediaType.APPLICATION_JSON).content(LAPTOP))
                .andExpect(status().isNotFound());
    }
}
