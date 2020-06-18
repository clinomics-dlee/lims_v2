package com.clinomics.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.ResultSpecification;
import com.clinomics.specification.lims.SampleSpecification;
import com.google.common.collect.Maps;

@Service
public class CalendarService {

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	DataTableService dataTableService;
	
	@Autowired
	SampleItemService sampleItemService;

	@PersistenceContext
	private EntityManager entityManager;

	public Bundle selectOne(int id) {
		return bundleRepository.findById(id).orElse(new Bundle());
	}

	public List<Bundle> selectAll() {
		return bundleRepository.findAll();
	}

	public Map<String, Object> selectCountByMonthly(Map<String, String> params) {
		
		
		List<Sample> sample = sampleRepository.findAll(getRegisteredWhere(params));
		List<Sample> resultAnalysis = sampleRepository.findAll(getAnalysisWhere(params));
		List<Sample> resultComplete = sampleRepository.findAll(getCompletedWhere(params));
		List<Sample> resultCompletePdf = sampleRepository.findAll(getReportedWhere(params));
		
//		TemporalField weekFields = WeekFields.of(Locale.getDefault()).weekOfMonth();
		
		List<Map<String, String>> mapSample = sample.stream()
			.map(s -> {
				Map<String, String> t = Maps.newHashMap();
				t.put("day", s.getCreatedDate().getDayOfMonth() + "");
//				t.put("weekOfMonth", s.getCreatedDate().get(weekFields) + "");
				return t;
			}).collect(Collectors.toList());
		
		List<Map<String, String>> mapAnalysis = resultAnalysis.stream()
			.map(s -> {
				Map<String, String> t = Maps.newHashMap();
				t.put("day", (s.getModifiedDate().getDayOfMonth() + ""));
				return t;
			}).collect(Collectors.toList());
		
		List<Map<String, String>> mapComplete = resultComplete.stream()
			.map(s -> {
				Map<String, String> t = Maps.newHashMap();
				t.put("day", (s.getModifiedDate().getDayOfMonth() + ""));
				return t;
			}).collect(Collectors.toList());
		
		List<Map<String, String>> mapCompletePdf = resultCompletePdf.stream()
			.map(s -> {
				Map<String, String> t = Maps.newHashMap();
				t.put("day", s.getModifiedDate().getDayOfMonth() + "");
				return t;
			}).collect(Collectors.toList());
		
		Map<String, Long> groupbySample = getGroupingMap(mapSample, "day");
		Map<String, Long> groupbyAnalysis = getGroupingMap(mapAnalysis, "day");
		Map<String, Long> groupbyComplete = getGroupingMap(mapComplete, "day");
		Map<String, Long> groupbyCompletePdf = getGroupingMap(mapCompletePdf, "day");
		
        Map<String, Object> rtn = Maps.newHashMap();
        rtn.put("sample", groupbySample);
        rtn.put("analysis", groupbyAnalysis);
        rtn.put("complete", groupbyComplete);
        rtn.put("completePdf", groupbyCompletePdf);
        
		return rtn;
		
	}
	
	public Map<String, Object> selectRegistered(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);

		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		
		Specification<Sample> where = getRegisteredWhere(params);
		Page<Sample> sample = sampleRepository.findAll(where, pageable);
		long total = sample.getTotalElements();
		List<Sample> list = sample.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}
	
	public Map<String, Object> selectAnalysis(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);

		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		
		Specification<Sample> where = getAnalysisWhere(params);
		Page<Sample> sample = sampleRepository.findAll(where, pageable);
		long total = sample.getTotalElements();
		List<Sample> list = sample.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}
	
	public Map<String, Object> selectCompleted(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);

		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		
		Specification<Sample> where = getCompletedWhere(params);
		Page<Sample> sample = sampleRepository.findAll(where, pageable);
		long total = sample.getTotalElements();
		List<Sample> list = sample.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}
	
	public Map<String, Object> selectReported(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);

		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		
		Specification<Sample> where = getReportedWhere(params);
		Page<Sample> sample = sampleRepository.findAll(where, pageable);
		long total = sample.getTotalElements();
		List<Sample> list = sample.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	private Specification<Sample> getCreateDateWhere(Map<String, String> params) {
		if (params.containsKey("yyyymm")) {
			return SampleSpecification.createdDateOneMonth(params);
		} else {
			return SampleSpecification.betweenDate(params);
		}
	}

	private Specification<Sample> getModifiedDateWhere(Map<String, String> params) {
		if (params.containsKey("yyyymm")) {
			return SampleSpecification.modifiedDateOneMonth(params);
		} else {
			return SampleSpecification.betweenModifiedDate(params);
		}
	}
	
	// private Specification<Sample> getSampleWhere(Map<String, String> params) {
	// 	return Specification
	// 		.where(getCreateDateWhere(params))
	// 		.and(SampleSpecification.bundleId(params))
	// 		.and(SampleSpecification.statusCodeGt(20));
	// }
	
	private Specification<Sample> getRegisteredWhere(Map<String, String> params) {
		return Specification
			.where(getCreateDateWhere(params))
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.statusCodeGt(20));
	}
	
	private Specification<Sample> getAnalysisWhere(Map<String, String> params) {
		return Specification
			.where(getModifiedDateWhere(params))
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.statusIn(
				Arrays.asList(new StatusCode[] {
					StatusCode.S200_EXP_READY
					, StatusCode.S210_EXP_STEP1
					, StatusCode.S220_EXP_STEP2
					, StatusCode.S230_EXP_STEP3
					, StatusCode.S400_ANLS_READY
					, StatusCode.S410_ANLS_RUNNING
					, StatusCode.S420_ANLS_SUCC
					, StatusCode.S440_ANLS_SUCC_CMPL
					, StatusCode.S430_ANLS_FAIL
					, StatusCode.S450_ANLS_FAIL_CMPL
				})
			));
	}
	
	private Specification<Sample> getCompletedWhere(Map<String, String> params) {
		return Specification
			.where(getModifiedDateWhere(params))
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.statusIn(Arrays.asList(
				StatusCode.S460_ANLS_CMPL
				, StatusCode.S600_JDGM_APPROVE
				, StatusCode.S700_OUTPUT_WAIT
			)));
	}
	
	private Specification<Sample> getReportedWhere(Map<String, String> params) {
		return Specification
			.where(getModifiedDateWhere(params))
				.and(SampleSpecification.bundleId(params))
				.and(SampleSpecification.statusIn(Arrays.asList(
					StatusCode.S710_OUTPUT_CMPL
					, StatusCode.S800_RE_OUTPUT_WAIT
					, StatusCode.S810_RE_OUTPUT_CMPL
				)));
	}
	
	private Map<String, Long> getGroupingMap(List<Map<String, String>> map, String key) {
		return map.stream().collect(
			Collectors.groupingBy(m -> m.get(key), Collectors.counting())
		);
	}
	
}
