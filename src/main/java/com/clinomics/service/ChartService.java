package com.clinomics.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Service
public class ChartService {

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	BundleRepository bundleRepository;

	@PersistenceContext
	private EntityManager entityManager;

	String[] colors = {"#FF3B30", "#FF9500", "#FFCC00", "#4CD964", "#5AC8FA", "#007AFF", "#5856D6"
						, "#FF6C64", "#FFD940", "#F7B048", "#79E38B", "#83D6FB", "#5DA5E8", "#8280E0"};
	
	DateTimeFormatter yyyymmFormat = DateTimeFormatter.ofPattern("yyyyMM");

	public Bundle selectOne(int id) {
		return bundleRepository.findById(id).orElse(new Bundle());
	}

	public List<Bundle> selectAll() {
		return bundleRepository.findAll();
	}

	public Map<String, Object> selectCountByMonthly(Map<String, String> params) {
		System.out.println("★★★★★★★★ params=" + params.toString());
		String paramStart = params.get("start") + "";
		String paramEnd = params.get("end") + "";
		String yyyymm = (params.get("yyyymm") + "").replace("-", "");
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime start = LocalDateTime.parse(paramStart + "-01 00:00:00", formatter);
		LocalDateTime end = LocalDateTime.parse(paramEnd + "-01 00:00:00", formatter);
		end = end.plusMonths(1).minusSeconds(1);
		
		String sDate = start.format(formatter);
		String fDate = end.format(formatter);

		String strBundles = params.get("bundleId") + "";
		
		List<Bundle> bundles = Lists.newArrayList();
		List<Integer> bundleIds = Lists.newArrayList();
		if (strBundles.length() > 0) {
			List<String> lstBundles = Arrays.asList((params.get("bundleId") + "").split(","));
			bundleIds = lstBundles.stream().map(b -> NumberUtils.toInt(b)).collect(Collectors.toList());
			
			bundles = bundleRepository.findByIdInAndIsActiveTrueOrderBySort(bundleIds);
		} else {
			bundles = bundleRepository.findByIsActiveTrueOrderBySort();
			bundleIds = bundles.stream().map(b -> b.getId()).collect(Collectors.toList());
		}
		
//		List<Sample> sample = sampleRepository.findAll(getSampleWhere(params));
//		List<Sample> complete = sampleRepository.findAll(getCompletedWhere(params));
//		List<Sample> reporte = sampleRepository.findAll(getReportedWhere(params));
		
		List<Map<String, String>> chart1 = sampleRepository.findChartDataByCreatedDate(sDate, fDate, bundleIds, Arrays.asList(StatusCode.class.getEnumConstants()).stream().map(e -> e.toString()).collect(Collectors.toList()));
		List<Map<String, String>> chart2 = sampleRepository.findChartDataByAnlsCmplDate(sDate, fDate, bundleIds, Arrays.asList(new StatusCode[] {
				StatusCode.S460_ANLS_CMPL
				, StatusCode.S600_JDGM_APPROVE
				, StatusCode.S700_OUTPUT_WAIT
				, StatusCode.S710_OUTPUT_CMPL
				, StatusCode.S810_RE_OUTPUT_CMPL
				, StatusCode.S900_OUTPUT_CMPL
			}).stream().map(e -> e.getKey()).collect(Collectors.toList()));
		List<Map<String, String>> chart3 = sampleRepository.findChartDataByOutputCmplDate(sDate, fDate, bundleIds, Arrays.asList(
				StatusCode.S710_OUTPUT_CMPL
				, StatusCode.S810_RE_OUTPUT_CMPL
				, StatusCode.S900_OUTPUT_CMPL
			).stream().map(e -> e.getKey()).collect(Collectors.toList()));
		
		
//		List<Map<String, String>> mapSample = chart1.stream()
//			.map(s -> {
//				Map<String, String> t = Maps.newHashMap();
//				t.put("yyyymm", s.getCreatedDate().format(yyyymmFormat));
//				t.put("name", s.getBundle().getName());
//				return t;
//			}).collect(Collectors.toList());
//		
//		List<Map<String, String>> mapSampleComplete = complete.stream()
//			.map(s -> {
//				Map<String, String> t = Maps.newHashMap();
//				LocalDateTime acd = s.getAnlsCmplDate();
//				String yyyymm = (acd == null ? "" : acd.format(yyyymmFormat));
//				t.put("yyyymm", yyyymm);
//				t.put("name", s.getBundle().getName());
//				return t;
//			}).collect(Collectors.toList());
//		
//		List<Map<String, String>> mapResult = reporte.stream()
//			.map(s -> {
//				Map<String, String> t = Maps.newHashMap();
//				LocalDateTime ocd = s.getOutputCmplDate();
//				String yyyymm = (ocd == null ? "" : ocd.format(yyyymmFormat));
//				t.put("yyyymm", yyyymm);
//				t.put("name", s.getBundle().getName());
//				return t;
//			}).collect(Collectors.toList());
		
		
		Map<String, Object> rtn1 = Maps.newHashMap();
		LocalDateTime loop = start;
		
		rtn1.put("labels", getChartLabels(end, loop));
		rtn1.put("datasets", getDatasetByBundle(bundles, end, loop, chart1));
		
		Map<String, Object> rtn2 = Maps.newHashMap();
		loop = start;
		
		rtn2.put("labels", getChartLabels(end, loop));
		rtn2.put("datasets", getDatasetByBundle(bundles, end, loop, chart2));
		
		Map<String, Object> rtn3 = Maps.newHashMap();
		loop = start;
		
		rtn3.put("labels", getChartLabels(end, loop));
		rtn3.put("datasets", getDatasetByBundle(bundles, end, loop, chart3));
		
		Map<String, Object> rtn4 = Maps.newHashMap();
		loop = start;

		
		rtn4.put("labels", bundles.stream().map(b -> b.getName()).collect(Collectors.toList()));
		rtn4.put("datasets", getPieDatasetByBundle(bundles, end, chart1));
			
		
		Map<String, Object> rtn = Maps.newHashMap();
		rtn.put("chart1", rtn1);
		rtn.put("chart2", rtn2);
		rtn.put("chart3", rtn3);
		rtn.put("chart4", rtn4);
		
		return rtn;
	}
	
