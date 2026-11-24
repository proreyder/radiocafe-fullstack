package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.*;import java.util.List;
public interface NotificacionRepository extends JpaRepository<NotificacionEntity,Long> {
 List<NotificacionEntity> findTop100ByUsuarioOrderByFechaDescIdDesc(Long usuario); long countByUsuarioAndLeidaFalse(Long usuario);
 @Modifying @Query("update NotificacionEntity n set n.leida=true where n.usuario=:usuario and n.leida=false") void leerTodas(Long usuario);
}
