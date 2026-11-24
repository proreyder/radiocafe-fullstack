package com.radiocafe.domain.model;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.List;
public record CuentaActividad(BigDecimal pedidosVigentes,BigDecimal entregados,BigDecimal cancelados,BigDecimal descuentos,List<Movimiento> movimientos) {
 public record Movimiento(Long pedido,LocalDateTime fecha,EstadoPedido estado,BigDecimal subtotal,BigDecimal descuento,BigDecimal envio,BigDecimal total,int unidades) {}
}
