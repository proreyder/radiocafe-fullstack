package com.radiocafe.domain.model;
public record Usuario(Long idUsuario,String nombre,String correo,String password,Rol rol,String telefono,boolean activo,long seguridadVersion,String fotoUrl,String direccion) {
 public Usuario(Long id,String nombre,String correo,String password,Rol rol,String telefono) {this(id,nombre,correo,password,rol,telefono,true,0L,null,null);}
 public Usuario(Long id,String nombre,String correo,String password,Rol rol,String telefono,boolean activo,long version) {this(id,nombre,correo,password,rol,telefono,activo,version,null,null);}
}
