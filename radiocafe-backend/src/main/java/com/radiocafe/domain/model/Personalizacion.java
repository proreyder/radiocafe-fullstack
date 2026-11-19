package com.radiocafe.domain.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/** The server owns the available choices and their surcharges. */
public record Personalizacion(String tamano, String leche, String crema, List<String> extras) {
    public record Opcion(String id, String nombre, BigDecimal recargo) {}
    public record Catalogo(List<Opcion> tamanos, List<Opcion> leches, List<Opcion> cremas, List<Opcion> extras, int maxExtras) {}
    public record Resultado(BigDecimal precio, String descripcion) {}

    public Personalizacion {
        extras = extras == null ? List.of() : List.copyOf(extras);
    }

    private static Opcion opcion(String id, String nombre, String precio) {
        return new Opcion(id, nombre, new BigDecimal(precio));
    }

    public static Catalogo catalogo() {
        return new Catalogo(
            List.of(opcion("ALTO", "Alto (300 ml)", "0.00"), opcion("GRANDE", "Grande (400 ml)", "1.00"), opcion("VENTI", "Venti (500 ml)", "2.00")),
            List.of(opcion("ENTERA", "Leche entera", "0.00"), opcion("SIN_LACTOSA", "Leche sin lactosa", "0.00"), opcion("DESCREMADA", "Leche descremada", "0.00"), opcion("SOYA", "Bebida de soya", "0.50"), opcion("AVENA", "Bebida de avena", "0.75"), opcion("ALMENDRAS", "Bebida de almendras", "0.75")),
            List.of(opcion("CON_CREMA", "Con crema batida", "0.00"), opcion("SIN_CREMA", "Sin crema batida", "0.00")),
            List.of(opcion("MOCHA", "Topping de mocha", "0.75"), opcion("VAINILLA", "Jarabe de vainilla", "0.50"), opcion("CARAMELO", "Topping de caramelo", "0.75"), opcion("CHOCOLATE", "Chispas de chocolate", "0.75"), opcion("ESPRESSO", "Shot de espresso", "1.00")), 5);
    }

    private static Opcion buscar(List<Opcion> opciones, String id) {
        return opciones.stream().filter(o -> o.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Selecciona una opción válida para personalizar la bebida."));
    }

    public Resultado aplicar(Producto producto) {
        if (!Boolean.TRUE.equals(producto.personalizable())) {
            throw new IllegalArgumentException("Este producto no admite personalización.");
        }
        Catalogo c = catalogo();
        if (extras.size() > c.maxExtras() || new HashSet<>(extras).size() != extras.size()) {
            throw new IllegalArgumentException("Los extras deben ser únicos y no superar el máximo permitido.");
        }
        Opcion size = buscar(c.tamanos(), tamano);
        Opcion milk = buscar(c.leches(), leche);
        Opcion cream = buscar(c.cremas(), crema);
        BigDecimal precio = producto.precio().add(size.recargo()).add(milk.recargo()).add(cream.recargo());
        StringBuilder descripcion = new StringBuilder(size.nombre()).append(" · ").append(milk.nombre()).append(" · ").append(cream.nombre());
        for (String extra : extras) {
            Opcion opcion = buscar(c.extras(), extra);
            precio = precio.add(opcion.recargo());
            descripcion.append(" · ").append(opcion.nombre());
        }
        return new Resultado(precio, descripcion.toString());
    }
}
