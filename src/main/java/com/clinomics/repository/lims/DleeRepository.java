package com.clinomics.repository.lims;

import java.util.List;

import com.clinomics.entity.lims.Sample;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DleeRepository extends JpaRepository<Sample, Integer>, JpaSpecificationExecutor<Sample> {
    List<Sample> findByLaboratoryId(String laboratoryId);
}
