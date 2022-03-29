package com.clinomics.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.MountWorkerCode;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.service.async.AnalysisService;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.FileUtil;
import com.google.common.collect.Maps;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AnlsService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${lims.workspacePath}")
	private String workspacePath;

	@Value("${titan.ftp.address}")
	private String ftpAddress;
	
	@Value("${titan.ftp.port}")
	private int ftpPort;

	@Value("${titan.ftp.username}")
	private String ftpUsername;

	@Value("${titan.ftp.password}")
	private String ftpPassword;

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
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);
		
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount);
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.mappingInfoGroupBy())
					.and(SampleSpecification.isNotTest())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusEqual(StatusCode.S400_ANLS_READY))
					.and(SampleSpecification.mappingInfoLike(params))
					.and(SampleSpecification.orderBy(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrderingForExpAnls(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, Object> findCelFiles(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		
		String chipBarcode = params.get("chipBarcode");
		
		List<Map<String, Object>> lstMapCelFiles = new ArrayList<>();
		// #. mount 위치 경로
		for (MountWorkerCode code : MountWorkerCode.values()) {
			File celDir = new File(code.getValue());
			logger.info("★★★ code.getValue()=[" + code.getValue() + "]");
			logger.info("★★★ celDir=[" + celDir + "]");
			logger.info("★★★ celDir.list()=[" + celDir.list() + "]");
			if (celDir.list() != null) {
				for (String fileName : celDir.list()) {
					if (fileName.indexOf("_") > -1) {
						String filePrefix = fileName.substring(0, fileName.indexOf("_"));
						String ext = FileUtil.getFileNameExt(fileName);
	
						// #. 파일명 검색
						if ("CEL".equals(ext) && filePrefix.equals(chipBarcode)) {
							Map<String, Object> map = Maps.newHashMap();
							map.put("fileName", fileName);
							lstMapCelFiles.add(map);
						}
					}
				}
			}
		}

		long total = lstMapCelFiles.size();
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, lstMapCelFiles);
	}

	public Map<String, String> startAnls(List<String> mappingNos, String userId) {
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

		// #. 마운트 장비 파일 목록 확인용 로그 추가
		for (MountWorkerCode code : MountWorkerCode.values()) {
			File celDir = new File(code.getValue());
			logger.info("★★★ code.getValue()=[" + code.getValue() + "]");
			logger.info("★★★ celDir=[" + celDir + "]");
			logger.info("★★★ celDir.list()=[" + celDir.list() + "]");
			logger.info("★★★ celDir.list().length=[" + celDir.list().length + "]");
		}

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
				sample.setModifiedDate(now);
				sample.setAnlsStartMember(member);
				sample.setStatusCode(StatusCode.S410_ANLS_RUNNING);
			}
			// #. sample 저장
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
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);
		
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount);
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.isNotTest())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.statusIn(Arrays.asList(new StatusCode[] {StatusCode.S410_ANLS_RUNNING, StatusCode.S430_ANLS_FAIL})))
					.and(SampleSpecification.orderBy(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrderingForExpAnls(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, Object> findChipBarcodeByAllFail(Map<String, String> params) {
		Specification<Sample> where = Specification
					.where(SampleSpecification.mappingInfoGroupBy())
					.and(SampleSpecification.isNotTest())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusIn(Arrays.asList(new StatusCode[] {StatusCode.S430_ANLS_FAIL})))
					.and(SampleSpecification.orderBy(params));
		
		List<Sample> list = sampleRepository.findAll(where);

		List<String> chipBarcodes = new ArrayList<String>();
		for (Sample s : list) {
			String chipBarcode = s.getChipBarcode();
			Specification<Sample> w = Specification
					.where(SampleSpecification.chipBarcodeEqual(chipBarcode))
					.and(SampleSpecification.isNotTest())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusNotEqual(StatusCode.S430_ANLS_FAIL));
		
			List<Sample> l = sampleRepository.findAll(w);

			if (l.size() < 1) {
				chipBarcodes.add(chipBarcode);
			}
		}
		
		Map<String, Object> rtn = Maps.newHashMap();
		rtn.put("chipBarcodes", chipBarcodes);
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, String> saveRdyStatus(Map<String, String> datas, String userId) {
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

		String chipBarcode = datas.get("chipBarcode");
		Specification<Sample> where = Specification
				.where(SampleSpecification.chipBarcodeEqual(chipBarcode))
				.and(SampleSpecification.isNotTest())
				.and(SampleSpecification.bundleIsActive())
				.and(SampleSpecification.statusEqual(StatusCode.S430_ANLS_FAIL));
	
		List<Sample> list = sampleRepository.findAll(where);
		
		// #. 파일 경로 삭제
		Sample s = list.get(0);
		String filePath = s.getFilePath();
		File dir = new File(filePath);
		try {
			FileUtils.deleteDirectory(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (Sample sample : list) {
			sample.setCheckCelFile(null);
			sample.setFilePath(null);
			sample.setFileName(null);
			sample.setAnlsStartDate(null);
			sample.setModifiedDate(now);
			sample.setAnlsStartMember(null);
			sample.setStatusMessage(null);
			sample.setAnlsEndDate(null);
			sample.setStatusCode(StatusCode.S400_ANLS_READY);
		}

		sampleRepository.saveAll(list);
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, String> reExpReg(List<Integer> sampleIds, String userId) {
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
			// #. 검체 상태 변경
			if (sample.getStatusCode().equals(StatusCode.S430_ANLS_FAIL)) {
				sample.setStatusCode(StatusCode.S450_ANLS_FAIL_CMPL);
			} else if (sample.getStatusCode().equals(StatusCode.S420_ANLS_SUCC)) {
				sample.setStatusCode(StatusCode.S440_ANLS_SUCC_CMPL);
			}
			sample.setModifiedDate(now);
			sample.setAnlsCmplDate(now);
			sample.setAnlsCmplMember(member);
			sample.setLastVersion(false);

			savedSamples.add(sample);

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
			nSample.setModifiedDate(sample.getModifiedDate());
			nSample.setCollectedDate(sample.getCollectedDate());
			nSample.setReceivedDate(sample.getReceivedDate());
			nSample.setSampleType(sample.getSampleType());

			savedSamples.add(nSample);
		}

		sampleRepository.saveAll(savedSamples);
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findSampleByAnlsSuccStatus(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);
		
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount);
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.isNotTest())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.statusEqual(StatusCode.S420_ANLS_SUCC))
					.and(SampleSpecification.orderBy(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrderingForExpAnls(list);
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

	public Map<String, String> completeAnls(List<Integer> sampleIds, String userId) {
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

			if (!sample.getStatusCode().equals(StatusCode.S420_ANLS_SUCC)) {
				rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
				rtn.put("message", "상태값이 다른 검체가 존재합니다.[" + sample.getLaboratoryId() + "]");
				return rtn;
			}

			sample.setJdgmApproveDate(now);
			sample.setJdgmApproveMember(member);

			sample.setModifiedDate(now);
			sample.setStatusCode(StatusCode.S460_ANLS_CMPL);
			sample.setAnlsCmplDate(now);
			sample.setAnlsCmplMember(member);

			savedSamples.add(sample);
		}

		sampleRepository.saveAll(savedSamples);

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}
}
