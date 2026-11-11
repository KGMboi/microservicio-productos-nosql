package com.utch.productos.repository;

import com.utch.productos.model.Producto;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductoRepository extends MongoRepository<Producto, String> {

    boolean existsBySku(String sku);

    // Sirve para actualizar: busca el SKU en cualquier otro producto distinto del actual
    boolean existsBySkuAndIdNot(String sku, String id);
}
