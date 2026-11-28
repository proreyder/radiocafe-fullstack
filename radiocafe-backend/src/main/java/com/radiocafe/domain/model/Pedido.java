package com.radiocafe.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pedido {

    private boolean stockDescontado;
    public boolean isStockDescontado() { return stockDescontado; }
    public void setStockDescontado(boolean value) { stockDescontado=value; }
    private Integer minutosPreparacion;
    private java.time.Instant preparacionEstimada;
    private String nombreBarista;
    public Integer getMinutosPreparacion() {return minutosPreparacion;}
    public void setMinutosPreparacion(Integer n) {minutosPreparacion=n;}
    public java.time.Instant getPreparacionEstimada() {return preparacionEstimada;}
    public void setPreparacionEstimada(java.time.Instant t) {preparacionEstimada=t;}
    public String getNombreBarista() {return nombreBarista;}
    public void setNombreBarista(String n) {nombreBarista=n;}
    private Long idPedido;
    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    private Long idUsuario;
    private ContactoPedido contacto;
    public ContactoPedido getContacto() { return contacto; }
    public void setContacto(ContactoPedido contacto) { this.contacto=contacto; }
    private EstadoPedido estadoPedido;
    private TipoEntrega tipoEntrega;
    private String direccionEntrega;
    private Double latitud;
    private Double longitud;
    private BigDecimal descuento = BigDecimal.ZERO;
    private String codigoCupon;
    private String promocion;
    public BigDecimal getDescuento() {return descuento;}
    public String getCodigoCupon() {return codigoCupon;}
    public void setCodigoCupon(String codigo) {this.codigoCupon=codigo;}
    public String getPromocion() {return promocion;}
    public void aplicarDescuento(BigDecimal importe,String codigo,String titulo) {
        this.descuento=importe==null?BigDecimal.ZERO:importe;this.codigoCupon=codigo;this.promocion=titulo;recalcularTotales();
    }
    private BigDecimal subtotal;
    private BigDecimal costoEnvio;
    private BigDecimal total;
    private LocalDateTime fechaCreacion;
    private List<DetallePedido> detalles;

    public Pedido() {
        this.detalles = new ArrayList<>();
        this.estadoPedido = EstadoPedido.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO;
        this.costoEnvio = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
    }

    public void agregarDetalle(DetallePedido detalle) {
        this.detalles.add(detalle);
        recalcularTotales();
    }

    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(DetallePedido::getSubtotalLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Regla de Negocio: Envío Gratis si Subtotal >= S/ 50.00
        if (this.tipoEntrega == TipoEntrega.DELIVERY) {
            if (this.subtotal.compareTo(new BigDecimal("50.00")) >= 0) {
                this.costoEnvio = BigDecimal.ZERO;
            } else {
                this.costoEnvio = new BigDecimal("5.00"); // Costo fijo estándar
            }
        } else {
            this.costoEnvio = BigDecimal.ZERO; // PICKUP no cobra envío
        }

        this.total = this.subtotal.subtract(this.descuento.min(this.subtotal)).add(this.costoEnvio);
    }

    public void cambiarEstado(EstadoPedido nuevo) {
        boolean permitido = nuevo != null && switch (estadoPedido) {
            case PENDIENTE -> nuevo == EstadoPedido.EN_PREPARACION || nuevo == EstadoPedido.CANCELLED;
            case EN_PREPARACION -> nuevo == EstadoPedido.LISTO || nuevo == EstadoPedido.CANCELLED;
            case LISTO -> tipoEntrega == TipoEntrega.DELIVERY ? nuevo == EstadoPedido.EN_CAMINO : nuevo == EstadoPedido.ENTREGADO;
            case EN_CAMINO -> nuevo == EstadoPedido.ENTREGADO;
            default -> false;
        };
        if (!permitido) throw BusinessException.conflicto("Cambio de estado no permitido.");
        estadoPedido = nuevo;
    }

    // Getters y Setters
    public Long getIdPedido() { return idPedido; }
    public void setIdPedido(Long idPedido) { this.idPedido = idPedido; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public EstadoPedido getEstadoPedido() { return estadoPedido; }
    public void setEstadoPedido(EstadoPedido estadoPedido) { this.estadoPedido = estadoPedido; }

    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(TipoEntrega tipoEntrega) { 
        this.tipoEntrega = tipoEntrega; 
        recalcularTotales();
    }

    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public BigDecimal getTotal() { return total; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { 
        this.detalles = detalles; 
        recalcularTotales();
    }
}
