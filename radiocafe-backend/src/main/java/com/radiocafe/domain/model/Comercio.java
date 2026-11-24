package com.radiocafe.domain.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public final class Comercio {
 private Comercio() {}
 public record Campana(Long id,String titulo,String mensaje,String codigo,BigDecimal porcentaje,BigDecimal minimo,LocalDateTime inicio,LocalDateTime fin,boolean activa) {
  public boolean vigente(LocalDateTime ahora) {return activa && !ahora.isBefore(inicio) && ahora.isBefore(fin);}
 }
 public record Descuento(BigDecimal importe,String codigo,String titulo) {}
 public record Opinion(Long id,Long producto,Long usuario,String autor,int estrellas,String comentario,boolean visible,LocalDateTime fecha) {}
 public record Consulta(Long id,Long usuario,String autor,String asunto,String mensaje,String respuesta,String estado,LocalDateTime fecha,LocalDateTime respondida) {}
 public record Tienda(String nombre,String tipoComercio,String descripcion,String horario,String telefono,String correo) {}
 public record Recomendacion(Producto producto,String motivo) {}
}
