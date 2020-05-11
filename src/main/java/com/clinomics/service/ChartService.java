package com.clinomics.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.ResultSpecification;
import com.clinomics.specification.lims.SampleSpecification;
import com.google.common.collect.Maps;

@Service
public class ChartService {

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	BundleRepository bundleRepository;

	@PersistenceContext
	private EntityManager entityManager;
	
	DateTimeFormatter yyyymmFormat = DateTimeFormatter.ofPattern("yyyy-MM");

	public Bundle selectOne(int id) {
		return bundleRepository.findById(id).orElse(new Bundle());
	}

	public List<Bundle> selectAll() {
		return bundleRepository.findAll();
	}

	public Map<String, Object> selectCountByMonthly(Map<String, String> params) {
		String paramStart = params.get("start");
		String paramEnd = params.get("end");
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime start = LocalDateTime.parse(paramStart + "-01 00:00:00", formatter);
		LocalDateTime end = LocalDateTime.parse(paramEnd + "-01 00:00:00", formatter);
		end = end.plusMonths(1).minusSeconds(1);
		
		List<Bundle> bundles = bundleRepository.findAll();
		
		List<Sample> sample = sampleRepository.findAll(getSampleWhere(params));
		List<Sample> complete = sampleRepository.findAll(getCompletedWhere(params));
		List<Sample> ifComplete = sampleRepository.findAll(getReportedWhere(params));
		
		List<Map<String, String>> mapSample = sample.stream()
			.map(s -> {
				Map<String, String> t = Maps.newHashMap();
				t.put("yyyymm", s.getCreatedDate().format(yyyymmFormat));
				t.put("name", s.getBundle().getName());
				return t;
			}).collect(Collectors.toList());
		
		List<Map<String, String>> mapSampleComplete = complete.stream()
			.map(s -> {
				Map<String, String> t = Maps.newHashMap();
				t.put("yyyymm", (s.getModifiedDate().format(yyyymmFormat)));
				t.put("name", s.getBundle().getName());
				return t;
			}).collect(Collectors.toList());
		
		List<Map<String, String>> mapResult = ifComplete.stream()
			.map(s -> {
				Map<String, String> t = Maps.newHashMap();
				t.put("yyyymm", (s.getModifiedDate().format(yyyymmFormat)));
				t.put("name", s.getBundle().getName());
				return t;
			}).collect(Collectors.toList());
		
		Map<String, Object> rtn1 = Maps.newHashMap();
		LocalDateTime loop = start;
		
		rtn1.put("labels", getChartLabels(end, loop));
		rtn1.put("datasets", getDatasetByBundle(bundles, end, loop, mapSample));
		
		Map<String, Object> rtn2 = Maps.newHashMap();
		loop = start;
		
		rtn2.put("labels", getChartLabels(end, loop));
		rtn2.put("datasets", getDatasetByBundle(bundles, end, loop, mapSampleComplete));
		
		Map<String, Object> rtn3 = Maps.newHashMap();
		loop = start;
		
		rtn3.put("labels", getChartLabels(end, loop));
		rtn3.put("datasets", getDatasetByBundle(bundles, end, loop, mapResult));
		
		Map<String, Object> rtn4 = Maps.newHashMap();
		loop = start;
		
		rtn4.put("labels", bundles.stream().map(b -> b.getName()).collect(Collectors.toList()));
		rtn4.put("datasets", getPieDatasetByBundle(bundles, end, mapSample));
			
		
		Map<String, Object> rtn = Maps.newHashMap();
		rtn.put("chart1", rtn1);
		rtn.put("chart2", rtn2);
		rtn.put("chart3", rtn3);
		rtn.put("chart4", rtn4);
		
		return rtn;
	}
	
	private List<Map<String, Object>> getDatasetByBundle(List<Bundle> bundles, LocalDateTime end, LocalDateTime loop, List<Map<String, String>> map) {
		String[] colors = {"#FF3B30", "#FF9500", "#FFCC00", "#4CD964", "#5AC8FA", "#007AFF"};
		int co = 0;
		List<Map<String, Object>> datasets = new ArrayList<>();
		for (Bundle b : bundles) {
			Map<String, Object> dataset = Maps.newHashMap();
			String name = b.getName();
			dataset.put("label", name);
			dataset.put("borderWidth", "2");
			dataset.put("borderColor", colors[co]);
			dataset.put("backgroundColor", colors[co++]);
			
			dataset.put("data", getChartDatasYyyymm(end, loop, map, name));
			datasets.add(dataset);
		}
		return datasets;
	}
	
	private List<Map<String, Object>> getPieDatasetByBundle(List<Bundle> bundles, LocalDateTime end, List<Map<String, String>> map) {
		String[] colors = {"#FF3B30", "#FF9500", "#FFCC00", "#4CD964", "#5AC8FA", "#007AFF"};
		List<String> colorList = Arrays.asList(colors);
		List<Map<String, Object>> datasets = new ArrayList<>();
		List<Long> datas = new ArrayList<>();
		Map<String, Object> dataset = Maps.newHashMap();
		
		for (Bundle b : bundles) {
			datas.add(getCounting(map, "name", b.getName()));
		}
		dataset.put("borderWidth", "2");
		dataset.put("borderColor", colorList);
		dataset.put("backgroundColor", colorList);
		dataset.put("data", datas);
		datasets.add(dataset);
		
		return datasets;
	}
	
	private Specification<Sample> getSampleWhere(Map<String, String> params) {
		return Specification
				.where(SampleSpecification.createdDateOneMonth(params));
	}
	
	private Specification<Sample> getCompletedWhere(Map<String, String> params) {
		// return Specification
		// 		.where(ResultSpecification.modifiedDateOneMonth(params))
		// 		.and(ResultSpecification.statusCodeIn(Arrays.asList(
		// 					StatusCode.ANLS_CMPL
		// 		)));
		return null;
	}
	
	private Specification<Sample> getReportedWhere(Map<String, String> params) {
		// return Specification
		// 		.where(ResultSpecification.modifiedDateOneMonth(params))
		// 		.and(ResultSpecification.statusCodeIn(Arrays.asList(
		// 					StatusCode.ANLS_CMPL
		// 		)));
		return null;
	}

	private List<String> getChartLabels(LocalDateTime end, LocalDateTime loop) {
		List<String> labels = new ArrayList<>();
		while (end.getMonthValue() >= loop.getMonthValue()) {
			String yyyymm = loop.format(yyyymmFormat);
			labels.add(yyyymm);
			loop = loop.plusMonths(1);
		}
		return labels;
	}
	
	private List<Long> getChartDatasYyyymm(LocalDateTime end, LocalDateTime loop, List<Map<String, String>> map, String name) {
		List<Long> datas = new ArrayList<>();
		while (end.getMonthValue() >= loop.getMonthValue()) {
			String yyyymm = loop.format(yyyymmFormat);
			long c = getCounting(map, "yyyymm", yyyymm, "name", name);
			datas.add(c);
			loop = loop.plusMonths(1);
		}
		return datas;
	}
	
	private Long getCounting(List<Map<String, String>> map, String key1, String value1) {
		return map.stream().filter(m -> value1.equals(m.get(key1))).collect(Collectors.counting());
	}
	
	private Long getCounting(List<Map<String, String>> map, String key1, String value1, String key2, String value2) {
		return map.stream().filter(m -> value1.equals(m.get(key1)) && value2.equals(m.get(key2))).collect(Collectors.counting());
	}
	
}
