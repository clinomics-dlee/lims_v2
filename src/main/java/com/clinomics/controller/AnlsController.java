package com.clinomics.controller;

import java.util.List;
import java.util.Map;

import com.clinomics.service.AnlsService;
import com.clinomics.service.SampleDbService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/anls")
@RestController
public class AnlsController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
    @Autowired
    AnlsService anlsService;

	@Autowired
    SampleDbService sampleDbService;

	@GetMapping("/rdy/get")
	public Map<String, Object> getRdy(@RequestParam Map<String, String> params) {
		return anlsService.findMappingSampleByAnlsRdyStatus(params);
	}

	@PostMapping("/rdy/start")
	public Map<String, String> startAnls(@RequestParam("mappingNos[]") List<String> mappingNos) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return anlsService.startAnls(mappingNos, userDetails.getUsername());
	}
}
