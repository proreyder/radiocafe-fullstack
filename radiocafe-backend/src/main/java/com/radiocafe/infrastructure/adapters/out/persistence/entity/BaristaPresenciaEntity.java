package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="barista_disponibilidad") @Data @NoArgsConstructor @AllArgsConstructor
public class BaristaPresenciaEntity { @Id private Long usuario;private boolean disponible;private Instant actualizado; }
