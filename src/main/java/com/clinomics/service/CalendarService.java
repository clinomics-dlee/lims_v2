package com.clinomics.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.clinomics.specification.lims.SampleSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private final List<StatusCode> STS_EXP = Arrays.asList(new StatusCode[] {
		StatusCode.S210_EXP_STEP1
		, StatusCode.S220_EXP_STEP2
		, StatusCode.S230_EXP_STEP3
		, StatusCode.S400_ANLS_READY
		, StatusCode.S410_ANLS_RUNNING
		, StatusCode.S420_ANLS_SUCC
		, StatusCode.S440_ANLS_SUCC_CMPL
		, StatusCode.S430_ANLS_FAIL
		, StatusCode.S450_ANLS_FAIL_CMPL
		, StatusCode.S460_ANLS_CMPL
	});

	private final List<StatusCode> STS_JDGM = Arrays.asList(new StatusCode[] {
		StatusCode.S600_JDGM_APPROVE
		, StatusCode.S700_OUTPUT_WAIT
	});

	private final List<StatusCode> STS_REPORT = Arrays.asList(new StatusCode[] {
		StatusCode.S710_OUTPUT_CMPL
		, StatusCode.S800_RE_OUTPUT_WAIT
		, StatusCode.S810_RE_OUTPUT_CMPL
		, StatusCode.S900_OUTPUT_CMPL
	});
		
	public Bundle selectOne(int id) {
		return bundleRepository.findById(id).orElse(new Bundle());
	}

	public List<Bundle> selectAll() {
		return bundleRepository.findAll();
	}

	public Map<String, Object> selectCountByMonthly(Map<String, String> params) {
		String yyyymm = (params.get("yyyymm") + "").replace("-", "");
		String strBundles = params.get("bundleId") + "";
		
		List<Integer> bundleIds = Lists.newArrayList();
		if (strBundles.length() > 0) {
			List<String> bundles = Arrays.asList((params.get("bundleId") + "").split(","));
			bundleIds = bundles.stream().map(b -> NumberUtils.toInt(b)).collect(Collectors.toList());
		} else {
			List<Bundle> bs = bundleRepository.findByIsActiveTrue();
			bundleIds = bs.stream().map(b -> b.getId()).collect(Collectors.toList());
		}

		List<String[]> cal1 = sampleRepository.findCalendarDataByCreatedDate(yyyymm, bundleIds, Arrays.asList(StatusCode.class.getEnumConstants()).stream().map(e -> e.toString()).collect(Collectors.toList()));
		List<String[]> cal2 = sampleRepository.findCalendarDataByModifiedDate(yyyymm, bundleIds, STS_EXP.stream().map(e -> e.getKey()).collect(Collectors.toList()));
		List<String[]> cal3 = sampleRepository.findCalendarDataByModifiedDate(yyyymm, bundleIds, STS_JDGM.stream().map(e -> e.getKey()).collect(Collectors.toList()));
		List<String[]> cal4 = sampleRepository.findCalendarDataByOutputCmplDate(yyyymm, bundleIds, STS_REPORT.stream().map(e -> e.getKey()).collect(Collectors.toList()));

		ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> rtn = Maps.newHashMap();

        String json1 = cal1.stream().map(c -> "\"" + c[0] + "\":\"" + c[1] + "\"").collect(Collectors.joining(","));
        String json2 = cal1.stream().map(c -> "\"" + c[0] + "\":\"" + c[2] + "\"").collect(Collectors.joining(","));
        String json3 = cal2.stream().map(c -> "\"" + c[0] + "\":\"" + c[1] + "\"").collect(Collectors.joining(","));
        String json4 = cal3.stream().map(c -> "\"" + c[0] + "\":\"" + c[1] + "\"").collect(Collectors.joining(","));
        String json5 = cal4.stream().map(c -> "\"" + c[0] + "\":\"" + c[1] + "\"").collect(Collectors.joining(","));
        
        try {
			rtn.put("sample", mapper.readValue("{" + json1 + "}", Map.class));
			rtn.put("completeSample", mapper.readValue("{" + json2 + "}", Map.class));
			rtn.put("analysis", mapper.readValue("{" + json3 + "}", Map.class));
			rtn.put("complete", mapper.readValue("{" + json4 + "}", Map.class));
			rtn.put("completePdf", mapper.readValue("{" + json5 + "}", Map.class));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return rtn;
		
	}
	
	public Map<String, Object> selectRegistered(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);

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
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);

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
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);

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
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);

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

	private Specification<Sample> getCompleteDateWhere(Map<String, String> params) {
		if (params.containsKey("yyyymm")) {
			return SampleSpecification.customDateOneMonth("anlsCmplDate", params);
		} else {
			return SampleSpecification.customDateBetween("anlsCmplDate", params);
		}
	}

	private Specification<Sample> getOutputCmplDateWhere(Map<String, String> params) {
		if (params.containsKey("yyyymm")) {
			return SampleSpecification.customDateOneMonth("outputCmplDate", params);
		} else {
			return SampleSpecification.customDateBetween("outputCmplDate", params);
		}
	}
	
	private Specification<Sample> getRegisteredWhere(Map<String, String> params) {
		
		return Specification
			.where(getCreateDateWhere(params))
			.and(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.hNameIn(params))
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.statusCodeGt(20));
		
	}
	
	private Specification<Sample> getModifiedWhere(Map<String, String> params) {
		List<StatusCode> sc = Lists.newArrayList();
		sc.addAll(STS_EXP);
		sc.addAll(STS_JDGM);
		return Specification
			.where(getModifiedDateWhere(params))
			.and(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.hNameIn(params))
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.statusIn(sc));
	}
	
	private Specification<Sample> getAnalysisWhere(Map<String, String> params) {
		return Specification
			.where(getModifiedDateWhere(params))
			.and(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.hNameIn(params))
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.statusIn(STS_EXP));
	}
	
	private Specification<Sample> getCompletedWhere(Map<String, String> params) {
		return Specification
			.where(getCompleteDateWhere(params))
			.and(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.hNameIn(params))
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.statusIn(STS_JDGM));
	}
	
	private Specification<Sample> getReportedWhere(Map<String, String> params) {
		return Specification
			.where(getOutputCmplDateWhere(params))
				.and(SampleSpecification.isLastVersionTrue())
				.and(SampleSpecification.bundleId(params))
				.and(SampleSpecification.hNameIn(params))
				.and(SampleSpecification.isNotTest())
				.and(SampleSpecification.bundleIsActive())
				.and(SampleSpecification.statusIn(STS_REPORT));
	}
	
	private Map<String, Long> getGroupingMap(List<Map<String, String>> map, String key) {
		return map.stream().collect(
			Collectors.groupingBy(m -> m.get(key), Collectors.counting())
		);
	}
	
}
