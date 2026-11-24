package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.Producto;
import java.util.List;
public interface CatalogUseCase {
 Producto guardarConStock(Long id,Producto producto,Integer stock,Integer stockAnterior,Long actor);
 List<Producto> listar(); Producto guardar(Long id,Producto producto); void eliminar(Long id);
}
