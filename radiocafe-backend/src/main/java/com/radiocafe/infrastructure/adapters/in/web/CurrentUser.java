package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.Usuario;
import com.radiocafe.domain.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
@Component
public class CurrentUser {
 private final UserRepositoryPort users;
 public CurrentUser(UserRepositoryPort users) {this.users=users;}
 public Usuario get(Authentication auth) {return users.buscarPorCorreo(auth.getName()).orElseThrow(()->new AccessDeniedException("Usuario no disponible."));}
 public boolean staff(Authentication auth) {return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMINISTRADOR")||a.getAuthority().equals("ROLE_BARISTA"));}
}
