package com.radiocafe.domain.model;

import java.math.BigDecimal;

public record Producto(Long idProducto, String nombre, String descripcion, String categoria,
                       BigDecimal precio, String imagenUrl, Boolean disponible,
                       Boolean personalizable, Integer stock) {
    public Producto(Long id, String nombre, String descripcion, String categoria, BigDecimal precio,
                    String imagen, Boolean disponible, Boolean personalizable) {
        this(id, nombre, descripcion, categoria, precio, imagen, disponible, personalizable, null);
    }
    public Producto(Long id, String nombre, String descripcion, String categoria, BigDecimal precio,
                    String imagen, Boolean disponible) {
        this(id, nombre, descripcion, categoria, precio, imagen, disponible, false, null);
    }
    public int stockDisponible() { return stock == null ? 0 : stock; }
    public Producto conStock(int nuevoStock) {
        if (nuevoStock < 0 || nuevoStock > 999999) throw new IllegalArgumentException("El stock debe estar entre 0 y 999999.");
        return new Producto(idProducto, nombre, descripcion, categoria, precio, imagenUrl, disponible, personalizable, nuevoStock);
    }
}
