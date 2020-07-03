package com.clinomics.controller.menu;

import java.util.Map;

import com.clinomics.enums.ChipTypeCode;
import com.clinomics.enums.GenotypingMethodCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.service.setting.BundleService;
import com.google.common.collect.Maps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
 * @feature 기능별로 view 페이지 정렬
 * 
 * 
 */
@Controller
public class PageController {

	@Autowired
	BundleService bundleService;
	
	@GetMapping()
	public String calendar(Model model) {
		Map<String, String> statusCodeMap = Maps.newLinkedHashMap();
		for (StatusCode statusCode : StatusCode.values()) {
			statusCodeMap.put(statusCode.getKey(), statusCode.getValue());
		}
		model.addAttribute("statusCodes", statusCodeMap);
		model.addAttribute("bundles", bundleService.selectAll());
		return "calendar";
	}

	@GetMapping("/chart")
	public String chart(Model model) {
		model.addAttribute("bundles", bundleService.selectAll());
		return "chart";
	}
	
	@GetMapping("/p/{path1}/{path2}")
	public String intake(@PathVariable String path1, @PathVariable String path2, Model model) {
		model.addAttribute("bundles", bundleService.selectAll());
		Map<String, String> statusCodeMap = Maps.newHashMap();
		for (StatusCode statusCode : StatusCode.values()) {
			statusCodeMap.put(statusCode.getKey(), statusCode.getValue());
		}
		model.addAttribute("statusCodes", statusCodeMap);

		Map<String, String> genotypingMethodCodeMap = Maps.newHashMap();
		for (GenotypingMethodCode code : GenotypingMethodCode.values()) {
			genotypingMethodCodeMap.put(code.getKey(), code.getValue());
		}

		Map<String, String> chipTypeCodeMap = Maps.newHashMap();
		for (ChipTypeCode code : ChipTypeCode.values()) {
			chipTypeCodeMap.put(code.getKey(), code.getValue());
		}
		model.addAttribute("statusCodes", statusCodeMap);
		model.addAttribute("genotypingMethodCodes", genotypingMethodCodeMap);
		model.addAttribute("chipTypeCodes", chipTypeCodeMap);
		return path1 + "/" + path2;
	}
}