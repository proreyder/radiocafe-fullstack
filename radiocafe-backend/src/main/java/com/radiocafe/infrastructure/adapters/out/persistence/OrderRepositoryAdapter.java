package com.radiocafe.infrastructure.adapters.out.persistence;

import com.radiocafe.domain.model.EstadoPedido;
import com.radiocafe.domain.model.Pedido;
import com.radiocafe.domain.ports.out.OrderRepositoryPort;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.DetallePedidoEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.PedidoEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.SpringDataOrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository repository;

    public OrderRepositoryAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    public Optional<Pedido> bloquearPorId(Long id) { return repository.bloquear(id).map(this::toDomain); }

    @Override
    public Pedido guardar(Pedido pedido) {
        PedidoEntity entity = toEntity(pedido);
        PedidoEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Pedido> buscarPorId(Long idPedido) {
        return repository.findById(idPedido).map(this::toDomain);
    }

    @Override
    public List<Pedido> buscarPorUsuario(Long idUsuario) {
        return repository.findByIdUsuario(idUsuario).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return repository.findByEstadoPedido(estado).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Pedido> obtenerTodos() {
        return repository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    // Mapeadores Manuales (Entity <-> Domain)
    private PedidoEntity toEntity(Pedido domain) {
        PedidoEntity entity = new PedidoEntity();
        entity.setIdPedido(domain.getIdPedido());
        entity.setVersion(domain.getVersion());
        entity.setStockDescontado(domain.isStockDescontado());
        entity.setFechaCreacion(domain.getFechaCreacion());
        entity.setIdUsuario(domain.getIdUsuario());
        entity.setEstadoPedido(domain.getEstadoPedido());
        entity.setTipoEntrega(domain.getTipoEntrega());
        entity.setDireccionEntrega(domain.getDireccionEntrega());
        entity.setLatitud(domain.getLatitud());
        entity.setLongitud(domain.getLongitud());
        entity.setSubtotal(domain.getSubtotal());
        entity.setCostoEnvio(domain.getCostoEnvio());
        entity.setTotal(domain.getTotal());

        if (domain.getDetalles() != null) {
            List<DetallePedidoEntity> detalles = domain.getDetalles().stream().map(d -> {
                DetallePedidoEntity de = new DetallePedidoEntity();
                de.setIdDetalle(d.getIdDetalle());
                de.setIdProducto(d.getIdProducto());
                de.setNombreProducto(d.getNombreProducto());
                de.setPersonalizacionDescripcion(d.getPersonalizacionDescripcion());
                de.setCantidad(d.getCantidad());
                de.setPrecioUnitario(d.getPrecioUnitario());
                de.setSubtotalLinea(d.getSubtotalLinea());
                return de;
            }).collect(Collectors.toList());
            entity.setDetalles(detalles);
        }
        return entity;
    }

    private Pedido toDomain(PedidoEntity entity) {
        Pedido domain = new Pedido();
        domain.setIdPedido(entity.getIdPedido());
        domain.setVersion(entity.getVersion());
        domain.setStockDescontado(entity.isStockDescontado());
        domain.setIdUsuario(entity.getIdUsuario());
        domain.setEstadoPedido(entity.getEstadoPedido());
        domain.setTipoEntrega(entity.getTipoEntrega());
        domain.setDireccionEntrega(entity.getDireccionEntrega());
        domain.setLatitud(entity.getLatitud());
        domain.setLongitud(entity.getLongitud());
        domain.setFechaCreacion(entity.getFechaCreacion());
        domain.setDetalles(entity.getDetalles().stream().map(d -> {
            var detalle = new com.radiocafe.domain.model.DetallePedido(d.getIdProducto(), d.getNombreProducto(), d.getCantidad(), d.getPrecioUnitario());
            detalle.setIdDetalle(d.getIdDetalle());
            detalle.setPersonalizacionDescripcion(d.getPersonalizacionDescripcion());
            return detalle;
        }).collect(Collectors.toList()));
        return domain;
    }
}