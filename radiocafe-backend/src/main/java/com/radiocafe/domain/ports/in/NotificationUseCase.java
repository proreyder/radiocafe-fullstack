package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.model.Operacion.*;
public interface NotificationUseCase {
 Bandeja listar(Long usuario); void leer(Long id,Long usuario); void leerTodas(Long usuario);
 void enviar(Long usuario,Long actor,String tipo,String titulo,String mensaje,String enlace);
 void enviarRol(Rol rol,Long actor,String tipo,String titulo,String mensaje,String enlace);
}
