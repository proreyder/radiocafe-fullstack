package com.radiocafe.domain.ports.out;

import com.radiocafe.domain.model.Usuario;

import java.util.Optional;
import java.util.List;

public interface UserRepositoryPort {

    Usuario guardar(Usuario usuario);
    Optional<Usuario> bloquearPorId(Long id);

    List<Usuario> obtenerTodos();

    void bloquearAdministradores();

    Optional<Usuario> buscarPorId(Long idUsuario);

    Optional<Usuario> buscarPorCorreo(String correo);

    boolean existePorCorreo(String correo);
}