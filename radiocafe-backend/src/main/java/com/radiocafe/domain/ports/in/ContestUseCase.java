package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.IlustracionKappu;
import java.util.List;
public interface ContestUseCase {
 com.radiocafe.domain.model.ConcursoEstado estado();
 com.radiocafe.domain.model.ConcursoEstado configurar(java.time.LocalDateTime inicio,java.time.LocalDateTime fin);
 com.radiocafe.domain.model.ConcursoEstado cerrar();
 boolean yaVoto(Long usuario);
 void verificarAbierto();
 IlustracionKappu inscribir(Long usuario,String titulo,String imagen,String nombre,String descripcion);
 List<IlustracionKappu> listar();
 IlustracionKappu participar(Long usuario,String titulo,String imagen);
 IlustracionKappu votar(Long usuario,Long ilustracion);
}
