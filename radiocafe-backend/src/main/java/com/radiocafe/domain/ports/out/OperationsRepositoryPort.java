package com.radiocafe.domain.ports.out;
import com.radiocafe.domain.model.Operacion.*;
import java.util.*;
public interface OperationsRepositoryPort {
 Notificacion guardar(Notificacion n); Optional<Notificacion> notificacion(Long id); Bandeja bandeja(Long usuario); void leerTodas(Long usuario);
 Presencia presencia(Long usuario); Presencia guardar(Presencia p);
}
