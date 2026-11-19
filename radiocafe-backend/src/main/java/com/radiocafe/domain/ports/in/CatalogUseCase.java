package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.Producto;
import java.util.List;
public interface CatalogUseCase {
 List<Producto> listar(); Producto guardar(Long id,Producto producto); void eliminar(Long id);
}
