package com.radiocafe.infrastructure.adapters.out.persistence;
import com.radiocafe.domain.ports.out.GuestOrderAccessPort;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.GuestOrderAccessEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.GuestOrderAccessRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component public class GuestOrderAccessAdapter implements GuestOrderAccessPort {
 private final GuestOrderAccessRepository repository;
 public GuestOrderAccessAdapter(GuestOrderAccessRepository repository){this.repository=repository;}
 public void guardar(Long pedido,String hash){var e=new GuestOrderAccessEntity();e.setIdPedido(pedido);e.setTokenHash(hash);repository.save(e);}
 public Optional<String> buscarHash(Long pedido){return repository.findById(pedido).map(GuestOrderAccessEntity::getTokenHash);}
}
