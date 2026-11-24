package com.radiocafe.domain.model;
import java.time.Instant;
import java.util.List;
public final class Operacion {
 private Operacion() {}
 public record Notificacion(Long id,Long usuario,Long emisor,String autor,String tipo,String titulo,String mensaje,String enlace,boolean leida,Instant fecha) {}
 public record Bandeja(List<Notificacion> elementos,long noLeidas) {}
 public record Barista(Long usuario,String nombre,boolean disponible,boolean enHorario,boolean disponibilidadManual,String horario,Instant actualizado) {}
 public record Presencia(Long usuario,boolean disponible,Instant actualizado) {}
}
