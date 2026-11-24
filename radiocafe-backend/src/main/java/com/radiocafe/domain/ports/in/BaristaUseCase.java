package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.model.Operacion.*;
import java.util.List;
public interface BaristaUseCase {
 List<Barista> listar(); Barista estado(Usuario usuario); Barista disponibilidad(Usuario usuario,boolean disponible);
 Pedido estimar(Long pedido,int minutos,Usuario barista); void faltaInsumos(Usuario barista,String insumo,int cantidad,String unidad,String detalle);
}
