package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.ports.in.AccountUseCase;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Locale;
@io.swagger.v3.oas.annotations.tags.Tag(name="Autenticación",description="Registro público de clientes y autenticación mediante sesión y CSRF.")
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final AccountUseCase accounts;private final AuthenticationManager manager;private final SecurityContextRepository contexts;private final SessionAuthenticationStrategy sessions;private final CurrentUser current;
 public AuthController(AccountUseCase accounts,AuthenticationManager manager,SecurityContextRepository contexts,SessionAuthenticationStrategy sessions,CurrentUser current) {this.accounts=accounts;this.manager=manager;this.contexts=contexts;this.sessions=sessions;this.current=current;}
 @io.swagger.v3.oas.annotations.security.SecurityRequirements
 @io.swagger.v3.oas.annotations.Operation(summary="Obtener token CSRF")
 @GetMapping("/csrf") public CsrfToken csrf(CsrfToken token) {return token;}
 @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="csrf")
 @io.swagger.v3.oas.annotations.Operation(summary="Registrar una cuenta de cliente")
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
 public ApiDtos.UserView register(@Valid @RequestBody ApiDtos.Register input) {return ApiDtos.UserView.of(accounts.registrar(input.nombre(),input.correo(),input.password()));}
 @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="csrf")
 @io.swagger.v3.oas.annotations.Operation(summary="Iniciar sesión con correo y contraseña")
 @PostMapping("/login") public ApiDtos.UserView login(@Valid @RequestBody ApiDtos.Login input,HttpServletRequest request,HttpServletResponse response) {
  Authentication auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.correo().strip().toLowerCase(Locale.ROOT),input.password()));
  sessions.onAuthentication(auth,request,response);var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);
  return ApiDtos.UserView.of(current.get(auth));
 }
 @io.swagger.v3.oas.annotations.Operation(summary="Consultar la cuenta de la sesión actual")
 @GetMapping("/me") public ApiDtos.UserView me(Authentication auth) {return ApiDtos.UserView.of(current.get(auth));}
}
