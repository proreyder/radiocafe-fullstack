package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.VotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VotoRepository extends JpaRepository<VotoEntity,Long> { boolean existsByIdUsuario(Long idUsuario); }
