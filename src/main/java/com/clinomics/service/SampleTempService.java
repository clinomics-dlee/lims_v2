package com.clinomics.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleTemp;
import com.clinomics.enums.ResultCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.repository.lims.SampleTempRepository;
import com.clinomics.util.CustomIndexPublisher;
import com.google.common.collect.Maps;

@Service
public class SampleTempService {
	
	@Autowired
	SampleRepository sampleRepository;
	
	@Autowired
	SampleTempRepository sampleTempRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CustomIndexPublisher customIndexPublisher;
	
	@Autowired
	DataTableService dataTableService;
	
	public Map<String, Object> findAll(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		// #. count 조회
		long total = sampleTempRepository.count();
		long filtered = total;

		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}

		Page<SampleTemp> list = sampleTempRepository.findAll(pageable);

		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list.getContent());
	}
	
	public Map<String, Object> importExcelToSample() {
		List<SampleTemp> st = sampleTempRepository.findAll();
		
		Map<String, Object> rtn = Maps.newHashMap();
		
		if (st.size() < 1) {
			rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get());
			return rtn;
		}
		
		Optional<Bundle> obundle = bundleRepository.findById(st.get(0).getBundleId());
		Bundle bundle = obundle.orElse(new Bundle());
		
		Optional<Member> om = memberRepository.findById(st.get(0).getMemberId());
		Member member = om.orElse(new Member());
		
		LocalDateTime ldt = LocalDateTime.now();
		List<Sample> newImports = new ArrayList<Sample>();
		for (SampleTemp t : st) {
			Sample sample = new Sample();
			sample.setBundle(bundle);
			sample.setCreatedMember(member);
			
			Map<String, Object> items = t.getItems();
			
			if (bundle.isAutoBarcode()) {
				String bar = customIndexPublisher.getNextBarcodeByBundle(bundle);
				if (!bar.isEmpty()) sample.setBarcode(bar);
			} else {
				sample.setBarcode(t.getItems().get("barcode") + "");
			}
			
			// #. TODO
			// if (bundle.isAutoSequence()) {
			// 	String seq = customIndexPublisher.getNextSequenceByBundle(bundle);
			// 	if (!seq.isEmpty()) sample.setId(seq);
			// } else {
			// 	sample.setId(t.getItems().get("laboratory") + "");
			// }
			items.remove("laboratory");
			items.remove("barcode");
			sample.setItems(items);
			sample.setCreatedDate(ldt);
			sample.setModifiedDate(ldt);
			
			newImports.add(sample);
			
		}
		
		sampleRepository.saveAll(newImports);
		
		sampleTempRepository.deleteAll();
		
		rtn.put("result", ResultCode.SUCCESS.get());
		
		return rtn;
	}
}
