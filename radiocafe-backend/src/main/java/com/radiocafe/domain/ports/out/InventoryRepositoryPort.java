package com.radiocafe.domain.ports.out;
import com.radiocafe.domain.model.MovimientoInventario;
import java.util.List;
public interface InventoryRepositoryPort {
    void registrar(MovimientoInventario movimiento);
    List<MovimientoInventario> ultimos(Long idProducto);
}
