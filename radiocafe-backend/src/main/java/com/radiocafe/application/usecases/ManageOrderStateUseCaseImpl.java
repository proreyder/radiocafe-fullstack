package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.ManageOrderStateUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.TreeMap;
@Service
public class ManageOrderStateUseCaseImpl implements ManageOrderStateUseCase {
    private final OrderRepositoryPort orders;
    private final ProductRepositoryPort products;
    private final InventoryRepositoryPort inventory;
    private final com.radiocafe.domain.ports.in.NotificationUseCase notifications;
    public ManageOrderStateUseCaseImpl(OrderRepositoryPort orders,ProductRepositoryPort products,InventoryRepositoryPort inventory,com.radiocafe.domain.ports.in.NotificationUseCase notifications) {this.orders=orders;this.products=products;this.inventory=inventory;this.notifications=notifications;}
    @Transactional
    public Pedido cambiarEstadoPedido(Long id,EstadoPedido nuevo,Long actor) {
        var pedido=orders.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Pedido no encontrado."));
        pedido.cambiarEstado(nuevo);
        if(nuevo==EstadoPedido.CANCELLED && pedido.isStockDescontado()) {
            var cantidades=new TreeMap<Long,Integer>();
            pedido.getDetalles().forEach(d->cantidades.merge(d.getIdProducto(),d.getCantidad(),Integer::sum));
            for(var entry:cantidades.entrySet()) {
                var p=products.bloquearPorId(entry.getKey()).orElseThrow(()->BusinessException.noEncontrado("Producto no encontrado."));
                int saldo=p.stockDisponible()+entry.getValue();products.guardar(p.conStock(saldo));
                inventory.registrar(new MovimientoInventario(null,p.idProducto(),p.nombre(),entry.getValue(),saldo,"DEVOLUCION","Cancelación de pedido",actor,id,LocalDateTime.now()));
            }
            pedido.setStockDescontado(false);
        }
        var result=orders.guardar(pedido);
        notifications.enviar(pedido.getIdUsuario(),actor,"PEDIDO","Actualización de tu pedido #"+id,"El pedido cambió a "+nuevo+".","/catalogo?seccion=orders");
        return result;
    }
}
