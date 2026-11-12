package com.utch.productos.service;

import com.utch.productos.dto.ProductoRequest;
import com.utch.productos.dto.ProductoResponse;
import com.utch.productos.exception.DatoDuplicadoException;
import com.utch.productos.exception.RecursoNoEncontradoException;
import com.utch.productos.model.Producto;
import com.utch.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProductoService {

    private static final String RECURSO = "Producto";

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<ProductoResponse> listar() {
        return repository.findAll().stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    public ProductoResponse obtener(String id) {
        return ProductoResponse.desde(buscar(id));
    }

    public ProductoResponse crear(ProductoRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new DatoDuplicadoException("SKU", request.sku());
        }
        Producto producto = new Producto();
        copiarDatos(producto, request);
        Instant ahora = Instant.now();
        producto.setCreadoEn(ahora);
        producto.setActualizadoEn(ahora);
        return ProductoResponse.desde(repository.save(producto));
    }

    public ProductoResponse actualizar(String id, ProductoRequest request) {
        Producto producto = buscar(id);
        if (repository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new DatoDuplicadoException("SKU", request.sku());
        }
        copiarDatos(producto, request);
        producto.setActualizadoEn(Instant.now());
        return ProductoResponse.desde(repository.save(producto));
    }

    public void eliminar(String id) {
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException(RECURSO, id);
        }
        repository.deleteById(id);
    }

    private Producto buscar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(RECURSO, id));
    }

    private void copiarDatos(Producto producto, ProductoRequest request) {
        producto.setNombre(request.nombre());
        producto.setSku(request.sku());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setCategoria(request.categoria());
    }
}
