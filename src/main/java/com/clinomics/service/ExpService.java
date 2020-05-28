package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ChipTypeCode;
import com.clinomics.enums.GenotypingMethodCode;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
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
public class ExpService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	SampleRepository sampleRepository;

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
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.statusEqual(StatusCode.S200_EXP_READY));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> startExp(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setStatusCode(StatusCode.S210_EXP_STEP1);
			sample.setExpStartDate(now);
			sample.setExpStartMember(member);

			sampleRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findSampleByExpStep1Status(Map<String, String> params) {
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
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.statusEqual(StatusCode.S210_EXP_STEP1));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> completeStep1(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setStatusCode(StatusCode.S220_EXP_STEP2);
			sample.setExpStep1Date(now);
			sample.setExpStep1Member(member);

			sampleRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findSampleByExpStep2Status(Map<String, String> params) {
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
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.statusEqual(StatusCode.S220_EXP_STEP2));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> updateQrtPcr(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();

		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setGenotypingMethodCode(GenotypingMethodCode.QRT_PCR);

			sampleRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	@Transactional
	public Map<String, Object> saveAllMapping(List<Map<String, String>> datas, String userId) {
		Map<String, Object> rtn = Maps.newHashMap();
		
		for (Map<String, String> data : datas) {
			String mappingNo = data.get("mappingNo");
			String genotypingId = data.get("genotypingId");
			String wellPosition = data.get("wellPosition");

			if (genotypingId.length() > 0 && wellPosition.length() > 0) {
				String[] genotypingInfo = genotypingId.split("-V");
				// #. genotypingId양식이 틀린경우
				if (genotypingInfo.length != 2) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					return rtn;
				}
	
				String laboratoryId = genotypingInfo[0];
				// #. version값이 숫자가아닌경우
				if (!NumberUtils.isCreatable(genotypingInfo[1])) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					return rtn;
				}
				int version = NumberUtils.toInt(genotypingInfo[1]);
				
				Specification<Sample> where = Specification
						.where(SampleSpecification.laboratoryIdEqual(laboratoryId))
						.and(SampleSpecification.versionEqual(version));
				List<Sample> samples = sampleRepository.findAll(where);
				Sample s = samples.get(0);
				// #. 검사실ID 또는 version이 잘못 입력된 경우
				if (s == null) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					return rtn;
				}
				// #. 조회된 검체의 상태가 STEP2가 아닌경우
				if (!s.getStatusCode().equals(StatusCode.S220_EXP_STEP2)) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					return rtn;
				}
	
				s.setGenotypingMethodCode(GenotypingMethodCode.CHIP);
				s.setWellPosition(wellPosition);
				s.setMappingNo(mappingNo);
	
				sampleRepository.save(s);
			}
		}
		
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	@Transactional
	public Map<String, String> completeStep2(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			// #. GenotypingMethodCode 가 chip인 경우 step3으로, QRT_PCR인 경우 분석 성공으로 처리
			if (GenotypingMethodCode.CHIP.equals(sample.getGenotypingMethodCode())) {
				sample.setStatusCode(StatusCode.S230_EXP_STEP3);
			} else if (GenotypingMethodCode.QRT_PCR.equals(sample.getGenotypingMethodCode())) {
				sample.setStatusCode(StatusCode.S420_ANLS_SUCC);
			}
			sample.setExpStep2Date(now);
			sample.setExpStep2Member(member);

			sampleRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findMappingInfosByExpStep3Status(Map<String, String> params) {
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
					.where(SampleSpecification.mappingInfoGroupBy())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusEqual(StatusCode.S230_EXP_STEP3))
					.and(SampleSpecification.mappingInfoLike(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> updateChipInfo(Map<String, String> datas, String userId) {
		Map<String, String> rtn = Maps.newHashMap();

		String mappingNo = datas.get("mappingNo");
		String chipBarcode = datas.get("chipBarcode");
		String chipTypeCodeKey = datas.get("chipTypeCode");
		ChipTypeCode chipTypeCode = null;
		for (ChipTypeCode code : ChipTypeCode.values()) {
			if (chipTypeCodeKey.equals(code.getKey())) {
				chipTypeCode = code;
			}
		}

		Specification<Sample> where = Specification.where(SampleSpecification.mappingNoEqual(mappingNo));
		List<Sample> samples = sampleRepository.findAll(where);

		for (Sample sample : samples) {
			sample.setChipBarcode(chipBarcode);
			sample.setChipTypeCode(chipTypeCode);
		}
		sampleRepository.saveAll(samples);

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	@Transactional
	public Map<String, String> updateChipInfos(List<Map<String, String>> datas, String userId) {
		Map<String, String> rtn = Maps.newHashMap();

		for (Map<String, String> data : datas) {
			String mappingNo = data.get("mappingNo");
			String chipBarcode = data.get("chipBarcode");
			String chipTypeCodeKey = data.get("chipTypeCode");
			ChipTypeCode chipTypeCode = null;
			for (ChipTypeCode code : ChipTypeCode.values()) {
				if (chipTypeCodeKey.equals(code.getKey())) {
					chipTypeCode = code;
				}
			}
	
			Specification<Sample> where = Specification.where(SampleSpecification.mappingNoEqual(mappingNo));
			List<Sample> samples = sampleRepository.findAll(where);
	
			for (Sample sample : samples) {
				sample.setChipBarcode(chipBarcode);
				sample.setChipTypeCode(chipTypeCode);
			}

			sampleRepository.saveAll(samples);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	@Transactional
	public Map<String, String> completeStep3(List<String> mappingNos, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (String mappingNo : mappingNos) {
			Specification<Sample> where = Specification.where(SampleSpecification.mappingNoEqual(mappingNo));
			List<Sample> samples = sampleRepository.findAll(where);

			for (Sample sample : samples) {
				sample.setExpStep3Date(now);
				sample.setExpStep3Member(member);
				sample.setStatusCode(StatusCode.S400_ANLS_READY);
			}
			sampleRepository.saveAll(samples);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findMappingInfosForDb(Map<String, String> params) {
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
					.where(SampleSpecification.mappingInfoGroupBy())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.mappingInfoLike(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, Object> findMappingSample(Map<String, String> params, String mappingNo) {
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
					.where(SampleSpecification.mappingNoEqual(mappingNo))
					.and(SampleSpecification.bundleIsActive());
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}
}
