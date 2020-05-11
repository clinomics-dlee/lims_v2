package com.clinomics.repository.lims;

import com.clinomics.entity.lims.Sample;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DleeRepository extends JpaRepository<Sample, Integer>, JpaSpecificationExecutor<Sample> {

}
