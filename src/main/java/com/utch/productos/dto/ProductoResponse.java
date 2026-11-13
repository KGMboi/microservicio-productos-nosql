package com.utch.productos.dto;

import com.utch.productos.model.Producto;

import java.math.BigDecimal;
import java.time.Instant;

// Datos que devuelve la API
public record ProductoResponse(
        String id,
        String nombre,
        String sku,
        BigDecimal precio,
        Integer stock,
        String categoria,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getSku(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getCategoria(),
                producto.getCreadoEn(),
                producto.getActualizadoEn());
    }
}
