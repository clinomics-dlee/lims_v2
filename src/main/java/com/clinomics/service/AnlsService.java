package com.clinomics.service;

import java.io.File;
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
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.service.async.AnalysisService;
import com.clinomics.specification.lims.SampleSpecification;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AnlsService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${lims.workspacePath}")
	private String workspacePath;

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	BundleRepository bundleRepository;
	
	@Autowired
	MemberRepository memberRepository;

	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	AnalysisService analysisService;

	@Autowired
	DataTableService dataTableService;

	public Map<String, Object> findMappingSampleByAnlsRdyStatus(Map<String, String> params) {
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
					.and(SampleSpecification.statusEqual(StatusCode.S400_ANLS_READY))
					.and(SampleSpecification.mappingInfoLike(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, String> startAnls(List<String> mappingNos, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (String mappingNo : mappingNos) {
			Specification<Sample> where = Specification.where(SampleSpecification.mappingNoEqual(mappingNo));
			List<Sample> samples = sampleRepository.findAll(where);
			String chipBarcode = samples.get(0).getChipBarcode();
			String filePath = workspacePath + "/" + chipBarcode;
			String chipDesc = samples.get(0).getChipTypeCode().getDesc();
			File path = new File(filePath);
			if (!path.exists()) path.mkdir();

			for (Sample sample : samples) {
				// #. sample 분석관련값 셋팅
				sample.setFilePath(filePath);
				sample.setFileName(chipBarcode + "_" + chipDesc + "_" + sample.getWellPosition() + ".CEL");
				sample.setAnlsStartDate(now);
				sample.setAnlsStartMember(member);
				sample.setStatusCode(StatusCode.S410_ANLS_RUNNING);
			}
			sampleRepository.saveAll(samples);

			// #. 가져와서 분석 실행하기
			analysisService.doPythonAnalysis(samples);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findSampleByAnlsSttsStatus(Map<String, String> params) {
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
					.and(SampleSpecification.statusIn(Arrays.asList(new StatusCode[] {StatusCode.S410_ANLS_RUNNING, StatusCode.S430_ANLS_FAIL})));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> reExpReg(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);
			// #. 검체 상태 변경
			if (sample.getStatusCode().equals(StatusCode.S430_ANLS_FAIL)) {
				sample.setStatusCode(StatusCode.S450_ANLS_FAIL_CMPL);
			} else if (sample.getStatusCode().equals(StatusCode.S420_ANLS_SUCC)) {
				sample.setStatusCode(StatusCode.S440_ANLS_SUCC_CMPL);
			}
			sample.setAnlsCmplDate(now);
			sample.setAnlsCmplMember(member);

			sampleRepository.save(sample);

			// #. 해당 검체 복사 step2전까지만 사용한 데이터 복사
			Sample nSample = new Sample();
			nSample.setLaboratoryId(sample.getLaboratoryId());
			nSample.setVersion(sample.getVersion() + 1);
			nSample.setBundle(sample.getBundle());
			nSample.setItems(sample.getItems());
			nSample.setA260280(sample.getA260280());
			nSample.setCncnt(sample.getCncnt());
			nSample.setDnaQc(sample.getDnaQc());
			nSample.setStatusCode(StatusCode.S220_EXP_STEP2);
			nSample.setCreatedDate(sample.getCreatedDate());
			nSample.setCreatedMember(sample.getCreatedMember());
			nSample.setInputApproveDate(sample.getInputApproveDate());
			nSample.setInputApproveMember(sample.getInputApproveMember());
			nSample.setInputMngApproveDate(sample.getInputMngApproveDate());
			nSample.setInputMngApproveMember(sample.getInputMngApproveMember());
			nSample.setInputDrctApproveDate(sample.getInputDrctApproveDate());
			nSample.setInputDrctMember(sample.getInputDrctMember());
			nSample.setExpStartDate(sample.getExpStartDate());
			nSample.setExpStartMember(sample.getExpStartMember());
			nSample.setExpStep1Date(sample.getExpStep1Date());
			nSample.setExpStep1Member(sample.getExpStep1Member());

			sampleRepository.save(nSample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findSampleByAnlsSuccStatus(Map<String, String> params) {
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
					.and(SampleSpecification.statusEqual(StatusCode.S420_ANLS_SUCC));
		
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
	public Map<String, String> completeAnls(List<Integer> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime now = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (int id : sampleIds) {
			Optional<Sample> oSample = sampleRepository.findById(id);
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setStatusCode(StatusCode.S460_ANLS_CMPL);
			sample.setAnlsCmplDate(now);
			sample.setAnlsCmplMember(member);

			sampleRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}
}
