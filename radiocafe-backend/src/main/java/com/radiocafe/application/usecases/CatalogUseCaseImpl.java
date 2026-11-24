package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.CatalogUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class CatalogUseCaseImpl implements CatalogUseCase {
 private final ProductRepositoryPort products;private final InventoryRepositoryPort inventory;
 public CatalogUseCaseImpl(ProductRepositoryPort products,InventoryRepositoryPort inventory) {this.products=products;this.inventory=inventory;}
 public List<Producto> listar() {return products.obtenerTodosDisponibles();}
 @Transactional public Producto guardar(Long id,Producto p) {return guardarConStock(id,p,null,null,null);}
 @Transactional public Producto guardarConStock(Long id,Producto p,Integer solicitado,Integer anterior,Long actor) {
  int actual=id==null?0:products.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Producto no encontrado.")).stockDisponible();
  int saldo=actual;
  if(solicitado!=null && (id==null || anterior==null || !solicitado.equals(anterior))) {
   if(solicitado<0 || solicitado>999999) throw new IllegalArgumentException("Stock entre 0 y 999999.");
   if(id!=null && anterior!=null && anterior!=actual) throw BusinessException.conflicto("El stock cambió mientras editabas. Actualiza el catálogo y vuelve a intentarlo.");
   saldo=solicitado;
  }
  var result=products.guardar(new Producto(id,p.nombre().strip(),p.descripcion(),p.categoria(),p.precio(),p.imagenUrl(),true,Boolean.TRUE.equals(p.personalizable()),saldo));
  if(saldo!=actual) inventory.registrar(new MovimientoInventario(null,result.idProducto(),result.nombre(),saldo-actual,saldo,id==null?"INICIAL":"AJUSTE",id==null?"Stock inicial desde catálogo":"Edición de stock desde catálogo",actor,null,LocalDateTime.now()));
  return result;
 }
 @Transactional public void eliminar(Long id) {
  var p=products.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Producto no encontrado."));
  products.guardar(new Producto(id,p.nombre(),p.descripcion(),p.categoria(),p.precio(),p.imagenUrl(),false,p.personalizable(),p.stockDisponible()));
 }
}
