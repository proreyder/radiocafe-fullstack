package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.RedeemPrizeUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RedeemPrizeUseCaseImpl implements RedeemPrizeUseCase {
 private final IllustrationRepositoryPort illustrations;private final ContestRepositoryPort contest;
 public RedeemPrizeUseCaseImpl(IllustrationRepositoryPort illustrations,ContestRepositoryPort contest) {this.illustrations=illustrations;this.contest=contest;}
 @Transactional
 public IlustracionKappu canjearPremioPorCodigo(String codigo) {
  contest.bloquear();var i=illustrations.buscarPorCodigoPremio(codigo).orElseThrow(()->BusinessException.noEncontrado("Código de premio inválido."));
  if(i.getEstadoPremio()==EstadoPremio.RECLAMADO) throw BusinessException.conflicto("El premio ya fue canjeado.");
  i.canjearPremio();return illustrations.guardar(i);
 }
}
