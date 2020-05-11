package com.clinomics.controller.setting;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinomics.service.setting.BundleService;
import com.clinomics.service.setting.ProductService;

@Controller
@RequestMapping("/set/prd")
public class ProductController {
	
	@Autowired
	ProductService productService;
	
	@Autowired
	BundleService bundleService;

	@GetMapping("/get/bnd")
	@ResponseBody
	public Map<String, Object> getBundle(@RequestParam Map<String, String> params) {
		return productService.selectBundleAll(params);
	}

	@GetMapping("/get")
	@ResponseBody
	public Map<String, Object> getProduct(@RequestParam Map<String, String> params) {
		return productService.selectProductAll(params);
	}

	@PostMapping("/add")
	@ResponseBody
	public Map<String, String> add(@RequestBody Map<String, String> datas) {
		return bundleService.save(datas);
	}

	@PostMapping("/save")
	@ResponseBody
	public Map<String, String> save(@RequestBody Map<String, String> datas) {
		return productService.save(datas);
	}

	@PostMapping("/add/item")
	@ResponseBody
	public Map<String, String> addSampleItem(@RequestBody Map<String, String> datas) {
		return productService.addSampleItem(datas);
	}
}
