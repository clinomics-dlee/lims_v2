package com.clinomics.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Agency;
import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Product;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.AgencyRepository;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.CustomIndexPublisher;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Service
public class OutputService {

	@Autowired
    SampleRepository sampleRepository;
	
	@Autowired
	MemberRepository memberRepository;

	@Autowired
	AgencyRepository agencyRepository;

	@Autowired
	BundleRepository bundleRepository;
	
	@Autowired
	DataTableService dataTableService;
	
	@Autowired
	SampleItemService sampleItemService;
	
	@Autowired
	InputService inputService;

	@Autowired
	RoleService roleService;

	@Autowired
	CustomIndexPublisher customIndexPublisher;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public Map<String, Object> find(Map<String, String> params, List<StatusCode> statusCodes) {
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
					.where(SampleSpecification.betweenModifiedDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.isNotTest())
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusIn(statusCodes))
					.and(SampleSpecification.orderBy(params));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}
    
	@Transactional
	@CacheEvict(value = "apiCache", allEntries = true)
	public Map<String, String> outputApprove(List<Integer> ids, String memberId) {
		logger.info("☆☆☆☆☆☆☆ outputApprove ☆☆☆ Cache Evict ☆☆☆");
		Map<String, String> rtn = Maps.newHashMap();
		
		List<Sample> samples = sampleRepository.findByIdInAndStatusCodeIn(ids, Arrays.asList(new StatusCode[] { StatusCode.S600_JDGM_APPROVE }));
		
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
		rtn.put("message", ResultCode.SUCCESS_APPROVED.getMsg());
		
		if (roles.contains(RoleCode.ROLE_INPUT_20.toString())
				|| roles.contains(RoleCode.ROLE_OUTPUT_20.toString())
				|| roles.contains(RoleCode.ROLE_EXP_80.toString())) {
			
			samples.stream().forEach(s -> {
				StatusCode sc = s.getStatusCode();
				if (sc.equals(StatusCode.S600_JDGM_APPROVE)) {

					s.setOutputWaitDate(now);
					s.setOutputWaitMember(member);
					s.setModifiedDate(now);

					if (s.getBundle().isGenoData()) {
						// #. GenoData 검체는 출고완료 상태로 바로 설정
						s.setOutputCmplDate(now);
						s.setStatusCode(StatusCode.S710_OUTPUT_CMPL);
					} else {
						s.setStatusCode(StatusCode.S700_OUTPUT_WAIT);
					}
				}

			});

		} else {
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}
		sampleRepository.saveAll(samples);
		
		return rtn;
	}

	@Transactional
	@CacheEvict(value = "apiCache", allEntries = true)
	public Map<String, String> outputReIssue(Map<String, String> inputItems, String memberId) {
		logger.info("☆☆☆☆☆☆☆ outputReIssue ☆☆☆ Cache Evict ☆☆☆");

		Map<String, String> rtn = inputService.save(inputItems, null, true);

		if ("00".equals(rtn.getOrDefault("result", ""))) {
			String id = inputItems.getOrDefault("id", "0") + "";
		
			Sample sample = inputService.searchExistsSample(NumberUtils.toInt(id));

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
			rtn.put("message", ResultCode.SUCCESS_APPROVED.getMsg());

			if (roles.contains(RoleCode.ROLE_INPUT_20.toString())
				|| roles.contains(RoleCode.ROLE_OUTPUT_20.toString())
				|| roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
				StatusCode sc = sample.getStatusCode();
				if (sc.equals(StatusCode.S710_OUTPUT_CMPL) || sc.equals(StatusCode.S810_RE_OUTPUT_CMPL)) {
					sample.setReOutputWaitDate(now);
					sample.setReOutputWaitMember(member);
					sample.setModifiedDate(now);
					
					if (sample.getBundle().isGenoData()) {
						// #. GenoData 검체는 재발행완료 상태로 바로 설정
						sample.setReOutputCmplDate(now);
						sample.setStatusCode(StatusCode.S810_RE_OUTPUT_CMPL);
					} else {
						sample.setStatusCode(StatusCode.S800_RE_OUTPUT_WAIT);
					}
				}

			} else {
				rtn.put("result", ResultCode.NO_PERMISSION.get());
				rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
				return rtn;
			}
			sampleRepository.save(sample);
		}
		
		return rtn;
	}

	@Transactional
	@Cacheable(value = "apiCache", key = "#params")
	public Map<String, Object> getResultsForRest(Map<String, String> params, String ip) {
		logger.info("☆☆☆☆☆☆☆ getResultsForRest ☆☆☆ DB Conn ☆☆☆ IN interface : " + params.toString());
		Map<String, Object> rtn = Maps.newHashMap();
		// #. productType 추가
		String productType = params.get("productType");
		
		Specification<Sample> where = Specification
				.where(SampleSpecification.productNotLike(params))
				.and(SampleSpecification.statusIn(Arrays.asList(StatusCode.S700_OUTPUT_WAIT, StatusCode.S800_RE_OUTPUT_WAIT)));

		List<Sample> samples = sampleRepository.findAll(where);

		List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		if (samples.size() > 0) {
			for (Sample sample : samples) {
				Set<String> productTypes = Sets.newHashSet();
				sample.getBundle().getProduct().stream().forEach(p -> {
					productTypes.add(p.getType());
				});

				if (productTypes.contains(productType)) {
					Map<String, Object> data = Maps.newHashMap();

					Map<String, Object> items = sample.getItems();
					if (sample.getBundle().isHospital()) {
						data.put("barcode", sample.getLaboratoryId());
						data.put("name", items.get("h_name"));
						data.put("tel", items.get("h_tel"));
						data.put("address", items.get("h_address"));

						// #. 출생연도 birthyear 값이 있는 경우는 api 호출시 birthday에 '01-01'을 고정으로 붙여서 보냄
						if (items.get("birthyear") != null && items.get("birthyear").toString().length() > 0) {
							String birthyear = (String)items.get("birthyear");
							birthyear = birthyear.trim() + "-01-01";

							data.put("birthday", birthyear);
						}

						// #. 설문지정보 값을 추가
						data.put("docinfos", sample.getDocInfos());
					}

					data.putAll(items);

					Map<String, Object> dataMap = Maps.newHashMap();
					dataMap.putAll(sample.getData());

					// #. bundle에 type이 GS, GP, GPH 중 하나라도 포함되어 있는 경우 15_VF_rs495366, 15_AJ_rs11226373 값이 없으면 추가
					if ("GS".equals(sample.getBundle().getType())
					 	|| "GP".equals(sample.getBundle().getType())
					 	|| "GPH".equals(sample.getBundle().getType())) {

						// #. 15_VF_rs495366, 15_AJ_rs11226373 값이 비어있는 경우 고정값 A/A를 추가
						if (StringUtils.isBlank((String)dataMap.get("15_VF_rs495366"))) {
							dataMap.put("15_VF_rs495366", "A/A");
						}

						if (StringUtils.isBlank((String)dataMap.get("15_AJ_rs11226373"))) {
							dataMap.put("15_AJ_rs11226373", "A/A");
						}
					}

					data.put("genedata", dataMap);
					data.put("experimentid", sample.getLaboratoryId());
					data.put("collecteddate", (sample.getCollectedDate() != null ? sample.getCollectedDate().format(formatter) : ""));
					data.put("receiveddate", (sample.getReceivedDate() != null ? sample.getReceivedDate().format(formatter) : ""));
					data.put("sampletype", sample.getSampleType());
					data.put("p_name", sample.getBundle().getName());
					
					datas.add(data);
					
					logger.info("☆☆☆☆☆☆☆☆☆ [" + ip + "] data : " + sample.getLaboratoryId());
				}
			}
		}

		// #. 병원용 검체들중 실험단계에 있는 검체중 approvedOutputCmplDate 값이 없는 경우의 검체 목록 추가
		Specification<Sample> hospitalWhere = Specification
				.where(SampleSpecification.approvedProductNotLike(params))
				.and(SampleSpecification.isHospital(true))
				.and(SampleSpecification.approvedOutputCmplDateIsNull())
				.and(SampleSpecification.statusIn(
					Arrays.asList(StatusCode.S200_EXP_READY, StatusCode.S210_EXP_STEP1, StatusCode.S220_EXP_STEP2, StatusCode.S230_EXP_STEP3)));

		List<Sample> hospitalSamples = sampleRepository.findAll(hospitalWhere);

		// #. 추가 조건으로 병원용 검체 별도 조회
		if (hospitalSamples.size() > 0) {
			for (Sample sample : hospitalSamples) {
				Set<String> productTypes = Sets.newHashSet();
				sample.getBundle().getProduct().stream().forEach(p -> {
					productTypes.add(p.getType());
				});
				
				if (productTypes.contains(productType)) {
					Map<String, Object> data = Maps.newHashMap();

					Map<String, Object> items = sample.getItems();
					data.put("barcode", sample.getLaboratoryId());
					data.put("name", items.get("h_name"));
					data.put("tel", items.get("h_tel"));
					data.put("address", items.get("h_address"));

					// #. 출생연도 birthyear 값이 있는 경우는 api 호출시 birthday에 '01-01'을 고정으로 붙여서 보냄
					if (items.get("birthyear") != null && items.get("birthyear").toString().length() > 0) {
						String birthyear = (String)items.get("birthyear");
						birthyear = birthyear.trim() + "-01-01";

						data.put("birthday", birthyear);
					}

					// #. 설문지정보 값을 추가
					data.put("docinfos", sample.getDocInfos());
					data.putAll(items);

					data.put("experimentid", sample.getLaboratoryId());
					data.put("collecteddate", (sample.getCollectedDate() != null ? sample.getCollectedDate().format(formatter) : ""));
					data.put("receiveddate", (sample.getReceivedDate() != null ? sample.getReceivedDate().format(formatter) : ""));
					data.put("sampletype", sample.getSampleType());
					data.put("p_name", sample.getBundle().getName());

					datas.add(data);
						
					logger.info("☆☆☆☆☆☆☆☆☆ [" + ip + "] hospital data : " + sample.getLaboratoryId());
				}
			}
		}

		
		rtn.put("result", "success");
		rtn.put("datas", datas);
		
		return rtn;
	}

	@Transactional
	@CacheEvict(value = "apiCache", allEntries = true)
	public Map<String, Object> updateStatus(Map<String, String> params, String ip) {
		logger.info("☆☆☆☆☆☆☆ getResultsForRest ☆☆☆ Cache Evict ☆☆☆ IN interface : " + params.toString());
		Map<String, Object> rtn = Maps.newHashMap();
		// #. productType 추가
		String laboratoryId = params.get("experimentid");
		String productType = params.get("productType");
		String productTypeData = "_" + productType + "_";
		boolean isTest = BooleanUtils.toBoolean(params.get("isTest"));
		
		Specification<Sample> where = Specification
				.where(SampleSpecification.laboratoryIdEqual(laboratoryId))
				.and(SampleSpecification.productNotLike(params))
				.and(SampleSpecification.statusIn(Arrays.asList(StatusCode.S700_OUTPUT_WAIT, StatusCode.S800_RE_OUTPUT_WAIT)));

		List<Sample> samples = sampleRepository.findAll(where);
		LocalDateTime now = LocalDateTime.now();

		if (samples.size() == 1) {
			Sample sample = samples.get(0);
			logger.info("☆☆☆☆ laboratory Id : " + sample.getLaboratoryId());
			logger.info("☆☆☆☆ productType : " + productType);
			Set<String> productTypes = Sets.newHashSet();
			sample.getBundle().getProduct().stream().forEach(p -> {
				productTypes.add(p.getType());
			});

			if (productTypes.contains(productType)) {
				// #. result status update
				if (!isTest) {
					logger.info("☆☆☆☆ isTest : " + isTest);
					
					String outputProductTypes = sample.getOutputProductTypes();
					if (outputProductTypes == null) outputProductTypes = "";
					if (!outputProductTypes.contains(productTypeData)) {
						outputProductTypes += productTypeData;
						outputProductTypes.replace("__", "_");
						sample.setOutputProductTypes(outputProductTypes);
					}

					boolean outputAllProduct = true;
					for (Product p : sample.getBundle().getProduct()) {
						if (!outputProductTypes.contains(p.getType())) {
							outputAllProduct = false;
							break;
						}
					}
					// #. 현재 productType과 interface된 productType값이 동일한 경우 상태 및 일자 처리
					if (outputAllProduct) {
						if (StatusCode.S700_OUTPUT_WAIT.equals(sample.getStatusCode())) {
							sample.setOutputCmplDate(now);
							sample.setStatusCode(StatusCode.S710_OUTPUT_CMPL);
							sample.setOutputProductTypes("");
							sample.setModifiedDate(now);
						} else if (StatusCode.S800_RE_OUTPUT_WAIT.equals(sample.getStatusCode())) {
							sample.setReOutputCmplDate(now);
							sample.setStatusCode(StatusCode.S810_RE_OUTPUT_CMPL);
							sample.setOutputProductTypes("");
							sample.setModifiedDate(now);
						}
					}
					sampleRepository.save(sample);
					logger.info("☆☆☆☆  [" + ip + "]sample[" + samples.get(0).getLaboratoryId() + "] update complete.");
				}
			}
		} else if (samples.size() > 1) {
			logger.info("☆☆☆☆  sample[" + samples.get(0).getLaboratoryId() + "] count error : size = [" + samples.size() + "]");
		}

		Specification<Sample> hospitalWhere = Specification
				.where(SampleSpecification.laboratoryIdEqual(laboratoryId))
				.and(SampleSpecification.approvedProductNotLike(params))
				.and(SampleSpecification.isHospital(true))
				.and(SampleSpecification.approvedOutputCmplDateIsNull())
				.and(SampleSpecification.statusIn(
					Arrays.asList(StatusCode.S200_EXP_READY, StatusCode.S210_EXP_STEP1, StatusCode.S220_EXP_STEP2, StatusCode.S230_EXP_STEP3)));

		List<Sample> hospitalSamples = sampleRepository.findAll(hospitalWhere);

		// #. 추가 검색된 병원용 검체 별도 수정
		if (hospitalSamples.size() == 1) {
			Sample sample = hospitalSamples.get(0);
			logger.info("☆☆☆☆ hospital laboratory Id : " + sample.getLaboratoryId());
			logger.info("☆☆☆☆ hospital productType : " + productType);
			Set<String> productTypes = Sets.newHashSet();
			sample.getBundle().getProduct().stream().forEach(p -> {
				productTypes.add(p.getType());
			});

			
			if (productTypes.contains(productType)) {
				// #. result status update
				if (!isTest) {
					logger.info("☆☆☆☆ hospital isTest : " + isTest);
					
					String outputProductTypes = sample.getApprovedOutputProductTypes();
					if (outputProductTypes == null) outputProductTypes = "";
					if (!outputProductTypes.contains(productTypeData)) {
						outputProductTypes += productTypeData;
						outputProductTypes.replace("__", "_");
						sample.setApprovedOutputProductTypes(outputProductTypes);
					}

					boolean outputAllProduct = true;
					for (Product p : sample.getBundle().getProduct()) {
						if (!outputProductTypes.contains(p.getType())) {
							outputAllProduct = false;
							break;
						}
					}
					// #. 현재 productType과 interface된 productType값이 동일한 경우 상태 및 일자 처리
					if (outputAllProduct) {
						sample.setApprovedOutputCmplDate(now);
						sample.setApprovedOutputProductTypes("");
					}
					sampleRepository.save(sample);
					logger.info("☆☆☆☆  hospital [" + ip + "]sample[" + hospitalSamples.get(0).getLaboratoryId() + "] update complete.");
				}
			}
		} else if (samples.size() > 1) {
			logger.info("☆☆☆☆  hospital sample[" + hospitalSamples.get(0).getLaboratoryId() + "] count error : size = [" + hospitalSamples.size() + "]");
		}

		rtn.put("result", "success");
		
		return rtn;
	}

	public Map<String, Object> getResultByLaboratoryForRest(Map<String, String> params, String ip) {
		logger.info("☆☆☆☆☆☆☆ getResultByLaboratoryForRest ☆☆☆☆☆ IN interface : " + params.toString());
		Map<String, Object> rtn = Maps.newHashMap();
		// #. 실험실번호
		String laboratoryId = params.get("experimentid");
		
		Specification<Sample> where = Specification
				.where(SampleSpecification.statusCodeGt(710))
				.and(SampleSpecification.laboratoryIdEqual(laboratoryId))
				.and(SampleSpecification.isLastVersionTrue());

		List<Sample> samples = sampleRepository.findAll(where);

		Map<String, Object> data = Maps.newHashMap();
		if (samples.size() > 0) {
			Sample sample = samples.get(0);
			Map<String, Object> items = sample.getItems();

			// #. 출생연도 birthyear 값이 있는 경우는 api 호출시 birthday에 '01-01'을 고정으로 붙여서 보냄
			if (items.get("birthyear") != null && items.get("birthyear").toString().length() > 0) {
				String birthyear = (String)items.get("birthyear");
				birthyear += "-01-01";

				data.put("birthday", birthyear);
			}

			data.putAll(items);
			data.put("experimentid", sample.getLaboratoryId());
			data.put("genedata", sample.getData());
			data.put("fileFullPath", sample.getFilePath() + "/" + sample.getFileName());
			data.put("chipType", sample.getChipTypeCode());
				
			logger.info("☆☆☆☆☆☆☆☆☆ [" + ip + "]data : " + sample.getLaboratoryId());
		}

		rtn.put("result", "success");
		rtn.put("data", data);
		
		return rtn;
	}

	public Map<String, Object> getResultByParamsForRest(Map<String, String> params, String ip) {
		logger.info("☆☆☆☆☆☆☆ getResultByParamsForRest ☆☆☆☆☆ IN interface : " + params.toString());
		Map<String, Object> rtn = Maps.newHashMap();

		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);
		int markerCount = NumberUtils.toInt(params.get("markerCount") + "", 0);
		
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount);
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusCodeGt(700))
					.and(SampleSpecification.orderBy(params));

		logger.info("☆☆☆ markerCount=[" + markerCount + "]");
		if (markerCount > 0) {
			where = where.and(SampleSpecification.markerCountGt(markerCount));
		}
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		logger.info("★★★ list.size()=" + list.size());

		rtn.put("result", "success");
		rtn.put("list", list);
		rtn.put("pageNumber", pageNumber);
		rtn.put("totalCount", total);
		
		return rtn;
	}

	public Map<String, Object> getResultByLaboratoryIdsForRest(Map<String, Object> params, String ip) {
		logger.info("☆☆☆☆☆☆☆ getResultByLaboratoryIdsForRest ☆☆☆☆☆ IN interface : " + params.toString());
		Map<String, Object> rtn = Maps.newHashMap();

		List<String> laboratoryIds = (List<String>)params.get("laboratoryIds");
		logger.info("★★★ params=" + params.toString());
		Specification<Sample> where = Specification
					.where(SampleSpecification.laboratoryIdIn(laboratoryIds));
					
		
		List<Sample> list = sampleRepository.findAll(where);
		
		logger.info("★★★ list.size()=" + list.size());

		rtn.put("result", "success");
		rtn.put("list", list);
		
		return rtn;
	}

	public Map<String, Object> getHospitalInfos(String ip) {
		logger.info("☆☆☆☆☆☆☆ getHospitalInfos ☆☆☆☆☆");
		Map<String, Object> rtn = Maps.newHashMap();

		List<Agency> agencies = agencyRepository.findAll();
		List<Bundle> bundles = bundleRepository.findAll();
		
		rtn.put("result", "success");
		rtn.put("agencies", agencies);
		rtn.put("bundles", bundles);
		
		return rtn;
	}
}