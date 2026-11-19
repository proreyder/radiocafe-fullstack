package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.Producto;
import com.radiocafe.domain.ports.in.CatalogUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import java.util.List;
@io.swagger.v3.oas.annotations.tags.Tag(name="Catálogo",description="Consulta pública de productos. Las escrituras requieren ADMINISTRADOR y CSRF.")
@RestController @RequestMapping("/api/v1/products")
public class ProductController {
 private final CatalogUseCase catalog;
 public ProductController(CatalogUseCase catalog) {this.catalog=catalog;}
 @GetMapping("/options") public com.radiocafe.domain.model.Personalizacion.Catalogo options() {return com.radiocafe.domain.model.Personalizacion.catalogo();}
 @io.swagger.v3.oas.annotations.security.SecurityRequirements
 @io.swagger.v3.oas.annotations.Operation(summary="Listar productos disponibles y stock")
 @GetMapping public List<Producto> list() {return catalog.listar();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMINISTRADOR')")
 public Producto create(@Valid @RequestBody ApiDtos.ProductInput input) {return catalog.guardar(null,input.toDomain());}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
 public Producto update(@PathVariable Long id,@Valid @RequestBody ApiDtos.ProductInput input) {return catalog.guardar(id,input.toDomain());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMINISTRADOR')")
 public void delete(@PathVariable Long id) {catalog.eliminar(id);}
}
