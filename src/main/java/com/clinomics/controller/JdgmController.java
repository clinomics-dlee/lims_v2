package com.clinomics.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinomics.service.setting.BundleService;
import com.clinomics.service.JdgmService;
import com.clinomics.service.SampleDbService;
import com.clinomics.enums.StatusCode;
import com.clinomics.service.InputExcelService;
import com.clinomics.service.SampleItemService;

@RequestMapping("/jdgm")
@Controller
public class JdgmController {

	@Autowired
	JdgmService jdgmService;
	
	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	InputExcelService inputExcelService;

	@Autowired
	SampleDbService sampleDbService;
	
	@Autowired
	BundleService bundleService;
	
	@GetMapping("/aprv")
	@ResponseBody
	public Map<String, Object> rvc(@RequestParam Map<String, String> params) {
		return jdgmService.find(params, Arrays.asList(new StatusCode[] { StatusCode.S460_ANLS_CMPL }));
	}
	
	@PostMapping("/approve")
	@ResponseBody
	public Map<String, String> approve(@RequestBody List<Integer> ids) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
		return jdgmService.approve(ids, userDetails.getUsername());
	}
}
