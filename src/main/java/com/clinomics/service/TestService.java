package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Agency;
import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.SampleTest;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.AgencyRepository;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleTestRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.google.common.collect.Maps;

@Service
public class TestService {
	
	@Autowired
	SampleTestRepository sampleTestRepository;
	
//	@Autowired
//	SampleTestHistoryRepository sampleTestHistoryRepository;

	@Autowired
	BundleRepository bundleRepository;
	
	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	AgencyRepository agencyRepository;
	
	@Autowired
	VariousFieldsService variousDayService;
	
	
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
	
//	private void savesampleTestHistory(SampleTest smpl) {
//		sampleTestHistory sh = new sampleTestHistory();
//		sh.setsampleTest(smpl);
//		sh.setItems(smpl.getItems());
//		sh.setMember(smpl.getCreatedMember());
//		
//		sampleTestHistoryRepository.save(sh);
//	}
	
	public long searchExistsBarcode(String barcode) {
		
		Specification<SampleTest> where = Specification.where(SampleSpecification.testBarcodeEqual(barcode));
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

}
