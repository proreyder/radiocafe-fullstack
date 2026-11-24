package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.*;import com.radiocafe.domain.ports.in.*;import com.radiocafe.infrastructure.config.AuthenticatedUser;
import jakarta.servlet.http.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.http.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="Mi cuenta",description="Perfil, foto privada, contraseña y actividad de pedidos de la cuenta autenticada.")
@RestController @RequestMapping("/api/v1/account")
public class AccountController {
 private final AccountUseCase accounts;private final ProductImagesUseCase images;private final CurrentUser current;private final SecurityContextRepository contexts;
 public AccountController(AccountUseCase accounts,ProductImagesUseCase images,CurrentUser current,SecurityContextRepository contexts){this.accounts=accounts;this.images=images;this.current=current;this.contexts=contexts;}
 public record Profile(@NotBlank @Size(max=100) String nombre,@Size(max=20) String telefono,@Size(max=500) String direccion) {}
 public record ChangePassword(@NotBlank @Size(max=72) String actual,@NotBlank @Size(min=8,max=72) String nueva) {}
 @PatchMapping("/profile") public ApiDtos.UserView profile(@Valid @RequestBody Profile input,Authentication auth){return ApiDtos.UserView.of(accounts.actualizarPerfil(current.get(auth).idUsuario(),input.nombre(),input.telefono(),input.direccion()));}
 @PutMapping("/password") public ApiDtos.UserView password(@Valid @RequestBody ChangePassword input,Authentication auth,HttpServletRequest request,HttpServletResponse response){
  var u=accounts.cambiarPassword(current.get(auth).idUsuario(),input.actual(),input.nueva());
  var principal=new AuthenticatedUser(u);var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
  if(request.getSession(false)!=null)request.changeSessionId();SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);return ApiDtos.UserView.of(u);
 }
 @PostMapping(value="/avatar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiDtos.UserView upload(@RequestPart("file") MultipartFile file,Authentication auth)throws java.io.IOException {var u=current.get(auth);var image=images.subirPerfil(file.getBytes());return ApiDtos.UserView.of(accounts.actualizarFoto(u.idUsuario(),image.url()));}
 @DeleteMapping("/avatar") public ApiDtos.UserView remove(Authentication auth){return ApiDtos.UserView.of(accounts.actualizarFoto(current.get(auth).idUsuario(),null));}
 @GetMapping("/avatar/{nombre}") public ResponseEntity<byte[]> avatar(@PathVariable String nombre,Authentication auth){var u=current.get(auth);if(!("/api/v1/account/avatar/"+nombre).equals(u.fotoUrl()))throw BusinessException.noEncontrado("Foto de perfil no encontrada.");return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(nombre.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).header("X-Content-Type-Options","nosniff").body(images.leerPerfil(nombre));}
 @GetMapping("/activity") public CuentaActividad activity(Authentication auth){return accounts.actividad(current.get(auth).idUsuario());}
}
