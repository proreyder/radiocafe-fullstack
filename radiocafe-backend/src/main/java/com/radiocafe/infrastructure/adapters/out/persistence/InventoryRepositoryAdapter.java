package com.radiocafe.infrastructure.adapters.out.persistence;
import com.radiocafe.domain.model.MovimientoInventario;
import com.radiocafe.domain.ports.out.InventoryRepositoryPort;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.MovimientoInventarioEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.MovimientoInventarioRepository;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class InventoryRepositoryAdapter implements InventoryRepositoryPort {
    private final MovimientoInventarioRepository repository;
    public InventoryRepositoryAdapter(MovimientoInventarioRepository repository) {this.repository=repository;}
    public void registrar(MovimientoInventario m) {
        var e=new MovimientoInventarioEntity();
        e.setIdProducto(m.idProducto());e.setNombreProducto(m.nombreProducto());e.setCantidad(m.cantidad());
        e.setSaldo(m.saldo());e.setTipo(m.tipo());e.setMotivo(m.motivo());e.setIdUsuario(m.idUsuario());
        e.setIdPedido(m.idPedido());e.setFecha(m.fecha());repository.save(e);
    }
    public List<MovimientoInventario> ultimos(Long producto) {
        var list=producto==null?repository.findTop200ByOrderByIdDesc():repository.findTop200ByIdProductoOrderByIdDesc(producto);
        return list.stream().map(e->new MovimientoInventario(e.getId(),e.getIdProducto(),e.getNombreProducto(),e.getCantidad(),e.getSaldo(),e.getTipo(),e.getMotivo(),e.getIdUsuario(),e.getIdPedido(),e.getFecha())).toList();
    }
}
