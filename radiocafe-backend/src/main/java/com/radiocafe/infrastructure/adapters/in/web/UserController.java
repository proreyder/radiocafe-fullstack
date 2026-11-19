package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.ports.in.ManageUsersUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController @RequestMapping("/api/v1/users") @PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name="Usuarios",description="Gestión exclusiva del administrador. Las contraseñas y hashes nunca se incluyen en las respuestas.")
public class UserController {
    private final ManageUsersUseCase users;
    private final CurrentUser current;
    public UserController(ManageUsersUseCase users,CurrentUser current) {this.users=users;this.current=current;}
    @GetMapping @Operation(summary="Listar cuentas, roles y estado")
    public List<ApiDtos.UserView> list() {return users.listar().stream().map(ApiDtos.UserView::of).toList();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Crear una cuenta con un rol",description="Roles: CLIENTE, BARISTA, REPARTIDOR o ADMINISTRADOR. La cuenta se crea activa y la contraseña se almacena con BCrypt.")
    public ApiDtos.UserView create(@Valid @RequestBody ApiDtos.CreateUser input,Authentication auth) {
        return ApiDtos.UserView.of(users.crear(input.nombre(),input.correo(),input.password(),input.rol(),input.telefono(),current.get(auth).idUsuario()));
    }
    @PatchMapping("/{id}") @Operation(summary="Actualizar nombre, teléfono, rol o estado",description="No permite cambiar el correo ni la contraseña. Cambiar rol o estado revoca sesiones existentes. No se permite desactivar la propia cuenta ni quitarse el rol de administrador.")
    public ApiDtos.UserView update(@PathVariable Long id,@Valid @RequestBody ApiDtos.UpdateUser input,Authentication auth) {
        return ApiDtos.UserView.of(users.actualizar(id,input.nombre(),input.rol(),input.activo(),input.telefono(),current.get(auth).idUsuario()));
    }
}
