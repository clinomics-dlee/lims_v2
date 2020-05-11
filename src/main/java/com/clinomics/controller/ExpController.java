package com.clinomics.controller;

import java.util.List;
import java.util.Map;

import com.clinomics.service.DleeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/exp")
@RestController
public class ExpController {
    @Autowired
    DleeService dleeService;


	@GetMapping("/rdy/get")
	public Map<String, Object> getRdy(@RequestParam Map<String, String> params) {
		return dleeService.findSampleByExpRdyStatus(params);
	}

	@PostMapping("/rdy/start")
	public Map<String, String> startExp(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return dleeService.startExp(sampleIds, userDetails.getUsername());
	}
}
