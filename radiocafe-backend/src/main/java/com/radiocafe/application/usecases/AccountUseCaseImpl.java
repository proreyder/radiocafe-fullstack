package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.AccountUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
@Service
public class AccountUseCaseImpl implements AccountUseCase {
 private final UserRepositoryPort users; private final PasswordPort passwords;
 public AccountUseCaseImpl(UserRepositoryPort users,PasswordPort passwords) { this.users=users; this.passwords=passwords; }
 @Transactional
 public Usuario registrar(String nombre,String correo,String password) {
  correo=correo.strip().toLowerCase(Locale.ROOT);
  if(users.existePorCorreo(correo)) throw BusinessException.conflicto("Este correo ya está registrado.");
  return users.guardar(new Usuario(null,nombre.strip(),correo,passwords.codificar(password),Rol.CLIENTE,null));
 }
}
