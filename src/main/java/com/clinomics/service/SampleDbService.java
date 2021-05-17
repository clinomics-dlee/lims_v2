package com.clinomics.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleHistory;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleHistoryRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.CustomIndexPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
public class SampleDbService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Value("${externalApi.url}")
	private String externalApiUrl;
	@Value("${externalApi.tokenName}")
	private String externalApiTokenName;
	@Value("${externalApi.token}")
	private String externalApiToken;
	
	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	SampleHistoryRepository sampleHistoryRepository;

	@Autowired
	BundleRepository bundleRepository;
	
	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	DataTableService dataTableService;

	@Autowired
	VariousFieldsService variousFieldsService;
	
	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	CustomIndexPublisher customIndexPublisher;

	public Map<String, Object> getSampleDbList(Map<String, String> params) {
		logger.info("getDbList Params=" + params.toString());
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
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.orderBy(params));
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;

		List<Map<String, Object>> sList = new ArrayList<Map<String, Object>>();
		// for (Sample s : list) {
		// 	String id = s.getId();
		// 	Map<String, Object> map = Maps.newHashMap();
		// 	map.put("sample", s);
		// 	// Result r = resultRepository.getOne(NumberUtils.toInt(id));

		// 	Specification<Result> w = Specification.where(ResultSpecification.lastResult(id));
		// 	List<Result> rs = resultRepository.findAll(w);
		// 	Result r = new Result();
		// 	if (rs.size() > 0) {
		// 		r = rs.get(0);
		// 	}
		// 	map.put("result", r);
		// 	sList.add(map);
		// }

		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrderingForMap(sList);

		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, sList, header);
	}

	public Map<String, Object> getSampleHistory(Map<String, String> params, String id) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);
		
		long total = sampleHistoryRepository.countBySample_Id(id);
		List<Order> orders = Arrays.asList(new Order[] { Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		List<SampleHistory> list = sampleHistoryRepository.findBySample_Id(id, pageable);
		sampleItemService.filterItemsAndOrderingFromHistory(list);
		long filtered = total + 1;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list);
	}

	public Map<String, Object> find(Map<String, String> params, int statusCodeNumber) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);
		
		// #. paging 관련 객체
		Pageable pageable = PageRequest.of(pageNumber, pageRowCount);
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.hNameIn(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusCodeGt(statusCodeNumber))
					.and(SampleSpecification.orderBy(params));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, Object> find(Map<String, String> params, String statusCode) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb") + "", 0);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc") + "", 10);
		
		// #. paging 관련 객체
		Pageable pageable = PageRequest.of(pageNumber, pageRowCount);
		long total;
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusEqual(StatusCode.valueOf(statusCode)))
					.and(SampleSpecification.orderBy(params));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
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
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenModifiedDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusCodeGt(statusCodeNumber))
					.and(SampleSpecification.orderBy(params));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
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
		
		Specification<Sample> where = Specification
					.where(SampleSpecification.betweenModifiedDate(params))
					.and(SampleSpecification.bundleId(params))
					.and(SampleSpecification.keywordLike(params))
					.and(SampleSpecification.bundleIsActive())
					.and(SampleSpecification.statusEqual(StatusCode.valueOf(statusCode)))
					.and(SampleSpecification.orderBy(params));
					
		
		total = sampleRepository.count(where);
		Page<Sample> page = sampleRepository.findAll(where, pageable);
		
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list);
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public Map<String, List<Map<String, String>>> getMarkerInfo(List<String> reportTypes) throws Exception {
		Map<String, List<Map<String, String>>> rtn = Maps.newHashMap();
		BufferedReader br = null;
		try {
			String reportTypeString = String.join(",", reportTypes);
			URL url = new URL(externalApiUrl + "getMarker.php?reporttype=" + reportTypeString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(5000); // InputStream 읽어 오는 Timeout 시간 설정
			con.addRequestProperty(externalApiTokenName, externalApiToken); // key값 설정
			con.addRequestProperty("Content-Type", "application/json"); // key값 설정
			con.setRequestMethod("GET");

			con.setDoOutput(false);

			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				ObjectMapper mapper = new ObjectMapper();
				
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
				
				Map<Object, Object> jsonMap = mapper.readValue(sb.toString(), new TypeReference<Map<Object, Object>>(){});
				if (jsonMap != null && !jsonMap.keySet().isEmpty()) {
					// #. jsonMap이 있는경우 데이터 가공
					for (String reportType : reportTypes) {
						List<Map<String, String>> list = new ArrayList<Map<String, String>>();
						List<Map<String, String>> markerInfos = (List)jsonMap.get(reportType);
						for (Map<String, String> info : markerInfos) {
							Map<String, String> ri = Maps.newHashMap();
							String name = info.get("marker");
							String refValue = info.get("refer");
							String altValue = info.get("factor");
							
							ri.put("name", name);
							ri.put("nameCode", name);
							ri.put("refValue", refValue);
							ri.put("altValue", altValue);
							
							list.add(ri);
						}
						rtn.put(reportType, list);
					}
				}
			} else {
				logger.info(con.getResponseMessage());
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return rtn;
	}
}
