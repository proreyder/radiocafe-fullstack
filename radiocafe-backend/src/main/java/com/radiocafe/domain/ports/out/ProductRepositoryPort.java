package com.radiocafe.domain.ports.out;

import com.radiocafe.domain.model.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {

    boolean existenProductos();

    Optional<Producto> bloquearPorId(Long id);

    List<Producto> obtenerTodos();

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(Long idProducto);

    List<Producto> obtenerTodosDisponibles();

    List<Producto> obtenerPorCategoria(String categoria);

    void eliminarPorId(Long idProducto);
}