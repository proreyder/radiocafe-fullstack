package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.Usuario;
public interface AccountUseCase { Usuario registrar(String nombre,String correo,String password); }
