package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Data @NoArgsConstructor @AllArgsConstructor
@Table(name="opiniones",uniqueConstraints=@UniqueConstraint(columnNames={"producto","usuario"}))
public class OpinionEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long producto;
 @Column(nullable=false) private Long usuario;
 @Column(nullable=false,length=100) private String autor;
 private int estrellas;
 @Column(nullable=false,length=1500) private String comentario;
 private boolean visible;
 private java.time.LocalDateTime fecha;
}
