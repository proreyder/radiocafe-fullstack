package com.radiocafe.application.usecases;

import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.CreateOrderUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
    private final OrderRepositoryPort orders;
    private final ProductRepositoryPort products;
    private final InventoryRepositoryPort inventory;
    private final com.radiocafe.domain.ports.in.NotificationUseCase notifications;
    private final com.radiocafe.domain.ports.in.CommerceUseCase commerce;
    public CreateOrderUseCaseImpl(OrderRepositoryPort orders,ProductRepositoryPort products,InventoryRepositoryPort inventory,com.radiocafe.domain.ports.in.CommerceUseCase commerce,com.radiocafe.domain.ports.in.NotificationUseCase notifications) {
        this.orders=orders;this.products=products;this.inventory=inventory;this.commerce=commerce;this.notifications=notifications;
    }
    @Transactional
    public Pedido crearPedido(Pedido pedido) {
        if(pedido.getDetalles()==null || pedido.getDetalles().isEmpty()) throw new IllegalArgumentException("Un pedido debe contener productos.");
        if(pedido.getTipoEntrega()==null) throw new IllegalArgumentException("Indica el tipo de entrega.");
        if(pedido.getTipoEntrega()==TipoEntrega.DELIVERY && (pedido.getDireccionEntrega()==null || pedido.getDireccionEntrega().isBlank())) throw new IllegalArgumentException("El delivery requiere dirección.");
        // Aggregate variants of the same product; acquire locks in ID order to avoid deadlocks.
        Map<Long,Integer> cantidades=new TreeMap<>();
        for(var d:pedido.getDetalles()) {
            if(d.getIdProducto()==null || d.getCantidad()==null || d.getCantidad()<1 || d.getCantidad()>99) throw new IllegalArgumentException("La cantidad debe estar entre 1 y 99.");
            cantidades.merge(d.getIdProducto(),d.getCantidad(),Integer::sum);
        }
        Map<Long,Producto> catalogo=new TreeMap<>();
        for(var entry:cantidades.entrySet()) {
            var p=products.bloquearPorId(entry.getKey()).filter(x->Boolean.TRUE.equals(x.disponible())).orElseThrow(()->BusinessException.noEncontrado("Producto no disponible."));
            if(p.stockDisponible()<entry.getValue()) throw BusinessException.conflicto("Stock insuficiente para "+p.nombre()+". Disponible: "+p.stockDisponible()+".");
            catalogo.put(entry.getKey(),p);
        }
        for(var d:pedido.getDetalles()) {
            var p=catalogo.get(d.getIdProducto());d.setNombreProducto(p.nombre());
            if(d.getPersonalizacion()!=null) {
                var configurado=d.getPersonalizacion().aplicar(p);
                d.setPrecioUnitario(configurado.precio());d.setPersonalizacionDescripcion(configurado.descripcion());
            } else {d.setPrecioUnitario(p.precio());d.setPersonalizacionDescripcion(null);}
        }
        for(var entry:cantidades.entrySet()) {
            var p=catalogo.get(entry.getKey());products.guardar(p.conStock(p.stockDisponible()-entry.getValue()));
        }
        pedido.setIdPedido(null);pedido.setEstadoPedido(EstadoPedido.PENDIENTE);pedido.setStockDescontado(true);pedido.recalcularTotales();
        var descuento=commerce.descuento(pedido.getSubtotal(),pedido.getCodigoCupon());
        pedido.aplicarDescuento(descuento.importe(),descuento.codigo(),descuento.titulo());
        var guardado=orders.guardar(pedido);
        for(var entry:cantidades.entrySet()) {
            var p=catalogo.get(entry.getKey());
            inventory.registrar(new MovimientoInventario(null,p.idProducto(),p.nombre(),-entry.getValue(),p.stockDisponible()-entry.getValue(),"VENTA","Confirmación de pedido",pedido.getIdUsuario(),guardado.getIdPedido(),LocalDateTime.now()));
        }
        notifications.enviarRol(Rol.BARISTA,pedido.getIdUsuario(),"PEDIDO","Nuevo pedido #"+guardado.getIdPedido(),"Hay un nuevo pedido pendiente de preparación.","/admin?seccion=orders");
        if(pedido.getIdUsuario()==null) notifications.enviarRol(Rol.ADMINISTRADOR,null,"PEDIDO","Pedido de invitado #"+guardado.getIdPedido(),"Nuevo pedido sin cuenta. Los datos de contacto están en el panel de pedidos.","/admin?seccion=orders");
        return guardado;
    }
}
