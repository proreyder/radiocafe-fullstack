package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="pedido_invitado_acceso") @Getter @Setter @NoArgsConstructor
public class GuestOrderAccessEntity {
 @Id private Long idPedido;
 @Column(nullable=false,length=64) private String tokenHash;
}
