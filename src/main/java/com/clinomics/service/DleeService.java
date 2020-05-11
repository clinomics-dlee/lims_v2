package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.DleeRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.specification.lims.DleeSpecification;
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
public class DleeService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	DleeRepository dleeRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	DataTableService dataTableService;

	public Map<String, Object> findSampleByExpRdyStatus(Map<String, String> params) {
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
					.where(DleeSpecification.betweenDate(params))
					.and(DleeSpecification.bundleId(params))
					.and(DleeSpecification.keywordLike(params))
					.and(DleeSpecification.existsStatusIn(Arrays.asList(StatusCode.EXP_READY)));
					
		
		total = dleeRepository.count(where);
		Page<Sample> page = dleeRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> startExp(List<String> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		logger.info("★★★★★★★★★ sampleIds=" + sampleIds);
		LocalDateTime nnow = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (String id : sampleIds) {
			Optional<Sample> oSample = dleeRepository.findById(NumberUtils.toInt(id));
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setStatusCode(StatusCode.EXP_STEP1);
			sample.setExpStartDate(nnow);
			sample.setExpStep1Member(member);

			dleeRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}
}
