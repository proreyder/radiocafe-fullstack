package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Data @NoArgsConstructor @AllArgsConstructor
@Table(name="configuracion_comercio")
public class TiendaEntity {
 @Id private Long id;
 @Column(length=100) private String nombre;
 @Column(length=40) private String tipoComercio;
 @Column(length=1000) private String descripcion;
 @Column(length=200) private String horario;
 @Column(length=30) private String telefono;
 @Column(length=150) private String correo;
}
