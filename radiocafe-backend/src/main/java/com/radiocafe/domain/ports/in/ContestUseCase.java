package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.IlustracionKappu;
import java.util.List;
public interface ContestUseCase {
 List<IlustracionKappu> listar();
 IlustracionKappu participar(Long usuario,String titulo,String imagen);
 IlustracionKappu votar(Long usuario,Long ilustracion);
}
