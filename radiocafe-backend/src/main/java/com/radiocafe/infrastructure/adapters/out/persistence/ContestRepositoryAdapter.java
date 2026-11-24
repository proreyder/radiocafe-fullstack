package com.radiocafe.infrastructure.adapters.out.persistence;
import com.radiocafe.domain.ports.out.ContestRepositoryPort;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.*;
import org.springframework.stereotype.Component;
@Component
public class ContestRepositoryAdapter implements ContestRepositoryPort {
 private final ConcursoRepository contest; private final VotoRepository votes;
 public ContestRepositoryAdapter(ConcursoRepository contest,VotoRepository votes) {this.contest=contest;this.votes=votes;}
 public com.radiocafe.domain.model.ConcursoEstado estado() {var c=contest.findById(1L).orElseThrow();return new com.radiocafe.domain.model.ConcursoEstado(c.getInicio(),c.getFin(),c.isCerrado(),c.getIdGanador(),null,java.util.List.of());}
 public void configurar(java.time.LocalDateTime inicio,java.time.LocalDateTime fin,boolean cerrado) {var c=contest.findById(1L).orElseThrow();c.setInicio(inicio);c.setFin(fin);c.setCerrado(cerrado);contest.save(c);}
 public void bloquear() {contest.bloquear().orElseThrow(()->new IllegalStateException("Concurso no inicializado."));}
 public boolean tieneGanador() {return contest.findById(1L).orElseThrow().getIdGanador()!=null;}
 public void establecerGanador(Long id) {var c=contest.findById(1L).orElseThrow();c.setIdGanador(id);contest.save(c);}
 public boolean yaVoto(Long usuario) {return votes.existsByIdUsuario(usuario);}
 public void votar(Long usuario,Long ilustracion) {var v=new VotoEntity();v.setIdUsuario(usuario);v.setIdIlustracion(ilustracion);votes.save(v);}
}
