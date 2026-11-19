package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import java.util.List;
@io.swagger.v3.oas.annotations.tags.Tag(name="Pedidos",description="Precios del servidor, stock transaccional y estados según rol. El cliente solo consulta sus pedidos.")
@RestController @RequestMapping("/api/v1/orders")
public class OrderController {
 private final CreateOrderUseCase create;private final ManageOrderStateUseCase manage;private final OrderQueryUseCase orders;private final CurrentUser current;
 public OrderController(CreateOrderUseCase create,ManageOrderStateUseCase manage,OrderQueryUseCase orders,CurrentUser current) {this.create=create;this.manage=manage;this.orders=orders;this.current=current;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('CLIENTE','ADMINISTRADOR')")
 public Pedido create(@Valid @RequestBody ApiDtos.OrderInput input,Authentication auth) {return create.crearPedido(input.toDomain(current.get(auth).idUsuario()));}
 @GetMapping public List<Pedido> list(Authentication auth) {var u=current.get(auth);return orders.listar(u.idUsuario(),u.rol()!=Rol.CLIENTE);}
 @GetMapping("/{id}") public Pedido get(@PathVariable Long id,Authentication auth) {
  var p=orders.buscar(id);
  var u=current.get(auth);if(u.rol()==Rol.CLIENTE&&!u.idUsuario().equals(p.getIdUsuario())) throw new AccessDeniedException("Este pedido pertenece a otro cliente.");return p;
 }
 @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BARISTA','REPARTIDOR')")
 public Pedido status(@PathVariable Long id,@Valid @RequestBody ApiDtos.StatusInput input,Authentication auth) {
  var rol=current.get(auth).rol();var next=input.nuevoEstado();
  if(rol==Rol.REPARTIDOR && next!=EstadoPedido.EN_CAMINO && next!=EstadoPedido.ENTREGADO) throw new AccessDeniedException("El repartidor solo gestiona entregas.");
  if(rol==Rol.BARISTA && next==EstadoPedido.EN_CAMINO) throw new AccessDeniedException("La salida de delivery corresponde al repartidor.");
  if(rol==Rol.BARISTA && next==EstadoPedido.ENTREGADO && orders.buscar(id).getTipoEntrega()==TipoEntrega.DELIVERY) throw new AccessDeniedException("El repartidor confirma el delivery.");
  return manage.cambiarEstadoPedido(id,next,current.get(auth).idUsuario());
 }
}
