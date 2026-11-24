package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.Comercio.*;
import com.radiocafe.domain.model.Usuario;
import java.math.BigDecimal;
import java.util.List;
public interface CommerceUseCase {
 List<Campana> campanas(boolean todas); Campana guardarCampana(Long id,Campana c); Descuento descuento(BigDecimal subtotal,String codigo);
 List<Opinion> opiniones(Long producto,boolean todas); Opinion comentar(Long producto,int estrellas,String comentario,Usuario usuario); Opinion moderar(Long id,boolean visible);
 List<Consulta> consultas(Usuario usuario); Consulta consultar(String asunto,String mensaje,Usuario usuario); Consulta responder(Long id,String respuesta);
 Tienda tienda(); Tienda guardarTienda(Tienda t); List<Recomendacion> recomendar(Usuario usuario);
}
