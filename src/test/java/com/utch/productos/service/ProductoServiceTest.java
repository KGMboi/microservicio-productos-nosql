package com.utch.productos.service;

import com.utch.productos.dto.ProductoRequest;
import com.utch.productos.dto.ProductoResponse;
import com.utch.productos.exception.DatoDuplicadoException;
import com.utch.productos.exception.RecursoNoEncontradoException;
import com.utch.productos.model.Producto;
import com.utch.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService: pruebas unitarias con Mockito")
class ProductoServiceTest {

    private static final String ID = "abc123";
    private static final Instant FECHA_ANTIGUA = Instant.parse("2020-01-01T10:00:00Z");

    // El repositorio se simula: no se necesita MongoDB ni el contexto de Spring
    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    private ProductoRequest request;
    private Producto existente;

    @BeforeEach
    void setUp() {
        request = new ProductoRequest("Laptop Gamer XYZ", "LAP-1001", new BigDecimal("1200.00"), 15, "Computadoras");

        existente = new Producto();
        existente.setId(ID);
        existente.setNombre("Mouse Inalámbrico Pro");
        existente.setSku("MOU-0001");
        existente.setPrecio(new BigDecimal("25.50"));
        existente.setStock(120);
        existente.setCategoria("Accesorios");
        existente.setCreadoEn(FECHA_ANTIGUA);
        existente.setActualizadoEn(FECHA_ANTIGUA);
    }

    // ------------------------------------------------------------ listar y obtener

    @Test
    @DisplayName("listar devuelve los productos del repositorio convertidos a respuesta")
    void listar_conProductos_devuelveLaLista() {
        when(repository.findAll()).thenReturn(List.of(existente));

        List<ProductoResponse> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(ID);
        assertThat(resultado.get(0).sku()).isEqualTo("MOU-0001");
    }

    @Test
    @DisplayName("listar devuelve una lista vacía cuando no hay productos")
    void listar_sinProductos_devuelveListaVacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }

    @Test
    @DisplayName("obtener devuelve el producto cuando existe")
    void obtener_existente_devuelveElProducto() {
        when(repository.findById(ID)).thenReturn(Optional.of(existente));

        ProductoResponse resultado = service.obtener(ID);

        assertThat(resultado.nombre()).isEqualTo("Mouse Inalámbrico Pro");
        assertThat(resultado.precio()).isEqualByComparingTo("25.50");
    }

    @Test
    @DisplayName("obtener lanza RecursoNoEncontradoException cuando no existe")
    void obtener_inexistente_lanzaExcepcion() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(ID))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ID);
    }
}
