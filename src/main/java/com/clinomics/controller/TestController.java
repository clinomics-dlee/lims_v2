package com.clinomics.controller;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinomics.enums.ResultCode;
import com.clinomics.service.TestService;
import com.google.common.collect.Maps;

@RequestMapping("/test")
@Controller
public class TestController {
	
	@Autowired
	TestService testService;
	
	@PostMapping("/saveall")
	@ResponseBody
	public Map<String, String> saveall(@RequestBody List<Map<String, String>> datas) {
		
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// #. 바코드 중복체크
		for (Map<String, String> data : datas) {
			String barcode = StringUtils.stripToEmpty(data.get("barcode"));
			if (barcode.length() > 0) {
				long count = testService.searchExistsBarcode(barcode);
				if (count > 0) {
					Map<String, String> rtn = Maps.newHashMap();
					rtn.put("result", ResultCode.FAIL_DUPL_VALUE.get());
					rtn.put("message", ResultCode.FAIL_DUPL_VALUE.getMsg() + "[" + barcode + "]");
					return rtn;
				}
			}
		}
		return testService.saveFromList(datas, userDetails.getUsername());
	}

}
