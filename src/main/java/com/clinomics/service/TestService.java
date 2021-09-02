package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.clinomics.entity.lims.Agency;
import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Product;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.SampleItem;
import com.clinomics.entity.lims.SampleTest;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.AgencyRepository;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleTestRepository;
import com.clinomics.specification.lims.SampleTestSpecification;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TestService {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	SampleTestRepository sampleTestRepository;
	
	@Autowired
	BundleRepository bundleRepository;
	
	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	AgencyRepository agencyRepository;
	
	@Autowired
	VariousFieldsService variousDayService;

	@Autowired
	DataTableService dataTableService;
	
	@Autowired
	ProductRepository productRepository;

	@Autowired
	RoleService roleService;
	
	
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
	@CacheEvict(value = "hospitalCache", allEntries = true)
	public Map<String, String> save(Map<String, String> inputItems, boolean history) {
		Map<String, Object> items = Maps.newHashMap();
		items.putAll(inputItems);
		Map<String, String> rtn = Maps.newHashMap();
		
		String id = items.getOrDefault("id", "0") + "";
		
		SampleTest sampleTest = searchExistsSampleTest(NumberUtils.toInt(id));
		
		boolean existsSampleTest = sampleTest.getId() > 0;

		// #. 바코드 중복체크
		String barcode = StringUtils.stripToEmpty((String)items.get("barcode"));
		if (barcode.length() > 0) {
			long count = this.searchExistsBarcode(barcode);
			// #. 수정시 본인에 바코드가 수정되야하면 중복체크
			if (existsSampleTest) {
				String beforeBarcode = StringUtils.stripToEmpty((String)sampleTest.getItems().get("barcode"));
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
		if (existsSampleTest) {
			if (!sampleTestRepository.existsById(NumberUtils.toInt(id)) && history) {
				//savesampleTestHistory(sampleTest);
			}
			bundle = sampleTest.getBundle();
			
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
			sampleTest.setCreatedDate(now);
			sampleTest.setModifiedDate(now);
			sampleTest.setCreatedMember(member);
			sampleTest.setStatusCode(StatusCode.S700_OUTPUT_WAIT);

			if (barcode.length() > 0) {
				String barcodeLetter = barcode.replaceAll("^([a-zA-Z]+)\\-([0-9]+)$", "$1");
				String barcodeNumber = barcode.replaceAll("^([a-zA-Z]+)\\-([0-9]+)$", "$2");
				sampleTest.setBarcodeLetter(barcodeLetter);
				sampleTest.setBarcodeNumber(barcodeNumber);
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
				
				sampleTest.setAgency(ag);
			}
		}
		
		items.remove("memberId");
		items.remove("bundleId");
		sampleTest.setBundle(bundle);

		variousDayService.setFieldsTest(existsSampleTest, sampleTest, items);

		items.remove("id");
		
		Map<String, Object> newItems = Maps.newHashMap();
		newItems.putAll(items);
		sampleTest.setItems(newItems);
		
		sampleTestRepository.save(sampleTest);
		
		rtn.put("result", ResultCode.SUCCESS.get());
		rtn.put("message", ResultCode.SUCCESS.getMsg());
		return rtn;
	}
	
	public long searchExistsBarcode(String barcode) {
		
		Specification<SampleTest> where = Specification.where(SampleTestSpecification.barcodeEqual(barcode));
		long count = sampleTestRepository.count(where);
		
		return count;
	}
	
	public SampleTest searchExistsSampleTest(int id) {
		
		Optional<SampleTest> oSampleTest = sampleTestRepository.findById(id);
		
		SampleTest news = new SampleTest();
		LocalDateTime now = LocalDateTime.now();
		news.setCreatedDate(now);
		SampleTest sampleTest = oSampleTest.orElse(news);
		sampleTest.setModifiedDate(now);

		return sampleTest;
	}

	public Map<String, Object> findByModifiedDate(Map<String, String> params, int statusCodeNumber) {
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
		
		Specification<SampleTest> where = Specification
					.where(SampleTestSpecification.betweenModifiedDate(params))
					.and(SampleTestSpecification.bundleId(params))
					.and(SampleTestSpecification.hNameIn(params))
					.and(SampleTestSpecification.keywordLike(params))
					.and(SampleTestSpecification.bundleIsActive())
					.and(SampleTestSpecification.statusCodeGt(statusCodeNumber))
					.and(SampleTestSpecification.orderBy(params));
					
		
		total = sampleTestRepository.count(where);
		Page<SampleTest> page = sampleTestRepository.findAll(where, pageable);
		
		List<SampleTest> list = page.getContent();
		List<Map<String, Object>> header = this.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, Object> findByModifiedDate(Map<String, String> params, String statusCode) {
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
		
		Specification<SampleTest> where = Specification
					.where(SampleTestSpecification.betweenModifiedDate(params))
					.and(SampleTestSpecification.bundleId(params))
					.and(SampleTestSpecification.hNameIn(params))
					.and(SampleTestSpecification.keywordLike(params))
					.and(SampleTestSpecification.bundleIsActive())
					.and(SampleTestSpecification.statusEqual(StatusCode.valueOf(statusCode)))
					.and(SampleTestSpecification.orderBy(params));
					
		
		total = sampleTestRepository.count(where);
		Page<SampleTest> page = sampleTestRepository.findAll(where, pageable);
		
		List<SampleTest> list = page.getContent();
		List<Map<String, Object>> header = this.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, Object> findSampleItemBySample(String id) {
		Optional<SampleTest> oSampleTest = sampleTestRepository.findById(NumberUtils.toInt(id));
		SampleTest sampleTest = oSampleTest.orElse(new SampleTest());
		
		Optional<Bundle> oBundle = Optional.of(sampleTest.getBundle());
		Bundle bundle = oBundle.orElse(new Bundle());
		Set<SampleItem> sampleTestItems = new HashSet<SampleItem>();
		bundle.getProduct().stream().forEach(p -> {
			
			Optional<Product> oProduct = productRepository.findById(p.getId());
			Product product = oProduct.orElse(new Product());
			
			sampleTestItems.addAll(product.getSampleItem());
			
		});

		boolean personalView = roleService.checkPersonalView();
		
		List<SampleItem> sortedSampleTestItems = sampleTestItems.stream()
				.filter(fs -> {
					if (fs.isVisible()) {
						return personalView;
					} else {
						return true;
					}
				})
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());
		
		Map<String, Object> rtn = Maps.newHashMap();
		rtn.put("sampleTestItem", sortedSampleTestItems);
		rtn.put("sampleTest", sampleTest);
		
		return rtn;
	}

	public Map<String, Object> findSampleDataBySampleId(String id) {
		Optional<SampleTest> oSample = sampleTestRepository.findById(NumberUtils.toInt(id));
		SampleTest sampleTest = oSample.orElse(new SampleTest());
		
		Map<String, Object> resultData = sampleTest.getData();

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
		rtn.put("sampleTest", sampleTest);
		rtn.put("datas", datas);
		
		return rtn;
	}

	@Transactional
	@CacheEvict(value = "apiCache", allEntries = true)
	public Map<String, String> outputApprove(List<Integer> ids, String memberId) {
		logger.info("☆☆☆☆☆☆☆ outputApprove ☆☆☆ Cache Evict ☆☆☆");
		Map<String, String> rtn = Maps.newHashMap();
		
		List<SampleTest> sampleTests = sampleTestRepository.findByIdInAndStatusCodeIn(ids, Arrays.asList(new StatusCode[] { StatusCode.S600_JDGM_APPROVE, StatusCode.S710_OUTPUT_CMPL, StatusCode.S810_RE_OUTPUT_CMPL }));
		
		// sample.set
		Optional<Member> oMember = memberRepository.findById(memberId);
		Member member = oMember.get();
		LocalDateTime now = LocalDateTime.now();

		rtn.put("result", ResultCode.SUCCESS_APPROVED.get());
		rtn.put("message", ResultCode.SUCCESS_APPROVED.getMsg());
			
		sampleTests.stream().forEach(s -> {
			StatusCode sc = s.getStatusCode();
			if (sc.equals(StatusCode.S600_JDGM_APPROVE)) {
				// #. 발행
				s.setOutputWaitDate(now);
				s.setOutputWaitMember(member);
				s.setModifiedDate(now);
				s.setStatusCode(StatusCode.S700_OUTPUT_WAIT);
			} else if (sc.equals(StatusCode.S710_OUTPUT_CMPL) || sc.equals(StatusCode.S810_RE_OUTPUT_CMPL)) {
				// #. 재발행
				s.setReOutputWaitDate(now);
				s.setReOutputWaitMember(member);
				s.setModifiedDate(now);
				
				s.setStatusCode(StatusCode.S800_RE_OUTPUT_WAIT);
			}
		});

		sampleTestRepository.saveAll(sampleTests);

		
		return rtn;
	}

	// ############################## private ##############################
	private List<Map<String, Object>> filterItemsAndOrdering(List<SampleTest> list) {
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		List<Map<String, Object>> header = new ArrayList<>();
		list.stream().forEach(s -> {

			s.getBundle().getProduct().stream().forEach(p -> {
				sampleItems.addAll(p.getSampleItem());
			});
		});

		boolean personalView = roleService.checkPersonalView();

		List<SampleItem> filteredSampleItems = sampleItems.stream().filter(fs -> {
			if (!fs.isActive()) {
				return false;
			} else if (fs.isVisible()) {
				return personalView;
			} else {
				return true;
			}
		}).sorted(Comparator.comparing(SampleItem::getOrd)).collect(Collectors.toList());

		filteredSampleItems.forEach(f -> {
			Map<String, Object> tt = Maps.newLinkedHashMap();
			tt.put("title", f.getName());
			tt.put("data", "items." + f.getNameCode());
			if ("date".equals(f.getType())) {
				tt.put("type", "date");
			}
			header.add(tt);
		});

		return header;
	}
}