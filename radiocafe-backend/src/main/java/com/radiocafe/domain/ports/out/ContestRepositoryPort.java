package com.radiocafe.domain.ports.out;
public interface ContestRepositoryPort {
 void bloquear();
 boolean tieneGanador();
 void establecerGanador(Long id);
 boolean yaVoto(Long usuario);
 void votar(Long usuario, Long ilustracion);
}
