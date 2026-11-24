package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Data @NoArgsConstructor @AllArgsConstructor
@Table(name="consultas_atencion")
public class ConsultaEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long usuario;
 @Column(nullable=false,length=100) private String autor;
 @Column(nullable=false,length=120) private String asunto;
 @Column(nullable=false,length=2000) private String mensaje;
 @Column(length=2000) private String respuesta;
 private String estado;
 private java.time.LocalDateTime fecha;
 private java.time.LocalDateTime respondida;
}
