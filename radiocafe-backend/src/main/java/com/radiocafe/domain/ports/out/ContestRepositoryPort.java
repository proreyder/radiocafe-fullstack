package com.radiocafe.domain.ports.out;
public interface ContestRepositoryPort {
 com.radiocafe.domain.model.ConcursoEstado estado();
 void configurar(java.time.LocalDateTime inicio,java.time.LocalDateTime fin,boolean cerrado);
 void bloquear();
 boolean tieneGanador();
 void establecerGanador(Long id);
 boolean yaVoto(Long usuario);
 void votar(Long usuario, Long ilustracion);
}
