package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ProductoEntity;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface SpringDataProductoRepository extends JpaRepository<ProductoEntity,Long> {
    List<ProductoEntity> findByDisponibleTrue();
    List<ProductoEntity> findByCategoriaAndDisponibleTrue(String categoria);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoEntity p where p.idProducto=:id")
    Optional<ProductoEntity> bloquear(Long id);
}
