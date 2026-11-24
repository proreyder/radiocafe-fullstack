package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.Comercio.*;
import com.radiocafe.domain.ports.in.CommerceUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@io.swagger.v3.oas.annotations.tags.Tag(name="Comercio",description="Promociones, cupones, marketing, comentarios, atención al cliente y recomendaciones.")
@RestController @RequestMapping("/api/v1/commerce")
public class CommerceController {
 private final CommerceUseCase commerce;private final CurrentUser current;
 public CommerceController(CommerceUseCase commerce,CurrentUser current) {this.commerce=commerce;this.current=current;}
 public record CampaignInput(@NotBlank @Size(max=120) String titulo,@NotBlank @Size(max=1000) String mensaje,@Size(max=30) String codigo,@NotNull @DecimalMin("0.01") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal porcentaje,@NotNull @DecimalMin("0") @DecimalMax("9999999.99") @Digits(integer=7,fraction=2) BigDecimal minimo,@NotNull LocalDateTime inicio,@NotNull LocalDateTime fin,boolean activa) {
  Campana toDomain() {return new Campana(null,titulo,mensaje,codigo,porcentaje,minimo,inicio,fin,activa);}
 }
 public record QuoteInput(@NotNull @DecimalMin("0") @DecimalMax("9999999.99") @Digits(integer=7,fraction=2) BigDecimal subtotal,@Size(max=30) String codigo) {}
 public record ReviewInput(@Min(1) @Max(5) int estrellas,@NotBlank @Size(min=3,max=1500) String comentario) {}
 public record VisibilityInput(@NotNull Boolean visible) {}
 public record TicketInput(@NotBlank @Size(min=3,max=120) String asunto,@NotBlank @Size(min=5,max=2000) String mensaje) {}
 public record ReplyInput(@NotBlank @Size(min=3,max=2000) String respuesta) {}
 public record ShopInput(@NotBlank @Size(max=100) String nombre,@NotBlank @Pattern(regexp="CAFETERIA|PANADERIA|PASTELERIA|CAFETERIA_PASTELERIA") String tipoComercio,@NotBlank @Size(max=1000) String descripcion,@NotBlank @Size(max=200) String horario,@Size(max=30) String telefono,@Email @Size(max=150) String correo) {
  Tienda toDomain() {return new Tienda(nombre,tipoComercio,descripcion,horario,telefono,correo);}
 }
 @GetMapping("/shop") public Tienda shop() {return commerce.tienda();}
 @PutMapping("/shop") @PreAuthorize("hasRole('ADMINISTRADOR')") public Tienda shop(@Valid @RequestBody ShopInput input) {return commerce.guardarTienda(input.toDomain());}
 @GetMapping("/campaigns") public List<Campana> campaigns() {return commerce.campanas(false);}
 @GetMapping("/admin/campaigns") @PreAuthorize("hasRole('ADMINISTRADOR')") public List<Campana> allCampaigns() {return commerce.campanas(true);}
 @PostMapping("/campaigns") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMINISTRADOR')") public Campana create(@Valid @RequestBody CampaignInput input) {return commerce.guardarCampana(null,input.toDomain());}
 @PutMapping("/campaigns/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public Campana edit(@PathVariable Long id,@Valid @RequestBody CampaignInput input) {return commerce.guardarCampana(id,input.toDomain());}
 @PostMapping("/quote") public Descuento quote(@Valid @RequestBody QuoteInput input) {return commerce.descuento(input.subtotal(),input.codigo());}
 @GetMapping("/reviews") public List<Opinion> reviews(@RequestParam @Positive Long producto) {return commerce.opiniones(producto,false);}
 @GetMapping("/admin/reviews") @PreAuthorize("hasRole('ADMINISTRADOR')") public List<Opinion> allReviews() {return commerce.opiniones(null,true);}
 @PostMapping("/products/{id}/reviews") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('CLIENTE')") public Opinion review(@PathVariable Long id,@Valid @RequestBody ReviewInput input,Authentication auth) {return commerce.comentar(id,input.estrellas(),input.comentario(),current.get(auth));}
 @PatchMapping("/reviews/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public Opinion moderate(@PathVariable Long id,@Valid @RequestBody VisibilityInput input) {return commerce.moderar(id,input.visible());}
 @GetMapping("/support") public List<Consulta> tickets(Authentication auth) {return commerce.consultas(current.get(auth));}
 @PostMapping("/support") @ResponseStatus(HttpStatus.CREATED) public Consulta ticket(@Valid @RequestBody TicketInput input,Authentication auth) {return commerce.consultar(input.asunto(),input.mensaje(),current.get(auth));}
 @PatchMapping("/support/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public Consulta reply(@PathVariable Long id,@Valid @RequestBody ReplyInput input) {return commerce.responder(id,input.respuesta());}
 @GetMapping("/recommendations") public List<Recomendacion> recommendations(Authentication auth) {return commerce.recomendar(current.get(auth));}
}
