package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.*;import com.radiocafe.domain.model.Operacion.*;import com.radiocafe.domain.ports.in.*;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.Authentication;import org.springframework.http.HttpStatus;import java.util.List;
@io.swagger.v3.oas.annotations.tags.Tag(name="Operación y notificaciones")
@RestController @RequestMapping("/api/v1")
public class OperationsController {
 private final NotificationUseCase notifications;private final BaristaUseCase baristas;private final CurrentUser current;
 public OperationsController(NotificationUseCase notifications,BaristaUseCase baristas,CurrentUser current) {this.notifications=notifications;this.baristas=baristas;this.current=current;}
 public record Availability(@NotNull Boolean disponible) {}
 public record Estimate(@Min(1) @Max(240) int minutos) {}
 public record Shortage(@NotBlank @Size(max=100) String insumo,@Min(1) @Max(100000) int cantidad,@NotBlank @Size(max=30) String unidad,@NotBlank @Size(min=5,max=1000) String detalle) {}
 @GetMapping("/notifications") public Bandeja list(Authentication auth) {return notifications.listar(current.get(auth).idUsuario());}
 @PatchMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@PathVariable Long id,Authentication auth) {notifications.leer(id,current.get(auth).idUsuario());}
 @PatchMapping("/notifications/read-all") @ResponseStatus(HttpStatus.NO_CONTENT) public void readAll(Authentication auth) {notifications.leerTodas(current.get(auth).idUsuario());}
 @GetMapping("/baristas") public List<Barista> baristas() {return baristas.listar();}
 @GetMapping("/barista/me") @PreAuthorize("hasRole('BARISTA')") public Barista me(Authentication auth) {return baristas.estado(current.get(auth));}
 @PatchMapping("/barista/availability") @PreAuthorize("hasRole('BARISTA')") public Barista available(@Valid @RequestBody Availability input,Authentication auth) {return baristas.disponibilidad(current.get(auth),input.disponible());}
 @PatchMapping("/orders/{id}/preparation") @PreAuthorize("hasAnyRole('BARISTA','ADMINISTRADOR')") public Pedido estimate(@PathVariable Long id,@Valid @RequestBody Estimate input,Authentication auth) {return baristas.estimar(id,input.minutos(),current.get(auth));}
 @PostMapping("/barista/shortages") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('BARISTA')") public void shortage(@Valid @RequestBody Shortage input,Authentication auth) {baristas.faltaInsumos(current.get(auth),input.insumo(),input.cantidad(),input.unidad(),input.detalle());}
}
