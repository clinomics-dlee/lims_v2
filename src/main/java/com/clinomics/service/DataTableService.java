package com.clinomics.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

@Service
public class DataTableService {
	
	public Map<String, Object> getDataTableMap(int draw, int pageNumber, long total, long filtered, List<?> data) {
		Map<String, Object> rtn = Maps.newHashMap();
		
		rtn.put("draw", draw);
		rtn.put("pageNumber", pageNumber);
		rtn.put("recordsTotal", total);
		rtn.put("recordsFiltered", filtered);
		rtn.put("data", data);
		
		return rtn;
	}
	
	public Map<String, Object> getDataTableMap(int draw, int pageNumber, long total, long filtered, List<?> data, List<?> header) {
		Map<String, Object> rtn = Maps.newHashMap();
		
		rtn.put("draw", draw);
		rtn.put("pageNumber", pageNumber);
		rtn.put("recordsTotal", total);
		rtn.put("recordsFiltered", filtered);
		rtn.put("data", data);
		rtn.put("header", header);
		
		return rtn;
	}
}
