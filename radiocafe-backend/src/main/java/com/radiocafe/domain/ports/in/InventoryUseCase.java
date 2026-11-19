package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.*;
import java.util.List;
public interface InventoryUseCase {
    List<Producto> listar();
    Producto ajustar(Long producto,int cantidad,String motivo,Long actor);
    List<MovimientoInventario> movimientos(Long producto);
}
