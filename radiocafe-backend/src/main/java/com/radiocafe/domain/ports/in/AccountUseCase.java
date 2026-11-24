package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.Usuario;
public interface AccountUseCase { Usuario registrar(String nombre,String correo,String password);
 Usuario actualizarPerfil(Long id,String nombre,String telefono,String direccion);
 Usuario cambiarPassword(Long id,String actual,String nueva);
 Usuario actualizarFoto(Long id,String url);
 com.radiocafe.domain.model.CuentaActividad actividad(Long id); }
