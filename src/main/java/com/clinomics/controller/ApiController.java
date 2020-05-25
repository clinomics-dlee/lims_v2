package com.clinomics.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clinomics.service.OutputService;
import com.clinomics.util.EmailSender;
import com.google.common.collect.Maps;

@RequestMapping("/rest")
@RestController
public class ApiController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	OutputService outputService;
	
	@Autowired
	EmailSender emailSender;
	
	@RequestMapping(value = "/result/get")
	public Map<String, Object> getResultWithWaitStatus(@RequestParam Map<String, String> params) {
		return outputService.getResultsForRest(params);
	}
	
	@RequestMapping(value = "/mail/test")
	public String sendMailTest() {
		List<Map<String, String>> test = new ArrayList<>();
		Map<String, String> map = Maps.newHashMap();
		map.put("chipNumber", "534252345234523452");
		map.put("sampleId", "TEST-001-001,TEST-001-002");
		map.put("message", "TEST MAIL");
		test.add(map);
		emailSender.sendMailToFail(test);
		return "asdf";
	}
}
