package com.radiocafe.infrastructure.adapters.out.persistence.repository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.GuestOrderAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GuestOrderAccessRepository extends JpaRepository<GuestOrderAccessEntity,Long> {}
