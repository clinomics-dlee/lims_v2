package com.clinomics.repository.lims;

import org.springframework.data.jpa.repository.JpaRepository;
import com.clinomics.entity.lims.SampleTemp;

public interface SampleTempRepository extends JpaRepository<SampleTemp, String> {
}
