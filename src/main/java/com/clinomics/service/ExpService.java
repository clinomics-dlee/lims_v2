package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.transaction.Transactional;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ChipTypeCode;
import com.clinomics.enums.GenotypingMethodCode;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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

	public Map<String, String> startExp(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {

			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}

		List<Sample> savedSamples = new ArrayList<Sample>();
		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			if (!sample.getStatusCode().equals(StatusCode.S200_EXP_READY)) {
				rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
				rtn.put("message", "상태값이 다른 검체가 존재합니다.[" + sample.getLaboratoryId() + "]");
				return rtn;
			}

			sample.setStatusCode(StatusCode.S210_EXP_STEP1);
			sample.setExpStartDate(now);
			sample.setExpStartMember(member);

			savedSamples.add(sample);
		}

		/* 
		 * #. Transactional 어노테이션 사용시 repository.save 하기 전에 이미 DB를 변경하고 중간에 return을 해도 
		 * 이미 업데이트된 목록은 저장되어 롤백 되지 않음. 그래서 Transactional 어노테이션 제거하고 목록을 나중에 saveall 하는 방식 사용
		 */

		sampleRepository.saveAll(savedSamples);

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

	public Map<String, Object> findSampleDataBySampleId(String id) {
		Optional<Sample> oSample = sampleRepository.findById(NumberUtils.toInt(id));
		Sample sample = oSample.orElse(new Sample());
		
		Map<String, Object> resultData = sample.getData();

		TreeMap<String, Object> tm = new TreeMap<String, Object>(resultData);
		Iterator<String> iteratorKey = tm.keySet().iterator();   //키값 오름차순 정렬(기본)
		List<Map<String, String>> datas = new ArrayList<Map<String, String>>();
		while(iteratorKey.hasNext()) {
			String key = iteratorKey.next();
			Map<String, String> map = Maps.newHashMap();
			map.put("marker", key);
			map.put("value", (String)resultData.get(key));
			datas.add(map);
		}

		Map<String, Object> rtn = Maps.newHashMap();
		rtn.put("sample", sample);
		rtn.put("datas", datas);
		
		return rtn;
	}

	@Transactional
	public Map<String, String> updateDnaQcInfo(Map<String, String> datas, String userId) {
		Map<String, String> rtn = Maps.newHashMap();

		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}

		int id = NumberUtils.toInt(datas.get("id"), 0);
		String a260280 = datas.get("a260280");
		String cncnt = datas.get("cncnt");
		String dnaQc = (StringUtils.isEmpty((String)datas.get("dnaQc")) ? "PASS" : (String)datas.get("dnaQc"));

		Optional<Sample> oSample = sampleRepository.findById(id);
		Sample sample = oSample.orElseThrow(NullPointerException::new);
		if (!NumberUtils.isCreatable(a260280) || !NumberUtils.isCreatable(cncnt)) {
			logger.info(">> A 260/280 and concentration can only be entered in numbers=" + id);
			rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
			rtn.put("message", "A 260/280, 농도는 숫자만 입력가능합니다.[" + sample.getLaboratoryId() + "]");
			return rtn;
		}

		sample.setA260280(a260280);
		sample.setCncnt(cncnt);
		sample.setDnaQc(dnaQc);

		sampleRepository.save(sample);

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, String> completeStep1(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}

		List<Sample> savedSamples = new ArrayList<Sample>();
		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			if (!sample.getStatusCode().equals(StatusCode.S210_EXP_STEP1)) {
				rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
				rtn.put("message", "상태값이 다른 검체가 존재합니다.[" + sample.getLaboratoryId() + "]");
				return rtn;
			}

			sample.setStatusCode(StatusCode.S220_EXP_STEP2);
			sample.setExpStep1Date(now);
			sample.setExpStep1Member(member);

			savedSamples.add(sample);
		}
		
		/* 
		 * #. Transactional 어노테이션 사용시 repository.save 하기 전에 이미 DB를 변경하고 중간에 return을 해도 
		 * 이미 업데이트된 목록은 저장되어 롤백 되지 않음. 그래서 Transactional 어노테이션 제거하고 목록을 나중에 saveall 하는 방식 사용
		 */
		sampleRepository.saveAll(savedSamples);

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

	public Map<String, String> updateQrtPcr(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();

		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}

		List<Sample> savedSamples = new ArrayList<Sample>();
		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setMappingNo(null);
			sample.setWellPosition(null);
			sample.setGenotypingMethodCode(GenotypingMethodCode.QRT_PCR);

			savedSamples.add(sample);
		}

		sampleRepository.saveAll(savedSamples);

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> saveAllMapping(List<Map<String, String>> datas, String userId) {
		Map<String, Object> rtn = Maps.newHashMap();

		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}
		
		List<Sample> savedSamples = new ArrayList<Sample>();
		for (Map<String, String> data : datas) {
			String mappingNo = data.get("mappingNo");
			String genotypingId = data.get("genotypingId");
			String wellPosition = data.get("wellPosition");

			// #. 입력하려는 MappingNo가 STEP2가 아닌 다른상태의 검체에 동일한 값이 존재하면 리턴
			Specification<Sample> w = Specification
						.where(SampleSpecification.mappingNoEqual(mappingNo))
						.and(SampleSpecification.statusNotEqual(StatusCode.S220_EXP_STEP2));
			List<Sample> ss = sampleRepository.findAll(w);
			if (ss.size() > 0) {
				rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
				rtn.put("message", "해당 Mapping No는 이미 사용중입니다.[" + mappingNo + "]");
				return rtn;
			}

			if (genotypingId.length() > 0 && wellPosition.length() > 0) {
				String[] genotypingInfo = genotypingId.split("-V");
				// #. genotypingId양식이 틀린경우
				if (genotypingInfo.length != 2) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "Genotyping ID 값을 확인해주세요.[" + genotypingId + "]");
					return rtn;
				}
	
				String laboratoryId = genotypingInfo[0];
				// #. version값이 숫자가아닌경우
				if (!NumberUtils.isCreatable(genotypingInfo[1])) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "Genotyping ID Version 값을 확인해주세요.[" + genotypingId + "]");
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
					rtn.put("message", "조회된 Genotyping ID 값이 없습니다.[" + genotypingId + "]");
					return rtn;
				}
				// #. 조회된 검체의 상태가 STEP2가 아닌경우
				if (!s.getStatusCode().equals(StatusCode.S220_EXP_STEP2)) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "이미 실험이 진행중인 검체입니다.[" + genotypingId + "]");
					return rtn;
				}
	
				s.setGenotypingMethodCode(GenotypingMethodCode.CHIP);
				s.setWellPosition(wellPosition);
				s.setMappingNo(mappingNo);
	
				savedSamples.add(s);
			}
		}
		
		sampleRepository.saveAll(savedSamples);

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, String> completeStep2(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}

		List<Sample> savedSamples = new ArrayList<Sample>();
		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			if (!sample.getStatusCode().equals(StatusCode.S220_EXP_STEP2)) {
				rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
				rtn.put("message", "상태값이 다른 검체가 존재합니다.[" + sample.getLaboratoryId() + "]");
				return rtn;
			}

			// #. GenotypingMethodCode 가 chip인 경우 step3으로, QRT_PCR인 경우 분석 성공으로 처리
			if (GenotypingMethodCode.CHIP.equals(sample.getGenotypingMethodCode())) {
				sample.setStatusCode(StatusCode.S230_EXP_STEP3);
			} else if (GenotypingMethodCode.QRT_PCR.equals(sample.getGenotypingMethodCode())) {
				sample.setStatusCode(StatusCode.S420_ANLS_SUCC);
			}
			sample.setExpStep2Date(now);
			sample.setExpStep2Member(member);

			savedSamples.add(sample);
		}
		
		sampleRepository.saveAll(savedSamples);
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

	public Map<String, String> updateChipInfos(List<Map<String, String>> datas, String userId) {
		Map<String, String> rtn = Maps.newHashMap();

		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}

		List<Sample> savedSamples = new ArrayList<Sample>();
		for (Map<String, String> data : datas) {
			String mappingNo = data.get("mappingNo");
			String beforeChipBarcode = StringUtils.stripToEmpty(data.get("beforeChipBarcode"));
			String chipBarcode = StringUtils.stripToEmpty(data.get("chipBarcode"));
			String chipTypeCodeKey = StringUtils.stripToEmpty(data.get("chipTypeCode"));
			ChipTypeCode chipTypeCode = null;
			for (ChipTypeCode code : ChipTypeCode.values()) {
				if (chipTypeCodeKey.equals(code.getKey())) {
					chipTypeCode = code;
				}
			}

			Specification<Sample> w = Specification
					.where(SampleSpecification.mappingInfoGroupBy())
					.and(SampleSpecification.chipBarcodeEqual(chipBarcode));
			List<Sample> ss = sampleRepository.findAll(w);

			// #. 수정전 값과 수정후 값이 동일한 경우 해당 카운트 추가
			int cnt = 0;
			if (beforeChipBarcode.equals(chipBarcode)) cnt = 1;
			
			if (ss.size() > cnt) {
				rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
				rtn.put("message", "이미 등록된 Chip Barcode 입니다.[" + chipBarcode + "]");
				return rtn;
			}
	
			Specification<Sample> where = Specification.where(SampleSpecification.mappingNoEqual(mappingNo));
			List<Sample> samples = sampleRepository.findAll(where);
			for (Sample sample : samples) {
				sample.setChipBarcode(chipBarcode);
				sample.setChipTypeCode(chipTypeCode);
			}

			savedSamples.addAll(samples);
		}
		
		sampleRepository.saveAll(savedSamples);
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, String> completeStep3(List<String> mappingNos, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}

		List<Sample> savedSamples = new ArrayList<Sample>();
		for (String mappingNo : mappingNos) {
			Specification<Sample> where = Specification.where(SampleSpecification.mappingNoEqual(mappingNo));
			List<Sample> samples = sampleRepository.findAll(where);

			for (Sample sample : samples) {
				if (!sample.getStatusCode().equals(StatusCode.S230_EXP_STEP3)) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
					rtn.put("message", "상태값이 다른 검체가 존재합니다.[" + sample.getLaboratoryId() + "]");
					return rtn;
				}

				sample.setModifiedDate(now);
				sample.setExpStep3Date(now);
				sample.setExpStep3Member(member);
				sample.setStatusCode(StatusCode.S400_ANLS_READY);
			}
			savedSamples.addAll(samples);
		}
		
		sampleRepository.saveAll(savedSamples);
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
