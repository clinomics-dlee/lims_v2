package com.clinomics.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import com.google.common.io.Files;

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

		List<String> failMapplingNos = new ArrayList<String>();
		for (String mappingNo : mappingNos) {
			Specification<Sample> where = Specification.where(SampleSpecification.mappingNoEqual(mappingNo));
			List<Sample> samples = sampleRepository.findAll(where);
			String chipBarcode = samples.get(0).getChipBarcode();
			String filePath = workspacePath + "/" + chipBarcode;
			String chipDesc = samples.get(0).getChipTypeCode().getDesc();
			File path = new File(filePath);
			if (!path.exists()) path.mkdir();

			// #. Cell File 서버로 가져오기
			boolean isCompleteCopy = this.copyCelFiles(samples);

			if (isCompleteCopy) {
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
			} else {
				failMapplingNos.add(mappingNo);
			}
		}
		
		if (failMapplingNos.size() > 0) {
			rtn.put("result", "warning");
			rtn.put("message", "파일 복사중 오류가 발생하였습니다." + failMapplingNos.toString());
		} else {
			rtn.put("result", ResultCode.SUCCESS.get());
		}
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
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusIn(Arrays.asList(new StatusCode[] {StatusCode.S430_ANLS_FAIL})))
					.and(SampleSpecification.orderBy(params));
		
		List<Sample> list = sampleRepository.findAll(where);

		List<String> chipBarcodes = new ArrayList<String>();
		for (Sample s : list) {
			String chipBarcode = s.getChipBarcode();
			Specification<Sample> w = Specification
					.where(SampleSpecification.chipBarcodeEqual(chipBarcode))
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

	/**
	 * mount된 경로에서 파일을 복사한다.
	 * @param samples
	 */
	private boolean copyCelFiles(List<Sample> samples) {
		boolean isCompleteCopy = false;
		logger.info("★★★★★★★★★★ Start copyCelFiles");
		for (Sample sample : samples) {
			File sourceFile = null;

			// #. 마운트 장비에서 해당 샘플에 cel 파일이 존재하는지 확인
			logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "]fileName=" + sample.getFileName());
			for (MountWorkerCode code : MountWorkerCode.values()) {
				File dir = new File(code.getValue());
				// #. 파일이 존재한다면 sourceFile에 셋팅
				if (Arrays.asList(dir.list()).contains(sample.getFileName())) {
					sourceFile = new File(code.getValue(), sample.getFileName());
					break;
				}
			}

			// #. 파일이 존재한다면 카피 진행
			if (sourceFile != null) {
				logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "] sourceFile size=[" + sourceFile.length() + " byte]");
				File copyFile = new File(sample.getFilePath(), sample.getFileName());

				try {
					Files.copy(sourceFile, copyFile);
					// #. file 복사 확인
					logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "] copyFile size=[" + copyFile.length() + " byte]");
					if (Files.asByteSource(sourceFile).contentEquals(Files.asByteSource(copyFile))) {
						isCompleteCopy = true;
						sample.setCheckCelFile("PASS");
						sampleRepository.save(sample);
						logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "] success copy");
					} else {
						// #. 파일 복사가 잘못된 경우 
						logger.info("★★★★★★★ There was a problem copying the file=" + sample.getLaboratoryId());
						sample.setCheckCelFile("FAIL");
						sampleRepository.save(sample);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// #. 파일이 없는경우 
				logger.info("★★★★★★★ Not Found File=" + sample.getLaboratoryId());
				sample.setCheckCelFile("FAIL");
				sampleRepository.save(sample);
			}
		}
		logger.info("★★★★★★★★★★ finish copyCelFiles");
		return isCompleteCopy;
	}
}
