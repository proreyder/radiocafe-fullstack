package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.model.Operacion.*;
import com.radiocafe.domain.ports.in.NotificationUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
@Service @Transactional
public class NotificationUseCaseImpl implements NotificationUseCase {
 private final OperationsRepositoryPort store;private final UserRepositoryPort users;private final Clock clock;
 public NotificationUseCaseImpl(OperationsRepositoryPort store,UserRepositoryPort users,Clock clock) {this.store=store;this.users=users;this.clock=clock;}
 @Transactional(readOnly=true) public Bandeja listar(Long usuario) {return store.bandeja(usuario);}
 public void leer(Long id,Long usuario) {var n=store.notificacion(id).filter(x->x.usuario().equals(usuario)).orElseThrow(()->BusinessException.noEncontrado("Notificación no encontrada."));store.guardar(new Notificacion(n.id(),n.usuario(),n.emisor(),n.autor(),n.tipo(),n.titulo(),n.mensaje(),n.enlace(),true,n.fecha()));}
 public void leerTodas(Long usuario) {store.leerTodas(usuario);}
 public void enviar(Long usuario,Long actor,String tipo,String titulo,String mensaje,String enlace) {
  String autor=actor==null?"RadioCafé":users.buscarPorId(actor).map(Usuario::nombre).orElse("RadioCafé");
  store.guardar(new Notificacion(null,usuario,actor,autor,tipo,titulo,mensaje,enlace,false,clock.instant()));
 }
 public void enviarRol(Rol rol,Long actor,String tipo,String titulo,String mensaje,String enlace) {users.obtenerTodos().stream().filter(u->u.activo()&&u.rol()==rol).forEach(u->enviar(u.idUsuario(),actor,tipo,titulo,mensaje,enlace));}
}
