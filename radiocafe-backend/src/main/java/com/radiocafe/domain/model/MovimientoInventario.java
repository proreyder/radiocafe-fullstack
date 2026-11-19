package com.radiocafe.domain.model;
import java.time.LocalDateTime;
public record MovimientoInventario(Long id, Long idProducto, String nombreProducto, int cantidad,
                                  int saldo, String tipo, String motivo, Long idUsuario,
                                  Long idPedido, LocalDateTime fecha) {}
