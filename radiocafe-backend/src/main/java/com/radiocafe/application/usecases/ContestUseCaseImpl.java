package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.ContestUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class ContestUseCaseImpl implements ContestUseCase {
 private final IllustrationRepositoryPort illustrations; private final ContestRepositoryPort contest;
 public ContestUseCaseImpl(IllustrationRepositoryPort illustrations,ContestRepositoryPort contest) {this.illustrations=illustrations;this.contest=contest;}
 public List<IlustracionKappu> listar() {return illustrations.obtenerTodas();}
 @Transactional
 public IlustracionKappu participar(Long usuario,String titulo,String imagen) {
  contest.bloquear(); if(contest.tieneGanador()) throw BusinessException.conflicto("El concurso ya está cerrado.");
  var i=new IlustracionKappu(); i.setIdUsuario(usuario);i.setTituloObra(titulo.strip());i.setImagenUrl(imagen);
  return illustrations.guardar(i);
 }
 @Transactional
 public IlustracionKappu votar(Long usuario,Long id) {
  contest.bloquear(); if(contest.tieneGanador()) throw BusinessException.conflicto("La votación ya está cerrada.");
  if(contest.yaVoto(usuario)) throw BusinessException.conflicto("Solo se permite un voto por cuenta en este concurso.");
  var i=illustrations.buscarPorId(id).orElseThrow(()->BusinessException.noEncontrado("Ilustración no encontrada."));
  contest.votar(usuario,id);i.setVotosCount(i.getVotosCount()+1);return illustrations.guardar(i);
 }
}
