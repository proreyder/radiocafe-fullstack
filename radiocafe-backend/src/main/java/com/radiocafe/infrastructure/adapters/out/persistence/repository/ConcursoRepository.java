package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ConcursoEntity;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ConcursoRepository extends JpaRepository<ConcursoEntity,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from ConcursoEntity c where c.id=1") Optional<ConcursoEntity> bloquear();
}
