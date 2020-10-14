package com.clinomics.repository.lims;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;

public interface SampleRepository extends JpaRepository<Sample, Integer>, JpaSpecificationExecutor<Sample> {
    List<Sample> findByIdIn(List<Integer> id);
    List<Sample> findByIdInAndStatusCodeIn(List<Integer> id, List<StatusCode> statusCodes);
    //Optional<Sample> findTopByBundle_IdOrderByLaboratoryIdDesc(int bundleId);
    //Optional<Sample> findTopByBundle_IdAndReceivedDateOrderByLaboratoryIdDesc(int bundleId, LocalDate receivedDate);
    
	@Query(value = "SELECT MAX(laboratory_id) FROM sample WHERE bundle_id = ?1", nativeQuery = true)
	String findMaxLaboratoryId(int bundleId);
	@Query(value = "SELECT MAX(laboratory_id) FROM sample WHERE bundle_id = ?1 AND DATE_FORMAT(received_date, '%Y%m') = ?2", nativeQuery = true)
	String findMaxHospitalLaboratoryId(int bundleId, String yyyymm);
}
