package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="concurso") @Getter @Setter @NoArgsConstructor
public class ConcursoEntity { @Id private Long id=1L; private Long idGanador; }
