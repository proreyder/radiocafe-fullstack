package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioEntity,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from UsuarioEntity u where u.idUsuario=:id")
    Optional<UsuarioEntity> bloquearPorId(Long id);
    Optional<UsuarioEntity> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioEntity u where u.rol=com.radiocafe.domain.model.Rol.ADMINISTRADOR order by u.idUsuario")
    List<UsuarioEntity> bloquearAdministradores();
}
