package com.utch.productos.controller;

import com.utch.productos.dto.ProductoRequest;
import com.utch.productos.dto.ProductoResponse;
import com.utch.productos.exception.DatoDuplicadoException;
import com.utch.productos.exception.RecursoNoEncontradoException;
import com.utch.productos.service.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba de integración de la capa web: controlador, validación, serialización JSON y manejador de errores
@WebMvcTest(ProductoController.class)
@DisplayName("ProductoController: integración de los endpoints web")
class ProductoControllerTest {

    private static final String BASE = "/api/v1/productos";
    private static final String BASE_ID = BASE + "/abc123";
    private static final String CODIGO = "$.codigo";

    private static final String JSON_VALIDO = """
            {"nombre":"Laptop Gamer XYZ","sku":"LAP-1001","precio":1200.00,"stock":15,"categoria":"Computadoras"}
            """;

    private static final String JSON_INVALIDO = """
            {"nombre":"ab","sku":"mal","precio":-5,"stock":-1,"categoria":""}
            """;

    private static final ProductoResponse RESPUESTA = new ProductoResponse(
            "abc123", "Laptop Gamer XYZ", "LAP-1001", new BigDecimal("1200.00"), 15, "Computadoras",
            Instant.parse("2026-11-11T12:00:00Z"), Instant.parse("2026-11-11T12:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService service;

    // ------------------------------------------------------------ lectura

    @Test
    @DisplayName("GET /productos responde 200 con la lista en JSON")
    void listar_responde200ConLaLista() throws Exception {
        given(service.listar()).willReturn(List.of(RESPUESTA));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("LAP-1001"))
                .andExpect(jsonPath("$[0].precio").value(1200.0));
    }

    @Test
    @DisplayName("GET /productos/{id} responde 200 con el producto serializado")
    void obtener_existente_responde200() throws Exception {
        given(service.obtener("abc123")).willReturn(RESPUESTA);

        mockMvc.perform(get(BASE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.nombre").value("Laptop Gamer XYZ"))
                .andExpect(jsonPath("$.stock").value(15))
                .andExpect(jsonPath("$.creadoEn").value("2026-11-11T12:00:00Z"));
    }

    @Test
    @DisplayName("GET /productos/{id} responde 404 con el formato de error cuando no existe")
    void obtener_inexistente_responde404() throws Exception {
        given(service.obtener("abc123")).willThrow(new RecursoNoEncontradoException("Producto", "abc123"));

        mockMvc.perform(get(BASE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath(CODIGO).value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value(containsString("abc123")))
                .andExpect(jsonPath("$.ruta").value(BASE_ID));
    }

    // ------------------------------------------------------------ creación

    @Test
    @DisplayName("POST /productos responde 201 con encabezado Location")
    void crear_datosValidos_responde201() throws Exception {
        given(service.crear(any(ProductoRequest.class))).willReturn(RESPUESTA);

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE_ID))
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.sku").value("LAP-1001"));
    }

    @Test
    @DisplayName("POST /productos con datos inválidos responde 400 y lista los campos con error")
    void crear_datosInvalidos_responde400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON_INVALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODIGO).value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.errores[*].campo",
                        hasItems("nombre", "sku", "precio", "stock", "categoria")));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /productos con JSON mal formado responde 400")
    void crear_jsonMalFormado_responde400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{nombre:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODIGO).value("SOLICITUD_MAL_FORMADA"));
    }

    @Test
    @DisplayName("POST /productos con SKU repetido responde 409")
    void crear_skuDuplicado_responde409() throws Exception {
        given(service.crear(any(ProductoRequest.class))).willThrow(new DatoDuplicadoException("SKU", "LAP-1001"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(CODIGO).value("DATO_DUPLICADO"))
                .andExpect(jsonPath("$.mensaje").value(containsString("LAP-1001")));
    }

    @Test
    @DisplayName("POST /productos con Content-Type distinto de JSON responde 415")
    void crear_contentTypeNoSoportado_responde415() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath(CODIGO).value("TIPO_NO_SOPORTADO"));
    }

    // ------------------------------------------------------------ actualización y borrado

    @Test
    @DisplayName("PUT /productos/{id} responde 200 con el producto actualizado")
    void actualizar_datosValidos_responde200() throws Exception {
        given(service.actualizar(eq("abc123"), any(ProductoRequest.class))).willReturn(RESPUESTA);

        mockMvc.perform(put(BASE_ID).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laptop Gamer XYZ"));
    }

    @Test
    @DisplayName("PUT /productos/{id} responde 404 cuando el producto no existe")
    void actualizar_inexistente_responde404() throws Exception {
        given(service.actualizar(eq("abc123"), any(ProductoRequest.class)))
                .willThrow(new RecursoNoEncontradoException("Producto", "abc123"));

        mockMvc.perform(put(BASE_ID).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(CODIGO).value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("DELETE /productos/{id} responde 204 sin cuerpo")
    void eliminar_existente_responde204() throws Exception {
        mockMvc.perform(delete(BASE_ID))
                .andExpect(status().isNoContent());

        verify(service).eliminar("abc123");
    }

    @Test
    @DisplayName("DELETE /productos/{id} responde 404 cuando no existe")
    void eliminar_inexistente_responde404() throws Exception {
        willThrow(new RecursoNoEncontradoException("Producto", "abc123")).given(service).eliminar("abc123");

        mockMvc.perform(delete(BASE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(CODIGO).value("RECURSO_NO_ENCONTRADO"));
    }

    // ------------------------------------------------------------ otros errores

    @Test
    @DisplayName("Un método HTTP no permitido responde 405")
    void metodoNoPermitido_responde405() throws Exception {
        mockMvc.perform(patch(BASE))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath(CODIGO).value("METODO_NO_PERMITIDO"));
    }

    @Test
    @DisplayName("Una ruta inexistente responde 404")
    void rutaInexistente_responde404() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un error inesperado responde 500 sin exponer detalles internos")
    void errorInesperado_responde500SinDetalles() throws Exception {
        given(service.listar()).willThrow(new IllegalStateException("detalle secreto de la base de datos"));

        mockMvc.perform(get(BASE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(CODIGO).value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value(not(containsString("secreto"))));
    }
}
