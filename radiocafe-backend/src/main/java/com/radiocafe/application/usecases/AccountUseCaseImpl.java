package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.AccountUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
@Service
public class AccountUseCaseImpl implements AccountUseCase {
 private final UserRepositoryPort users; private final PasswordPort passwords;private final OrderRepositoryPort orders;
 public AccountUseCaseImpl(UserRepositoryPort users,PasswordPort passwords,OrderRepositoryPort orders) { this.users=users; this.passwords=passwords;this.orders=orders; }
 @Transactional
 public Usuario registrar(String nombre,String correo,String password) {
  correo=correo.strip().toLowerCase(Locale.ROOT);
  if(users.existePorCorreo(correo)) throw BusinessException.conflicto("Este correo ya está registrado.");
  return users.guardar(new Usuario(null,nombre.strip(),correo,passwords.codificar(password),Rol.CLIENTE,null));
 }

 private Usuario cuenta(Long id) {var u=users.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Cuenta no encontrada."));if(!u.activo()) throw new BusinessException(BusinessException.Tipo.PROHIBIDO,"Cuenta desactivada.");return u;}
 @Transactional public Usuario actualizarPerfil(Long id,String nombre,String telefono,String direccion) {
  var u=cuenta(id);return users.guardar(new Usuario(u.idUsuario(),nombre.strip(),u.correo(),u.password(),u.rol(),telefono==null?null:telefono.strip(),u.activo(),u.seguridadVersion(),u.fotoUrl(),direccion==null?null:direccion.strip()));
 }
 @Transactional public Usuario cambiarPassword(Long id,String actual,String nueva) {
  var u=cuenta(id);if(!passwords.coincide(actual,u.password())) throw new BusinessException(BusinessException.Tipo.INVALIDO,"La contraseña actual no es correcta.");
  if(nueva.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new BusinessException(BusinessException.Tipo.INVALIDO,"La contraseña no puede superar 72 bytes.");
  if(passwords.coincide(nueva,u.password())) throw BusinessException.conflicto("Elige una contraseña diferente de la actual.");
  return users.guardar(new Usuario(u.idUsuario(),u.nombre(),u.correo(),passwords.codificar(nueva),u.rol(),u.telefono(),u.activo(),u.seguridadVersion()+1,u.fotoUrl(),u.direccion()));
 }
 @Transactional public Usuario actualizarFoto(Long id,String url) {var u=cuenta(id);return users.guardar(new Usuario(u.idUsuario(),u.nombre(),u.correo(),u.password(),u.rol(),u.telefono(),u.activo(),u.seguridadVersion(),url,u.direccion()));}

 @Transactional(readOnly=true) public CuentaActividad actividad(Long id) {
  var lista=orders.buscarPorUsuario(id).stream().sorted(java.util.Comparator.comparing(Pedido::getIdPedido).reversed()).toList();
  var vigente=java.math.BigDecimal.ZERO;var entregado=java.math.BigDecimal.ZERO;var cancelado=java.math.BigDecimal.ZERO;var descuentos=java.math.BigDecimal.ZERO;
  for(var p:lista){if(p.getEstadoPedido()==EstadoPedido.CANCELLED)cancelado=cancelado.add(p.getTotal());else{vigente=vigente.add(p.getTotal());descuentos=descuentos.add(p.getDescuento());if(p.getEstadoPedido()==EstadoPedido.ENTREGADO)entregado=entregado.add(p.getTotal());}}
  return new CuentaActividad(vigente,entregado,cancelado,descuentos,lista.stream().map(p->new CuentaActividad.Movimiento(p.getIdPedido(),p.getFechaCreacion(),p.getEstadoPedido(),p.getSubtotal(),p.getDescuento(),p.getCostoEnvio(),p.getTotal(),p.getDetalles().stream().mapToInt(DetallePedido::getCantidad).sum())).toList());
 }
}
