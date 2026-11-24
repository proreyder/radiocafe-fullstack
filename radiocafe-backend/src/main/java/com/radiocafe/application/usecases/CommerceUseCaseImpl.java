package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.model.Comercio.*;
import com.radiocafe.domain.ports.in.CommerceUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
@Service @Transactional(readOnly=true)
public class CommerceUseCaseImpl implements CommerceUseCase {
 private final CommerceRepositoryPort store; private final ProductRepositoryPort products; private final OrderRepositoryPort orders;
 public CommerceUseCaseImpl(CommerceRepositoryPort store,ProductRepositoryPort products,OrderRepositoryPort orders) {this.store=store;this.products=products;this.orders=orders;}
 public List<Campana> campanas(boolean todas) {return store.campanas().stream().filter(c->todas || c.vigente(LocalDateTime.now())).toList();}
 @Transactional public Campana guardarCampana(Long id,Campana c) {
  if(id!=null) store.campana(id).orElseThrow(()->BusinessException.noEncontrado("Campaña no encontrada."));
  if(!c.fin().isAfter(c.inicio())) throw invalid("La fecha final debe ser posterior al inicio.");
  String code=normalizar(c.codigo());
  if(code!=null && !code.matches("[A-Z0-9_-]{3,30}")) throw invalid("El cupón debe tener de 3 a 30 letras, números, guiones o guiones bajos.");
  return store.guardar(new Campana(id,c.titulo().strip(),c.mensaje().strip(),code,c.porcentaje(),c.minimo(),c.inicio(),c.fin(),c.activa()));
 }
 public Descuento descuento(BigDecimal subtotal,String codigo) {
  if(subtotal==null || subtotal.signum()<0) throw invalid("Subtotal inválido.");
  String code=normalizar(codigo); var now=LocalDateTime.now(); Campana selected=null;
  if(code!=null) {
   selected=store.campanas().stream().filter(c->code.equals(c.codigo())).findFirst().orElseThrow(()->BusinessException.conflicto("El cupón no existe."));
   if(!selected.vigente(now)) throw BusinessException.conflicto("El cupón no está vigente.");
   if(subtotal.compareTo(selected.minimo())<0) throw BusinessException.conflicto("El cupón requiere una compra mínima de "+selected.minimo()+" USD.");
  } else selected=store.campanas().stream().filter(c->c.codigo()==null && c.vigente(now) && subtotal.compareTo(c.minimo())>=0).max(Comparator.comparing(Campana::porcentaje)).orElse(null);
  if(selected==null) return new Descuento(BigDecimal.ZERO,null,null);
  BigDecimal amount=subtotal.multiply(selected.porcentaje()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP).min(subtotal);
  return new Descuento(amount,selected.codigo(),selected.titulo());
 }
 public List<Opinion> opiniones(Long producto,boolean todas) {return store.opiniones(producto,todas);}
 @Transactional public Opinion comentar(Long producto,int estrellas,String comentario,Usuario usuario) {
  products.buscarPorId(producto).filter(p->Boolean.TRUE.equals(p.disponible())).orElseThrow(()->BusinessException.noEncontrado("Producto no disponible."));
  if(estrellas<1 || estrellas>5) throw invalid("La valoración debe estar entre 1 y 5.");
  if(store.yaOpino(producto,usuario.idUsuario())) throw BusinessException.conflicto("Ya dejaste una opinión sobre este producto.");
  return store.guardar(new Opinion(null,producto,usuario.idUsuario(),usuario.nombre(),estrellas,comentario.strip(),true,LocalDateTime.now()));
 }
 @Transactional public Opinion moderar(Long id,boolean visible) {
  var o=store.opinion(id).orElseThrow(()->BusinessException.noEncontrado("Comentario no encontrado."));
  return store.guardar(new Opinion(o.id(),o.producto(),o.usuario(),o.autor(),o.estrellas(),o.comentario(),visible,o.fecha()));
 }
 public List<Consulta> consultas(Usuario u) {return store.consultas(u.rol()==Rol.ADMINISTRADOR?null:u.idUsuario());}
 @Transactional public Consulta consultar(String asunto,String mensaje,Usuario u) {return store.guardar(new Consulta(null,u.idUsuario(),u.nombre(),asunto.strip(),mensaje.strip(),null,"ABIERTA",LocalDateTime.now(),null));}
 @Transactional public Consulta responder(Long id,String respuesta) {
  var c=store.consulta(id).orElseThrow(()->BusinessException.noEncontrado("Consulta no encontrada."));
  return store.guardar(new Consulta(c.id(),c.usuario(),c.autor(),c.asunto(),c.mensaje(),respuesta.strip(),"RESPONDIDA",c.fecha(),LocalDateTime.now()));
 }
 public Tienda tienda() {return store.tienda();}
 @Transactional public Tienda guardarTienda(Tienda t) {return store.guardar(t);}
 public List<Recomendacion> recomendar(Usuario u) {
  var catalog=products.obtenerTodosDisponibles(); var byId=new HashMap<Long,Producto>();catalog.forEach(p->byId.put(p.idProducto(),p));
  var preferencias=new HashMap<String,Integer>();var populares=new HashMap<Long,Integer>();
  for(var order:orders.obtenerTodos()) {
   if(order.getEstadoPedido()==EstadoPedido.CANCELLED) continue;
   for(var d:order.getDetalles()) {
    populares.merge(d.getIdProducto(),d.getCantidad(),Integer::sum);
    var p=byId.get(d.getIdProducto());if(order.getIdUsuario().equals(u.idUsuario()) && p!=null) preferencias.merge(p.categoria(),d.getCantidad(),Integer::sum);
   }
  }
  return catalog.stream().filter(p->p.stockDisponible()>0).sorted(Comparator.<Producto>comparingInt(p->preferencias.getOrDefault(p.categoria(),0)*10+populares.getOrDefault(p.idProducto(),0)).reversed().thenComparing(Producto::idProducto)).limit(4)
   .map(p->new Recomendacion(p,preferencias.containsKey(p.categoria())?"Por tus pedidos anteriores":populares.containsKey(p.idProducto())?"Popular en RadioCafé":"Descubre un favorito del menú")).toList();
 }
 private String normalizar(String codigo) {return codigo==null||codigo.isBlank()?null:codigo.strip().toUpperCase(Locale.ROOT);}
 private BusinessException invalid(String m) {return new BusinessException(BusinessException.Tipo.INVALIDO,m);}
}
