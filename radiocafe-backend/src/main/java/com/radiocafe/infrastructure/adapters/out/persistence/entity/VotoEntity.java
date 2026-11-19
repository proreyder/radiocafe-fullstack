package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="votos",uniqueConstraints=@UniqueConstraint(columnNames="idUsuario")) @Getter @Setter @NoArgsConstructor
public class VotoEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false) private Long idUsuario; @Column(nullable=false) private Long idIlustracion; }
