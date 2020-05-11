package com.clinomics.controller.menu;

import com.clinomics.service.setting.BundleService;

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
		return path1 + "/" + path2;
	}
}