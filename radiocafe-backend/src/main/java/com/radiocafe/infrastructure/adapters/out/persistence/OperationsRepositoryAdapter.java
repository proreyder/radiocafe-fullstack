package com.radiocafe.infrastructure.adapters.out.persistence;
import com.radiocafe.domain.model.Operacion.*;import com.radiocafe.domain.ports.out.OperationsRepositoryPort;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.*;import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import org.springframework.stereotype.Component;import java.util.*;
@Component
public class OperationsRepositoryAdapter implements OperationsRepositoryPort {
 private final NotificacionRepository notifications;private final BaristaPresenciaRepository presence;
 public OperationsRepositoryAdapter(NotificacionRepository notifications,BaristaPresenciaRepository presence) {this.notifications=notifications;this.presence=presence;}
 private Notificacion view(NotificacionEntity n) {return new Notificacion(n.getId(),n.getUsuario(),n.getEmisor(),n.getAutor(),n.getTipo(),n.getTitulo(),n.getMensaje(),n.getEnlace(),n.isLeida(),n.getFecha());}
 public Notificacion guardar(Notificacion n) {return view(notifications.save(new NotificacionEntity(n.id(),n.usuario(),n.emisor(),n.autor(),n.tipo(),n.titulo(),n.mensaje(),n.enlace(),n.leida(),n.fecha())));}
 public Optional<Notificacion> notificacion(Long id) {return notifications.findById(id).map(this::view);}
 public Bandeja bandeja(Long usuario) {return new Bandeja(notifications.findTop100ByUsuarioOrderByFechaDescIdDesc(usuario).stream().map(this::view).toList(),notifications.countByUsuarioAndLeidaFalse(usuario));}
 public void leerTodas(Long usuario) {notifications.leerTodas(usuario);}
 public Presencia presencia(Long usuario) {return presence.findById(usuario).map(p->new Presencia(p.getUsuario(),p.isDisponible(),p.getActualizado())).orElse(new Presencia(usuario,false,null));}
 public Presencia guardar(Presencia p) {var e=presence.save(new BaristaPresenciaEntity(p.usuario(),p.disponible(),p.actualizado()));return new Presencia(e.getUsuario(),e.isDisponible(),e.getActualizado());}
}
