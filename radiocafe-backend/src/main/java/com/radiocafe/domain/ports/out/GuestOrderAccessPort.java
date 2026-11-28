package com.radiocafe.domain.ports.out;
import java.util.Optional;
public interface GuestOrderAccessPort {
 void guardar(Long pedido,String hash);
 Optional<String> buscarHash(Long pedido);
}
