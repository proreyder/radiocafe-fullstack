package com.radiocafe.infrastructure.adapters.out.persistence.entity;

import com.radiocafe.domain.model.EstadoPedido;
import com.radiocafe.domain.model.TipoEntrega;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEntity {

    @Column(nullable=false, columnDefinition="boolean default false")
    private boolean stockDescontado;

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;
    private Integer minutosPreparacion;
    private java.time.Instant preparacionEstimada;
    @Column(length=100) private String nombreBarista;

    @Column(name = "id_usuario")
    private Long idUsuario;
    @Column(length=100) private String contactoNombre;
    @Column(length=30) private String contactoTelefono;
    @Column(length=150) private String contactoCorreo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false, length = 30)
    private EstadoPedido estadoPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrega", nullable = false, length = 20)
    private TipoEntrega tipoEntrega;

    @Column(name = "direccion_entrega", columnDefinition = "TEXT")
    private String direccionEntrega;

    private Double latitud;
    private Double longitud;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable=false,columnDefinition="numeric(10,2) default 0")
    private BigDecimal descuento = BigDecimal.ZERO;
    @Column(length=30) private String codigoCupon;
    @Column(length=120) private String promocion;

    @Column(name = "costo_envio", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEnvio;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_pedido")
    private List<DetallePedidoEntity> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
