package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.CampanaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CampanaRepository extends JpaRepository<CampanaEntity,Long> { java.util.List<CampanaEntity> findAllByOrderByIdDesc(); }
