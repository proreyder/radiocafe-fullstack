package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Data @NoArgsConstructor @AllArgsConstructor
@Table(name="campanas",uniqueConstraints=@UniqueConstraint(columnNames="codigo"))
public class CampanaEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=120) private String titulo;
 @Column(nullable=false,length=1000) private String mensaje;
 @Column(length=30) private String codigo;
 @Column(nullable=false,precision=5,scale=2) private java.math.BigDecimal porcentaje;
 @Column(nullable=false,precision=10,scale=2) private java.math.BigDecimal minimo;
 @Column(nullable=false) private java.time.LocalDateTime inicio;
 @Column(nullable=false) private java.time.LocalDateTime fin;
 private boolean activa;
}
