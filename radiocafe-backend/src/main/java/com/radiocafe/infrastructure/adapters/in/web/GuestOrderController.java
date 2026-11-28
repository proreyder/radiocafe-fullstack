package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.GuestOrderUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
@RestController @RequestMapping("/api/v1/guest/orders")
@io.swagger.v3.oas.annotations.tags.Tag(name="Pedidos de invitados",description="Compra sin cuenta y seguimiento privado mediante token. Pago al recoger o recibir.")
public class GuestOrderController {
 private final GuestOrderUseCase guests;
 public GuestOrderController(GuestOrderUseCase guests){this.guests=guests;}
 public record ContactInput(@NotBlank @Size(max=100) String nombre,@NotBlank @Pattern(regexp="[+0-9 ()-]{7,30}") String telefono,@NotBlank @Email @Size(max=150) String correo) {}
 public record GuestInput(@NotNull @Valid ContactInput contacto,@NotNull @Valid ApiDtos.OrderInput pedido) {}
 public record Line(int cantidad,String nombreProducto,String personalizacionDescripcion,BigDecimal subtotalLinea) {}
 public record Tracking(Long idPedido,EstadoPedido estadoPedido,TipoEntrega tipoEntrega,BigDecimal subtotal,BigDecimal costoEnvio,BigDecimal descuento,BigDecimal total,Integer minutosPreparacion,Instant preparacionEstimada,String nombreBarista,String metodoPago,List<Line> detalles) {
  static Tracking of(Pedido p){return new Tracking(p.getIdPedido(),p.getEstadoPedido(),p.getTipoEntrega(),p.getSubtotal(),p.getCostoEnvio(),p.getDescuento(),p.getTotal(),p.getMinutosPreparacion(),p.getPreparacionEstimada(),p.getNombreBarista(),p.getTipoEntrega()==TipoEntrega.DELIVERY?"PAGO_AL_RECIBIR":"PAGO_AL_RECOGER",p.getDetalles().stream().map(d->new Line(d.getCantidad(),d.getNombreProducto(),d.getPersonalizacionDescripcion(),d.getSubtotalLinea())).toList());}
 }
 public record Confirmation(Tracking pedido,String seguimientoToken) {}
 @PostMapping public ResponseEntity<Confirmation> create(@Valid @RequestBody GuestInput input){
  var pedido=input.pedido().toDomain(null);var c=input.contacto();pedido.setContacto(new ContactoPedido(c.nombre().strip(),c.telefono().strip(),c.correo().strip()));
  var result=guests.crear(pedido);return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(new Confirmation(Tracking.of(result.pedido()),result.token()));
 }
 @GetMapping("/{id}") public ResponseEntity<Tracking> track(@PathVariable @Positive Long id,@RequestHeader(value="X-Order-Token",required=false) String token){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer").body(Tracking.of(guests.seguir(id,token)));}
}
