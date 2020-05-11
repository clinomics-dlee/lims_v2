package com.clinomics.controller.setting;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.service.setting.BundleService;

@Controller
@RequestMapping("/set/bnd")
public class BundleController {
	
	@Autowired
	BundleService bundleService;

	@GetMapping("/get")
	@ResponseBody
	public Map<String, Object> get(@RequestParam Map<String, String> params) {
		return bundleService.selectAll(params);
	}

	@GetMapping("/get/{id}")
	@ResponseBody
	public Bundle get(@PathVariable int id) {
		return bundleService.selectOne(id);
	}

	@PostMapping("/add")
	@ResponseBody
	public Map<String, String> add(@RequestBody Map<String, String> datas) {
		return bundleService.save(datas);
	}

	@PostMapping("/save")
	@ResponseBody
	public Map<String, String> save(@RequestBody Map<String, String> datas) {
		return bundleService.save(datas);
	}
}
