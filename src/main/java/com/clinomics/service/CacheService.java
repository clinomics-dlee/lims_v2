package com.clinomics.service;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;

@Service
public class CacheService {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
    SampleRepository sampleRepository;

	@Autowired
	BundleRepository bundleRepository;

	// @Cacheable(value = "apiCache", key = "#productType")
	// public List<Sample> getOutputResultGet(String productType) {

	// 	Specification<Sample> where = Specification
	// 			.where(SampleSpecification.productNotLike(null))
	// 			.and(SampleSpecification.statusIn(Arrays.asList(StatusCode.S700_OUTPUT_WAIT, StatusCode.S800_RE_OUTPUT_WAIT)));

	// 	List<Sample> samples = sampleRepository.findAll(where);
	// 	return samples;
	// }

	@CacheEvict(value = "apiCache", allEntries = true)
	public void cleanOutputResult() {
		
	}

	@Cacheable(value = "hospitalCache")
	public List<String> getDistinctHospitalName() {
		List<String> names = sampleRepository.findDistinctHospitalName();
		return names;
	}

	@Cacheable(value = "bundleCache")
	public List<Bundle> selectAll() {
		return bundleRepository.findByIsActiveTrue();
	}

}
