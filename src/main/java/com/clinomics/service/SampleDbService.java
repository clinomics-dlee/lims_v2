package com.clinomics.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleHistory;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleHistoryRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.ResultSpecification;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.CustomIndexPublisher;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.BooleanUtils;
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

@Service
public class SampleDbService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	SampleHistoryRepository sampleHistoryRepository;

	@Autowired
	BundleRepository bundleRepository;
	
	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	DataTableService dataTableService;
	
	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	CustomIndexPublisher customIndexPublisher;

	public Map<String, Object> getSampleDbList(Map<String, String> params) {
		logger.info("getDbList Params=" + params.toString());
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		
		List<Order> orders = Arrays.asList(new Order[] { Order.desc("createdDate"), Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;

		List<Map<String, Object>> sList = new ArrayList<Map<String, Object>>();
		// for (Sample s : list) {
		// 	String id = s.getId();
		// 	Map<String, Object> map = Maps.newHashMap();
		// 	map.put("sample", s);
		// 	// Result r = resultRepository.getOne(NumberUtils.toInt(id));

		// 	Specification<Result> w = Specification.where(ResultSpecification.lastResult(id));
		// 	List<Result> rs = resultRepository.findAll(w);
		// 	Result r = new Result();
		// 	if (rs.size() > 0) {
		// 		r = rs.get(0);
		// 	}
		// 	map.put("result", r);
		// 	sList.add(map);
		// }

		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrderingForMap(sList, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));

		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, sList, header);
	}

	public Map<String, Object> getSampleHistory(Map<String, String> params, String id) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		
		long total = sampleHistoryRepository.countBySample_Id(id);
		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		List<SampleHistory> list = sampleHistoryRepository.findBySample_Id(id, pageable);
		sampleItemService.filterItemsAndOrdering(list);
		long filtered = total + 1;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
	}

	public Map<String, Object> find(Map<String, String> params, int statusCodeNumber) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		
		List<Order> orders = Arrays.asList(new Order[] { Order.desc("createdDate"), Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusCodeGt(statusCodeNumber));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}
}
