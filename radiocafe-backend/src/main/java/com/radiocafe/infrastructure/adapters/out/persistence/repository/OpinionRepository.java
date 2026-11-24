package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.OpinionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OpinionRepository extends JpaRepository<OpinionEntity,Long> { java.util.List<OpinionEntity> findByProductoAndVisibleTrueOrderByFechaDesc(Long producto); java.util.List<OpinionEntity> findAllByOrderByFechaDesc(); boolean existsByProductoAndUsuario(Long producto,Long usuario); }
