package com.utch.productos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Datos que recibe la API para crear o actualizar un producto
public record ProductoRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres.")
        String nombre,

        @NotBlank(message = "El SKU es obligatorio.")
        @Pattern(regexp = "^[A-Z]{3}-\\d{4}$", message = "El SKU debe tener el formato ABC-1234.")
        String sku,

        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0.")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 2 decimales.")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio.")
        @Min(value = 0, message = "El stock no puede ser negativo.")
        Integer stock,

        @NotBlank(message = "La categoría es obligatoria.")
        @Size(max = 50, message = "La categoría admite máximo 50 caracteres.")
        String categoria) {
}