	private List<Map<String, Object>> getDatasetByBundle(List<Bundle> bundles, LocalDateTime end, LocalDateTime loop, List<Map<String, String>> map) {
		int co = 0;
		List<Map<String, Object>> datasets = new ArrayList<>();
		for (Bundle b : bundles) {
			Map<String, Object> dataset = Maps.newHashMap();
			String name = b.getName();
			dataset.put("label", name);
			dataset.put("borderWidth", "2");
			dataset.put("borderColor", colors[co % colors.length]);
			dataset.put("backgroundColor", colors[co % colors.length]);
			co++;
			
			dataset.put("data", getChartDatasYyyymm(end, loop, map, name));
			datasets.add(dataset);
		}
		return datasets;
	}
	
	private List<Map<String, Object>> getPieDatasetByBundle(List<Bundle> bundles, LocalDateTime end, List<Map<String, String>> map) {
		List<String> colorList = new ArrayList<String>();
		List<Map<String, Object>> datasets = new ArrayList<>();
		List<Long> datas = new ArrayList<>();
		Map<String, Object> dataset = Maps.newHashMap();
		
		int co = 0;
		for (Bundle b : bundles) {
			datas.add(getCounting(map, "name", b.getName()));
			colorList.add(colors[co % colors.length]);
			co++;
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
			.where(SampleSpecification.betweenDate(params))
			.and(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.hNameIn(params))
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.statusCodeGt(20));
	}
	
	private Specification<Sample> getCompletedWhere(Map<String, String> params) {
		return Specification
			.where(SampleSpecification.customDateBetween("anlsCmplDate", params))
			.and(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.hNameIn(params))
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.statusIn(
				Arrays.asList(new StatusCode[] {
					StatusCode.S460_ANLS_CMPL
					, StatusCode.S600_JDGM_APPROVE
					, StatusCode.S700_OUTPUT_WAIT
					, StatusCode.S710_OUTPUT_CMPL
					, StatusCode.S810_RE_OUTPUT_CMPL
					, StatusCode.S900_OUTPUT_CMPL
				})
			));
	}
	
	private Specification<Sample> getReportedWhere(Map<String, String> params) {
		return Specification
			.where(SampleSpecification.customDateBetween("outputCmplDate", params))
			.and(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.bundleId(params))
			.and(SampleSpecification.hNameIn(params))
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.statusIn(Arrays.asList(
				StatusCode.S710_OUTPUT_CMPL
				, StatusCode.S810_RE_OUTPUT_CMPL
				, StatusCode.S900_OUTPUT_CMPL
			)));
	}

	private List<String> getChartLabels(LocalDateTime end, LocalDateTime loop) {
		List<String> labels = new ArrayList<>();
		//while (end.getMonthValue() >= loop.getMonthValue()) {
		while (end.format(yyyymmFormat).compareTo(loop.format(yyyymmFormat)) >= 0) {
			String yyyymm = loop.format(yyyymmFormat);
			labels.add(yyyymm);
			loop = loop.plusMonths(1);
		}
		return labels;
	}
	
	private List<Long> getChartDatasYyyymm(LocalDateTime end, LocalDateTime loop, List<Map<String, String>> map, String name) {
		List<Long> datas = new ArrayList<>();
		Map<String, String> zero = Maps.newHashMap();
		zero.put("count", "0");
		//while (end.getMonthValue() >= loop.getMonthValue()) {
		while (end.format(yyyymmFormat).compareTo(loop.format(yyyymmFormat)) >= 0) {
			String yyyymm = loop.format(yyyymmFormat);
			//long c = getCounting(map, "yyyymm", yyyymm, "name", name);
			Optional<Map<String, String>> oma = map.stream()
					.filter(
							m ->
							m.getOrDefault("yyyymm", "").equals(yyyymm) && m.getOrDefault("name", "").equals(name)
					).findAny();
			
			
			datas.add(NumberUtils.toLong(oma.orElse(zero).get("count")));
			loop = loop.plusMonths(1);
		}
		
		return datas;
	}
	
	private Long getCounting(List<Map<String, String>> map, String key1, String value1) {
		
		List<Long> tmp = map.stream().filter(m -> value1.equals(m.get(key1))).map(m -> NumberUtils.toLong(m.get("count"))).collect(Collectors.toList());
		
		
		return tmp.stream().collect(Collectors.summingLong(Long::longValue));
	}
	
}
