package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.SelectWinnerUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class SelectWinnerUseCaseImpl implements SelectWinnerUseCase {
 private final IllustrationRepositoryPort illustrations;private final ContestRepositoryPort contest;private final com.radiocafe.domain.ports.in.ContestUseCase lifecycle;private final com.radiocafe.domain.ports.in.NotificationUseCase notifications;
 public SelectWinnerUseCaseImpl(IllustrationRepositoryPort illustrations,ContestRepositoryPort contest,com.radiocafe.domain.ports.in.ContestUseCase lifecycle,com.radiocafe.domain.ports.in.NotificationUseCase notifications) {this.illustrations=illustrations;this.contest=contest;this.lifecycle=lifecycle;this.notifications=notifications;}
 @Transactional
 public IlustracionKappu seleccionarGanador(Long id) {
  contest.bloquear();if(contest.tieneGanador()) throw BusinessException.conflicto("Ya existe un ganador. No puede volver a seleccionarse.");
  var estado=lifecycle.estado();if(!estado.cerrado()) throw BusinessException.conflicto("Cierra primero las inscripciones y la votación para elegir entre los tres finalistas.");
  if(!estado.finalistas().contains(id)) throw BusinessException.conflicto("El ganador debe ser uno de los tres finalistas por likes.");
  var i=illustrations.buscarPorId(id).orElseThrow(()->BusinessException.noEncontrado("Ilustración no encontrada."));
  i.setEsFinalista(true);i.setEsGanador(true);i.setEstadoPremio(EstadoPremio.PENDIENTE_RECOJO);
  i.setCodigoPremio("KAPPU-WIN-"+UUID.randomUUID());contest.establecerGanador(id);
  var result=illustrations.guardar(i);notifications.enviar(i.getIdUsuario(),null,"KAPPU","¡Ganaste el concurso Kappu!","Tu ilustración fue elegida ganadora. Abre el concurso para ver el QR de tu premio.","/catalogo?seccion=kappu");return result;
 }
}
