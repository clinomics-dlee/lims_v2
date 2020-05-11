package com.clinomics.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinomics.service.setting.BundleService;
import com.clinomics.service.CalendarService;
import com.clinomics.service.ChartService;

@RequestMapping("/")
@Controller
public class DashboardController {

	@Autowired
	CalendarService calendarService;

	@Autowired
	ChartService chartService;

	@Autowired
	BundleService bundleService;
	
	@GetMapping()
	public String calendar(Model model) {
		model.addAttribute("bundles", bundleService.selectAll());
		return "calendar";
	}

	@GetMapping("/calendar/get/statistics")
	@ResponseBody
	public Map<String, Object> calendar(@RequestParam Map<String, String> params) {
		return calendarService.selectCountByMonthly(params);
	}

	@GetMapping("/popup/registered")
	@ResponseBody
	public Map<String, Object> complete(@RequestParam Map<String, String> params) {
		return calendarService.selectRegistered(params);
	}

	@GetMapping("/popup/analysis")
	@ResponseBody
	public Map<String, Object> analysis(@RequestParam Map<String, String> params) {
		return calendarService.selectAnalysis(params);
	}

	@GetMapping("/popup/completed")
	@ResponseBody
	public Map<String, Object> completed(@RequestParam Map<String, String> params) {
		return calendarService.selectCompleted(params);
	}

	@GetMapping("/popup/reported")
	@ResponseBody
	public Map<String, Object> reported(@RequestParam Map<String, String> params) {
		return calendarService.selectReported(params);
	}

	@GetMapping("/chart/get/statistics")
	@ResponseBody
	public Map<String, Object> chart(@RequestParam Map<String, String> params) {
		return chartService.selectCountByMonthly(params);
	}
}