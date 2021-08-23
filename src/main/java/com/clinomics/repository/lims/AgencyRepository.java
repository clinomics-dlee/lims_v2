package com.clinomics.repository.lims;

import java.util.Optional;

import com.clinomics.entity.lims.Agency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyRepository extends JpaRepository<Agency, Integer> {
    boolean existsByName(String name);
    Optional<Agency> findByName(String name);
}
