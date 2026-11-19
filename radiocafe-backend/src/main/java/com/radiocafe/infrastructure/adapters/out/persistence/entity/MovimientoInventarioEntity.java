package com.radiocafe.infrastructure.adapters.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="movimientos_inventario") @Getter @Setter @NoArgsConstructor
public class MovimientoInventarioEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long idProducto;
    @Column(nullable=false,length=120) private String nombreProducto;
    private int cantidad;
    private int saldo;
    @Column(nullable=false,length=30) private String tipo;
    @Column(nullable=false,length=200) private String motivo;
    private Long idUsuario;
    private Long idPedido;
    @Column(nullable=false) private LocalDateTime fecha;
}
