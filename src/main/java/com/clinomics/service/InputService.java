package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Agency;
import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Document;
import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleHistory;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.AgencyRepository;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.DocumentRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleHistoryRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.DocumentSpecification;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.CustomIndexPublisher;
import com.google.common.collect.Maps;

@Service
public class InputService {

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	SampleHistoryRepository sampleHistoryRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	AgencyRepository agencyRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	DocumentRepository documentRepository;
	
	@Autowired
	DataTableService dataTableService;

	@Autowired
	VariousFieldsService variousDayService;
	
	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	CustomIndexPublisher customIndexPublisher;
	
	public Map<String, Object> findAll() {
		int draw = 1;
		long total = sampleRepository.count();
		List<Sample> list = sampleRepository.findAll();
		
		return dataTableService.getDataTableMap(draw, draw, total, total, list);
	}
	
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
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.hNameIn(params))
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
	@CacheEvict(value = "hospitalCache", allEntries = true)
	public Map<String, String> save(Map<String, String> inputItems, boolean history) {
		Map<String, Object> items = Maps.newHashMap();
		items.putAll(inputItems);
		Map<String, String> rtn = Maps.newHashMap();
		
		String id = items.getOrDefault("id", "0") + "";
		
		Sample sample = searchExistsSample(NumberUtils.toInt(id));
		
		boolean existsSample = sample.getId() > 0;

		// #. 바코드 중복체크
		String barcode = StringUtils.stripToEmpty((String)items.get("barcode"));
		if (barcode.length() > 0) {
			long count = this.searchExistsBarcode(barcode);
			// #. 수정시 본인에 바코드가 수정되야하면 중복체크
			if (existsSample) {
				String beforeBarcode = StringUtils.stripToEmpty((String)sample.getItems().get("barcode"));
				if (!beforeBarcode.equals(barcode)) {
					if (count > 0) {
						rtn.put("result", ResultCode.FAIL_DUPL_VALUE.get());
						rtn.put("message", ResultCode.FAIL_DUPL_VALUE.getMsg() + "[" + barcode + "]");
						return rtn;
					}
				}
			} else {
				// #. 등록시 바코드 중복 체크
				if (count > 0) {
					rtn.put("result", ResultCode.FAIL_DUPL_VALUE.get());
					rtn.put("message", ResultCode.FAIL_DUPL_VALUE.getMsg() + "[" + barcode + "]");
					return rtn;
				}
			}
		}
		
		LocalDateTime now = LocalDateTime.now();
		Bundle bundle;
		if (existsSample) {
			if (!sampleRepository.existsById(NumberUtils.toInt(id)) && history) {
				saveSampleHistory(sample);
			}
			bundle = sample.getBundle();
			
		} else {
			if (!items.containsKey("bundleId")) {
				rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get());
				return rtn;
			}
			int bundleId = NumberUtils.toInt(items.get("bundleId") + "");
			Optional<Bundle> oBundle = bundleRepository.findById(bundleId);
			bundle = oBundle.orElseThrow(NullPointerException::new);
			
			Optional<Member> oMember = memberRepository.findById(items.getOrDefault("memberId", "") + "");
			Member member = oMember.orElseThrow(NullPointerException::new);
			sample.setCreatedDate(now);
			sample.setModifiedDate(now);
			sample.setCreatedMember(member);
			sample.setStatusCode(StatusCode.S000_INPUT_REG);

			if (barcode.length() > 0) {
				String barcodeLetter = barcode.replaceAll("^([a-zA-Z]+)\\-([0-9]+)$", "$1");
				String barcodeNumber = barcode.replaceAll("^([a-zA-Z]+)\\-([0-9]+)$", "$2");
				sample.setBarcodeLetter(barcodeLetter);
				sample.setBarcodeNumber(barcodeNumber);
			}
			if (items.containsKey("h_name")) {
				String hName = items.get("h_name") + "";
				
				Agency ag = agencyRepository.findByName(hName).orElseGet(() -> {
					Agency newag = new Agency();
					newag.setName(hName);
					agencyRepository.save(newag);
					agencyRepository.flush();
					return newag;
				});
				
				sample.setAgency(ag);
			}
		}
		
		items.remove("memberId");
		items.remove("bundleId");
		sample.setBundle(bundle);

		variousDayService.setFields(existsSample, sample, items);

		items.remove("id");
		
		Map<String, Object> newItems = Maps.newHashMap();
		newItems.putAll(items);
		sample.setItems(newItems);
		
		sampleRepository.save(sample);
		
		rtn.put("result", ResultCode.SUCCESS.get());
		rtn.put("message", ResultCode.SUCCESS.getMsg());
		return rtn;
	}
	
	public Map<String, String> saveFromList(List<Map<String, String>> list, String memberId) {
		Map<String, String> rtn = Maps.newHashMap();
		for (Map<String, String> l : list) {
			l.put("memberId", memberId);
			Map<String, String> tmp = this.save(l, false);
			if (!ResultCode.SUCCESS.get().equals(tmp.getOrDefault("result", "AA"))) {
				return tmp;
			}
			rtn.putAll(tmp);
		}
		return rtn;
	}

	@Transactional
	public Map<String, String> delete(List<Integer> ids) {
		Map<String, String> rtn = Maps.newHashMap();
		List<Sample> samples = sampleRepository.findByIdInAndStatusCodeIn(ids
			, Arrays.asList(new StatusCode[] { StatusCode.S020_INPUT_RCV, StatusCode.S000_INPUT_REG })
		);
		
		rtn.put("result", ResultCode.SUCCESS_DELETE.get());
		rtn.put("message", ResultCode.SUCCESS_DELETE.getMsg());

		
		sampleRepository.deleteAll(samples);
		
		return rtn;
	}

	@Transactional
	public Map<String, String> approve(List<Integer> ids, String memberId) {
		Map<String, String> rtn = Maps.newHashMap();
		
		// sample.set
		Optional<Member> oMember = memberRepository.findById(memberId);
		Member member = oMember.get();
		LocalDateTime now = LocalDateTime.now();
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		List<StatusCode> status = new ArrayList<>();
		status.add(StatusCode.S020_INPUT_RCV);
		if (roles.contains(RoleCode.ROLE_INPUT_20.toString())) {
			status.add(StatusCode.S000_INPUT_REG);
		} 
		List<Sample> samples = sampleRepository.findByIdInAndStatusCodeIn(ids, status);

		rtn.put("result", ResultCode.SUCCESS_APPROVED.get());
		rtn.put("message", ResultCode.SUCCESS_APPROVED.getMsg());

		if (roles.contains(RoleCode.ROLE_EXP_80.toString())) {
			
			samples.stream().forEach(s -> {
				s.setInputDrctApproveDate(now);
				s.setInputDrctMember(member);
				s.setModifiedDate(now);
				if (s.getInputApproveDate() != null && s.getInputMngApproveDate() != null && s.getInputDrctApproveDate() != null) {
					s.setStatusCode(StatusCode.S200_EXP_READY);
				}
			});

		} else if (roles.contains(RoleCode.ROLE_INPUT_40.toString())) {
			
			samples.stream().forEach(s -> {
				s.setInputMngApproveDate(now);
				s.setInputMngApproveMember(member);
				s.setModifiedDate(now);
				if (s.getInputApproveDate() != null && s.getInputMngApproveDate() != null && s.getInputDrctApproveDate() != null) {
					s.setStatusCode(StatusCode.S200_EXP_READY);
				}
			});

		} else if (roles.contains(RoleCode.ROLE_INPUT_20.toString())) {
			
			samples.stream().forEach(s -> {
				s.setInputApproveDate(now);
				s.setInputApproveMember(member);
				s.setModifiedDate(now);
				s.setStatusCode(StatusCode.S020_INPUT_RCV);
			});
		} else {
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}
		sampleRepository.saveAll(samples);
		
		return rtn;
	}

	public Map<String, Object> findDocment(Map<String, String> params) {
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
		
		Specification<Document> where = Specification
					.where(DocumentSpecification.betweenDate(params))
					.and(DocumentSpecification.bundleId(params))
					.and(DocumentSpecification.hNameIn(params))
					.and(DocumentSpecification.keywordLike(params))
					.and(DocumentSpecification.bundleIsActive())
					.and(DocumentSpecification.orderBy(params));
					
		
		total = documentRepository.count(where);
		Page<Document> page = documentRepository.findAll(where, pageable);
		
		List<Document> list = page.getContent();
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
	}

	public Map<String, Object> findDocumentById(String id) {
		Optional<Document> oDocument= documentRepository.findById(NumberUtils.toInt(id));
		Document document = oDocument.orElse(new Document());
		
		Map<String, Object> rtn = Maps.newHashMap();
		rtn.put("document", document);
		
		return rtn;
	}

	@Transactional
	public Map<String, String> saveDocument(Map<String, String> documentMap) {
		Map<String, String> rtn = Maps.newHashMap();

		int id = NumberUtils.toInt(documentMap.get("id"));
		String agencyName = documentMap.get("agencyName");
		int bundleId = NumberUtils.toInt(documentMap.get("bundleId"));

		Optional<Agency> oAgency = agencyRepository.findByName(agencyName);
		if (!oAgency.isPresent()) {
			rtn.put("result", ResultCode.FAIL_UNKNOWN.get());
			rtn.put("message", ResultCode.FAIL_UNKNOWN.getMsg());
			return rtn;
		}

		Optional<Bundle> oBundle = bundleRepository.findById(bundleId);
		if (!oBundle.isPresent()) {
			rtn.put("result", ResultCode.FAIL_UNKNOWN.get());
			rtn.put("message", ResultCode.FAIL_UNKNOWN.getMsg());
			return rtn;
		}

		Optional<Document> oDocument = documentRepository.findById(id);
		Document doc = oDocument.orElse(new Document());
		
		doc.setDoctorName(documentMap.get("doctorName"));
		doc.setChartNumber(documentMap.get("chartNumber"));
		doc.setName(documentMap.get("name"));
		doc.setTel(documentMap.get("tel"));
		doc.setAddress(documentMap.get("address"));
		doc.setSex(documentMap.get("sex"));
		doc.setBirthday(documentMap.get("birthday"));
		doc.setHeight(documentMap.get("height"));
		doc.setWeight(documentMap.get("weight"));
		doc.setSmoking(documentMap.get("smoking"));
		doc.setAlcohol(documentMap.get("alcohol"));
		doc.setMeat(documentMap.get("meat"));
		doc.setInstant(documentMap.get("instant"));
		doc.setFried(documentMap.get("fried"));
		doc.setSalt(documentMap.get("salt"));
		doc.setExercise(documentMap.get("exercise"));
		doc.setDepression(documentMap.get("depression"));
		doc.setStress(documentMap.get("stress"));

		doc.setAgency(oAgency.get());
		doc.setBundle(oBundle.get());

		documentRepository.save(doc);

		rtn.put("result", ResultCode.SUCCESS.get());
		rtn.put("message", ResultCode.SUCCESS.getMsg());
		return rtn;
	}

	@Transactional
	public Map<String, Object> saveDocumentForRest(Map<String, Object> documentMap) {
		Map<String, Object> rtn = Maps.newHashMap();

		int agencyId = NumberUtils.toInt((String)documentMap.get("agencyId"));
		int bundleId = NumberUtils.toInt((String)documentMap.get("bundleId"));

		System.out.println("★★★ documentMap=" + documentMap.toString());

		Optional<Agency> oAgency = agencyRepository.findById(agencyId);
		if (!oAgency.isPresent()) {
			rtn.put("result", "fail");
			rtn.put("message", "병원 코드가 정확하지 않습니다.");
			return rtn;
		}

		Optional<Bundle> oBundle = bundleRepository.findById(bundleId);
		if (!oBundle.isPresent()) {
			rtn.put("result", "fail");
			rtn.put("message", "상품 코드가 정확하지 않습니다.");
			return rtn;
		}

		Map<String, String> map = Maps.newHashMap();
		map.put("doctorName", (String)documentMap.get("doctorName"));
		map.put("chartNumber", (String)documentMap.get("chartNumber"));
		map.put("name", (String)documentMap.get("name"));
		map.put("tel", (String)documentMap.get("tel"));
		map.put("address", (String)documentMap.get("address"));
		map.put("sex", (String)documentMap.get("sex"));
		map.put("birthday", (String)documentMap.get("birthday"));
		map.put("height", (String)documentMap.get("height"));
		map.put("weight", (String)documentMap.get("weight"));
		map.put("smoking", (String)documentMap.get("smoking"));
		map.put("alcohol", (String)documentMap.get("alcohol"));
		map.put("meat", (String)documentMap.get("meat"));
		map.put("instant", (String)documentMap.get("instant"));
		map.put("fried", (String)documentMap.get("fried"));
		map.put("salt", (String)documentMap.get("salt"));
		map.put("exercise", (String)documentMap.get("exercise"));
		map.put("depression", (String)documentMap.get("depression"));
		map.put("stress", (String)documentMap.get("stress"));
		map.put("agencyName", oAgency.get().getName());
		map.put("bundleId", Integer.toString(oBundle.get().getId()));

		Map<String, String> result = this.saveDocument(map);

		if (result.get("result").equals(ResultCode.SUCCESS.get())) {
			rtn.put("result", "success");
			rtn.put("message", "");
		} else {
			rtn.put("result", result.get("result"));
			rtn.put("message", result.get("message"));
		}

		return rtn;
	}

	@Transactional
	public Map<String, String> deleteDocument(List<Integer> ids) {
		Map<String, String> rtn = Maps.newHashMap();
		List<Document> documents = documentRepository.findByIdIn(ids);
		
		rtn.put("result", ResultCode.SUCCESS_DELETE.get());
		rtn.put("message", ResultCode.SUCCESS_DELETE.getMsg());

		
		documentRepository.deleteAll(documents);
		
		return rtn;
	}

	@Transactional
	public Map<String, String> approveDocument(List<Integer> ids, String memberId) {
		Map<String, String> rtn = Maps.newHashMap();
		
		// sample.set
		Optional<Member> oMember = memberRepository.findById(memberId);
		Member member = oMember.get();
		LocalDateTime now = LocalDateTime.now();

		// #. TODO sample 로 등록
		
		return rtn;
	}

	public Sample searchExistsSample(int id) {
		
		Optional<Sample> oSample = sampleRepository.findById(id);
		
		Sample news = new Sample();
		LocalDateTime now = LocalDateTime.now();
		news.setCreatedDate(now);
		Sample sample = oSample.orElse(news);
		sample.setModifiedDate(now);

		return sample;
	}

	public long searchExistsBarcode(String barcode) {
		
		Specification<Sample> where = Specification.where(SampleSpecification.barcodeEqual(barcode));
		long count = sampleRepository.count(where);
		
		return count;
	}
	
	private void saveSampleHistory(Sample smpl) {
		SampleHistory sh = new SampleHistory();
		sh.setSample(smpl);
		sh.setItems(smpl.getItems());
		sh.setMember(smpl.getCreatedMember());
		
		sampleHistoryRepository.save(sh);
	}
}
