package com.radiocafe.domain.model;

public record Usuario(Long idUsuario, String nombre, String correo, String password, Rol rol,
                      String telefono, boolean activo, long seguridadVersion) {
    public Usuario(Long id, String nombre, String correo, String password, Rol rol, String telefono) {
        this(id, nombre, correo, password, rol, telefono, true, 0L);
    }
}
