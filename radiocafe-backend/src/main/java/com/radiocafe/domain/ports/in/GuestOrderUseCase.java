package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.Pedido;
public interface GuestOrderUseCase {
 record Confirmacion(Pedido pedido,String token) {}
 Confirmacion crear(Pedido pedido);
 Pedido seguir(Long id,String token);
}
