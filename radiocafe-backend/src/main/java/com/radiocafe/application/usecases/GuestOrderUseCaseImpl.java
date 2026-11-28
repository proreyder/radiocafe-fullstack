package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.*;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class GuestOrderUseCaseImpl implements GuestOrderUseCase {
 private final CreateOrderUseCase create;private final GuestOrderAccessPort access;private final OrderRepositoryPort orders;
 private final SecureRandom random=new SecureRandom();
 public GuestOrderUseCaseImpl(CreateOrderUseCase create,GuestOrderAccessPort access,OrderRepositoryPort orders){this.create=create;this.access=access;this.orders=orders;}
 private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 @Transactional public Confirmacion crear(Pedido pedido){
  var c=pedido.getContacto();
  if(c==null||c.nombre()==null||c.nombre().isBlank()||c.telefono()==null||c.telefono().isBlank()||c.correo()==null||c.correo().isBlank())throw new IllegalArgumentException("Completa los datos de contacto.");
  pedido.setIdUsuario(null);
  var saved=create.crearPedido(pedido);byte[] bytes=new byte[32];random.nextBytes(bytes);
  String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);access.guardar(saved.getIdPedido(),hash(token));return new Confirmacion(saved,token);
 }
 @Transactional(readOnly=true) public Pedido seguir(Long id,String token){
  if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw BusinessException.noEncontrado("Enlace de seguimiento no válido.");
  String expected=access.buscarHash(id).orElse("");
  if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),hash(token).getBytes(StandardCharsets.UTF_8)))throw BusinessException.noEncontrado("Enlace de seguimiento no válido.");
  return orders.buscarPorId(id).filter(p->p.getIdUsuario()==null).orElseThrow(()->BusinessException.noEncontrado("Pedido no encontrado."));
 }
}
