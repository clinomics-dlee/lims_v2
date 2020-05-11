package com.clinomics.controller;

import java.util.Map;

import com.clinomics.service.SampleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/exp")
@RestController
public class ExpController {
    @Autowired
    SampleService sampleService;


	@GetMapping("/rdy/get")
	@ResponseBody
	public Map<String, Object> get(@RequestParam Map<String, String> params) {
		return sampleService.findAll();
	}
}
