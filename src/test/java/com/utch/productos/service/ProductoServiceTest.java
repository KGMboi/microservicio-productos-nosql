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

    // ------------------------------------------------------------ crear

    @Test
    @DisplayName("crear guarda el producto con sus datos y fechas de registro")
    void crear_datosValidos_guardaElProducto() {
        when(repository.existsBySku("LAP-1001")).thenReturn(false);
        when(repository.save(any(Producto.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());

        ProductoResponse resultado = service.crear(request);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(repository).save(captor.capture());
        Producto guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Laptop Gamer XYZ");
        assertThat(guardado.getSku()).isEqualTo("LAP-1001");
        assertThat(guardado.getPrecio()).isEqualByComparingTo("1200.00");
        assertThat(guardado.getStock()).isEqualTo(15);
        assertThat(guardado.getCategoria()).isEqualTo("Computadoras");
        assertThat(guardado.getCreadoEn()).isNotNull();
        assertThat(guardado.getActualizadoEn()).isEqualTo(guardado.getCreadoEn());
        assertThat(resultado.sku()).isEqualTo("LAP-1001");
    }

    @Test
    @DisplayName("crear lanza DatoDuplicadoException si el SKU ya existe y no guarda")
    void crear_skuDuplicado_lanzaExcepcionYNoGuarda() {
        when(repository.existsBySku("LAP-1001")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(DatoDuplicadoException.class)
                .hasMessageContaining("LAP-1001");
        verify(repository, never()).save(any(Producto.class));
    }

    // ------------------------------------------------------------ actualizar

    @Test
    @DisplayName("actualizar modifica los datos y la fecha de actualización")
    void actualizar_datosValidos_guardaLosCambios() {
        when(repository.findById(ID)).thenReturn(Optional.of(existente));
        when(repository.existsBySkuAndIdNot("LAP-1001", ID)).thenReturn(false);
        when(repository.save(any(Producto.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());

        ProductoResponse resultado = service.actualizar(ID, request);

        assertThat(resultado.id()).isEqualTo(ID);
        assertThat(resultado.nombre()).isEqualTo("Laptop Gamer XYZ");
        assertThat(resultado.sku()).isEqualTo("LAP-1001");
        assertThat(resultado.stock()).isEqualTo(15);
        assertThat(resultado.creadoEn()).isEqualTo(FECHA_ANTIGUA);
        assertThat(resultado.actualizadoEn()).isAfter(FECHA_ANTIGUA);
    }

    @Test
    @DisplayName("actualizar lanza RecursoNoEncontradoException si el producto no existe")
    void actualizar_inexistente_lanzaExcepcion() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(ID, request))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).save(any(Producto.class));
    }

    @Test
    @DisplayName("actualizar lanza DatoDuplicadoException si el SKU pertenece a otro producto")
    void actualizar_skuDeOtroProducto_lanzaExcepcion() {
        when(repository.findById(ID)).thenReturn(Optional.of(existente));
        when(repository.existsBySkuAndIdNot("LAP-1001", ID)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(ID, request))
                .isInstanceOf(DatoDuplicadoException.class);
        verify(repository, never()).save(any(Producto.class));
    }

    // ------------------------------------------------------------ eliminar

    @Test
    @DisplayName("eliminar borra el producto cuando existe")
    void eliminar_existente_borraElProducto() {
        when(repository.existsById(ID)).thenReturn(true);

        service.eliminar(ID);

        verify(repository).deleteById(ID);
    }

    @Test
    @DisplayName("eliminar lanza RecursoNoEncontradoException si no existe y no borra")
    void eliminar_inexistente_lanzaExcepcion() {
        when(repository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminar(ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).deleteById(ID);
    }
}
