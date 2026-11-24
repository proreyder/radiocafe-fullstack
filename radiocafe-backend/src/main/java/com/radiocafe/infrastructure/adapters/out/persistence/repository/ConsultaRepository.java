package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ConsultaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConsultaRepository extends JpaRepository<ConsultaEntity,Long> { java.util.List<ConsultaEntity> findByUsuarioOrderByFechaDesc(Long usuario); java.util.List<ConsultaEntity> findAllByOrderByFechaDesc(); }
