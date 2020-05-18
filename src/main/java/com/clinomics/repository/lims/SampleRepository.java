package com.clinomics.repository.lims;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.clinomics.entity.lims.Sample;

public interface SampleRepository extends JpaRepository<Sample, Integer>, JpaSpecificationExecutor<Sample> {
    List<Sample> findByIdIn(List<Integer> id);
}
