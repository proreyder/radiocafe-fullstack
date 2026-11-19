package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.SelectWinnerUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class SelectWinnerUseCaseImpl implements SelectWinnerUseCase {
 private final IllustrationRepositoryPort illustrations;private final ContestRepositoryPort contest;
 public SelectWinnerUseCaseImpl(IllustrationRepositoryPort illustrations,ContestRepositoryPort contest) {this.illustrations=illustrations;this.contest=contest;}
 @Transactional
 public IlustracionKappu seleccionarGanador(Long id) {
  contest.bloquear();if(contest.tieneGanador()) throw BusinessException.conflicto("Ya existe un ganador. No puede volver a seleccionarse.");
  var i=illustrations.buscarPorId(id).orElseThrow(()->BusinessException.noEncontrado("Ilustración no encontrada."));
  i.setEsFinalista(true);i.setEsGanador(true);i.setEstadoPremio(EstadoPremio.PENDIENTE_RECOJO);
  i.setCodigoPremio("KAPPU-WIN-"+UUID.randomUUID());contest.establecerGanador(id);
  return illustrations.guardar(i);
 }
}
