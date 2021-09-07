package com.clinomics.controller;

import java.util.List;
import java.util.Map;

import com.clinomics.enums.ResultCode;
import com.clinomics.service.TestService;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/test")
@Controller
public class TestController {
	
	// @Autowired
	// TestService testService;
	
	// @PostMapping("/save")
	// @ResponseBody
	// public Map<String, String> save(@RequestBody Map<String, String> datas) {
		
	// 	UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	// 	datas.put("memberId", userDetails.getUsername());
		
	// 	return testService.save(datas, false);
	// }
	
	// @PostMapping("/saveall")
	// @ResponseBody
	// public Map<String, String> saveall(@RequestBody List<Map<String, String>> datas) {
		
	// 	UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	// 	// #. 바코드 중복체크
	// 	for (Map<String, String> data : datas) {
	// 		String barcode = StringUtils.stripToEmpty(data.get("barcode"));
	// 		if (barcode.length() > 0) {
	// 			long count = testService.searchExistsBarcode(barcode);
	// 			if (count > 0) {
	// 				Map<String, String> rtn = Maps.newHashMap();
	// 				rtn.put("result", ResultCode.FAIL_DUPL_VALUE.get());
	// 				rtn.put("message", ResultCode.FAIL_DUPL_VALUE.getMsg() + "[" + barcode + "]");
	// 				return rtn;
	// 			}
	// 		}
	// 	}
	// 	return testService.saveFromList(datas, userDetails.getUsername());
	// }

	// @GetMapping("/list")
	// @ResponseBody
	// public Map<String, Object> outputList(@RequestParam Map<String, String> params) {
	// 	if (params.containsKey("statusCode") && !params.get("statusCode").toString().isEmpty()) {
	// 		return testService.findByModifiedDate(params, params.get("statusCode") + "");
	// 	}

	// 	return testService.findByModifiedDate(params, 600);
	// }

	// @GetMapping("/itemby/sample/{id}")
	// @ResponseBody
	// public Map<String, Object> getItemBySample(@PathVariable String id) {
	// 	return testService.findSampleItemBySample(id);
	// }

	// @GetMapping("/databy/sample/{id}")
	// @ResponseBody
	// public Map<String, Object> getDataBySample(@PathVariable String id) {
	// 	return testService.findSampleDataBySampleId(id);
	// }

	// @PostMapping("/approve")
	// @ResponseBody
	// public Map<String, String> outputApprove(@RequestBody List<Integer> ids) {
	// 	UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
	// 	return testService.outputApprove(ids, userDetails.getUsername());
	// }
}
