package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.model.Operacion.*;
import com.radiocafe.domain.ports.in.*;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
@Service @Transactional
public class BaristaUseCaseImpl implements BaristaUseCase {
 private final OperationsRepositoryPort store;private final UserRepositoryPort users;private final OrderRepositoryPort orders;private final NotificationUseCase notifications;private final Clock clock;
 public BaristaUseCaseImpl(OperationsRepositoryPort store,UserRepositoryPort users,OrderRepositoryPort orders,NotificationUseCase notifications,Clock clock) {this.store=store;this.users=users;this.orders=orders;this.notifications=notifications;this.clock=clock;}
 private boolean enHorario() {var t=LocalTime.now(clock);return !t.isBefore(LocalTime.of(6,0))&&t.isBefore(LocalTime.of(20,30));}
 public Barista estado(Usuario usuario) {var p=store.presencia(usuario.idUsuario());boolean horario=enHorario();boolean turnoActual=p.actualizado()!=null&&p.actualizado().atZone(clock.getZone()).toLocalDate().equals(LocalDate.now(clock));return new Barista(usuario.idUsuario(),usuario.nombre(),usuario.activo()&&p.disponible()&&horario&&turnoActual,horario,p.disponible()&&turnoActual,"06:00–20:30 · Lima",p.actualizado());}
 @Transactional(readOnly=true) public List<Barista> listar() {return users.obtenerTodos().stream().filter(u->u.activo()&&u.rol()==Rol.BARISTA).map(this::estado).toList();}
 public Barista disponibilidad(Usuario usuario,boolean disponible) {
  if(disponible&&!enHorario()) throw BusinessException.conflicto("El horario de atención del barista es de 06:00 a 20:30, hora de Lima.");
  store.guardar(new Presencia(usuario.idUsuario(),disponible,clock.instant()));return estado(usuario);
 }
 public Pedido estimar(Long id,int minutos,Usuario barista) {
  if(minutos<1||minutos>240) throw new IllegalArgumentException("Tiempo entre 1 y 240 minutos.");
  if(barista.rol()==Rol.BARISTA&&!estado(barista).disponible()) throw BusinessException.conflicto("Activa tu disponibilidad dentro del horario para estimar la preparación.");
  var p=orders.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Pedido no encontrado."));
  if(p.getEstadoPedido()!=EstadoPedido.PENDIENTE&&p.getEstadoPedido()!=EstadoPedido.EN_PREPARACION) throw BusinessException.conflicto("Solo se estima un pedido pendiente o en preparación.");
  if(p.getEstadoPedido()==EstadoPedido.PENDIENTE)p.cambiarEstado(EstadoPedido.EN_PREPARACION);
  p.setMinutosPreparacion(minutos);p.setPreparacionEstimada(clock.instant().plusSeconds(minutos*60L));p.setNombreBarista(barista.nombre());
  var result=orders.guardar(p);
  notifications.enviar(p.getIdUsuario(),barista.idUsuario(),"PEDIDO","Tu pedido ya se está preparando",barista.nombre()+" estima "+minutos+" minutos para preparar el pedido #"+id+". Consulta la hora estimada en Mis pedidos.","/catalogo?seccion=orders");
  return result;
 }
 public void faltaInsumos(Usuario barista,String insumo,int cantidad,String unidad,String detalle) {
  notifications.enviarRol(Rol.ADMINISTRADOR,barista.idUsuario(),"INSUMOS","Falta de insumos: "+insumo.strip(),barista.nombre()+" solicita "+cantidad+" "+unidad.strip()+" de "+insumo.strip()+". "+detalle.strip(),"/admin?seccion=inventory");
 }
}
