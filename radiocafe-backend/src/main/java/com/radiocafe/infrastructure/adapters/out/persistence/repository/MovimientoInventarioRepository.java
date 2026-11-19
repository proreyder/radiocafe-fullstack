package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.MovimientoInventarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventarioEntity,Long> {
    List<MovimientoInventarioEntity> findTop200ByOrderByIdDesc();
    List<MovimientoInventarioEntity> findTop200ByIdProductoOrderByIdDesc(Long idProducto);
}
