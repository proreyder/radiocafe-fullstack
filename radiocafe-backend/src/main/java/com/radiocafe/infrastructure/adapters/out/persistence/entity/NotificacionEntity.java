package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="notificaciones",indexes=@Index(name="idx_notificacion_usuario",columnList="usuario,leida")) @Data @NoArgsConstructor @AllArgsConstructor
public class NotificacionEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long usuario;private Long emisor;
 @Column(length=100) private String autor;@Column(length=30) private String tipo;
 @Column(length=160) private String titulo;@Column(length=2000) private String mensaje;@Column(length=150) private String enlace;
 private boolean leida;private Instant fecha;
}
