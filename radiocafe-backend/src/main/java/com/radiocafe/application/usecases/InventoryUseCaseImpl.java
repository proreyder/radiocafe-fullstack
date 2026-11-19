package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.InventoryUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class InventoryUseCaseImpl implements InventoryUseCase {
    private final ProductRepositoryPort products;
    private final InventoryRepositoryPort inventory;
    public InventoryUseCaseImpl(ProductRepositoryPort products,InventoryRepositoryPort inventory) {this.products=products;this.inventory=inventory;}
    @Transactional(readOnly=true) public List<Producto> listar() {return products.obtenerTodos();}
    @Transactional(readOnly=true) public List<MovimientoInventario> movimientos(Long producto) {return inventory.ultimos(producto);}
    @Transactional public Producto ajustar(Long id,int cantidad,String motivo,Long actor) {
        if(cantidad==0 || cantidad < -100000 || cantidad > 100000) throw new IllegalArgumentException("El ajuste debe ser distinto de cero y estar entre -100000 y 100000.");
        if(motivo==null || motivo.strip().length()<5 || motivo.length()>200) throw new IllegalArgumentException("Describe el motivo del ajuste (5 a 200 caracteres).");
        var p=products.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Producto no encontrado."));
        int saldo=p.stockDisponible()+cantidad;
        if(saldo<0) throw BusinessException.conflicto("El ajuste dejaría el inventario negativo.");
        var guardado=products.guardar(p.conStock(saldo));
        inventory.registrar(new MovimientoInventario(null,id,p.nombre(),cantidad,saldo,"AJUSTE",motivo.strip(),actor,null,LocalDateTime.now()));
        return guardado;
    }
}
