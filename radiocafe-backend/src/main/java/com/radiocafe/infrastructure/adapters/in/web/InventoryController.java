package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.InventoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/inventory")
@Tag(name="Inventario",description="Existencias y movimientos. Administrador: ajustes; administrador y barista: consulta.")
public class InventoryController {
    private final InventoryUseCase inventory;
    private final CurrentUser current;
    public InventoryController(InventoryUseCase inventory,CurrentUser current) {this.inventory=inventory;this.current=current;}
    @GetMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','BARISTA')")
    @Operation(summary="Consultar stock de todos los productos, incluidos retirados")
    public List<Producto> list() {return inventory.listar();}
    @PostMapping("/{id}/adjustments") @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary="Registrar entrada o salida manual",description="Cantidad positiva para entrada, negativa para salida. Requiere motivo y rechaza stock negativo. No acepta precios ni un saldo calculado por el cliente.")
    public Producto adjust(@PathVariable Long id,@Valid @RequestBody ApiDtos.StockAdjustment input,Authentication auth) {
        return inventory.ajustar(id,input.cantidad(),input.motivo(),current.get(auth).idUsuario());
    }
    @GetMapping("/movements") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BARISTA')")
    @Operation(summary="Consultar los últimos 200 movimientos",description="Filtrar opcionalmente por idProducto. Ordenados del más reciente al más antiguo.")
    public List<MovimientoInventario> movements(@RequestParam(required=false) Long idProducto) {return inventory.movimientos(idProducto);}
}
