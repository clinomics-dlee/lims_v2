package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.CustomIndexPublisher;
import com.google.common.collect.Maps;

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

@Service
public class JdgmService {

	@Autowired
    SampleRepository sampleRepository;
    
	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	DataTableService dataTableService;
	
	@Autowired
	SampleItemService sampleItemService;

	@Autowired
    CustomIndexPublisher customIndexPublisher;
    
    public Map<String, Object> find(Map<String, String> params, List<StatusCode> statusCodes) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 0);
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
					.and(SampleSpecification.statusIn(statusCodes));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}
    
    @Transactional
	public Map<String, String> approve(List<Integer> ids, String memberId) {
		Map<String, String> rtn = Maps.newHashMap();
		List<Sample> samples = sampleRepository.findByIdIn(ids);
		
		// sample.set
		Optional<Member> oMember = memberRepository.findById(memberId);
		Member member = oMember.get();
		LocalDateTime now = LocalDateTime.now();
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		rtn.put("result", ResultCode.SUCCESS_APPROVED.get());

		if (roles.contains(RoleCode.ROLE_EXP_80.toString())) {
			
			samples.stream().forEach(s -> {
				s.setJdgmDrctApproveDate(now);
				s.setJdgmDrctApproveMember(member);
				if (s.getJdgmApproveDate() != null && s.getJdgmMngApproveDate() != null && s.getJdgmDrctApproveDate() != null) {
					s.setStatusCode(StatusCode.S600_JDGM_APPROVE);
				}
			});

		} else if (roles.contains(RoleCode.ROLE_EXP_40.toString())) {
			
			samples.stream().forEach(s -> {
				s.setJdgmMngApproveDate(now);
				s.setJdgmMngApproveMember(member);
				if (s.getJdgmApproveDate() != null && s.getJdgmMngApproveDate() != null && s.getJdgmDrctApproveDate() != null) {
					s.setStatusCode(StatusCode.S600_JDGM_APPROVE);
				}
			});

		} else if (roles.contains(RoleCode.ROLE_EXP_20.toString())) {
			
			samples.stream().forEach(s -> {
				s.setJdgmApproveDate(now);
				s.setJdgmApproveMember(member);
				if (s.getJdgmApproveDate() != null && s.getJdgmMngApproveDate() != null && s.getJdgmDrctApproveDate() != null) {
					s.setStatusCode(StatusCode.S600_JDGM_APPROVE);
				}
			});
		} else {
			rtn.put("result", ResultCode.NO_PERMISSION.get());
		}
		sampleRepository.saveAll(samples);
		
		return rtn;
	}
}