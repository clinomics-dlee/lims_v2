package com.clinomics.controller.setting;

import java.util.Map;

import com.clinomics.service.SampleItemService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/set/item")
public class SampleItemController {
	
	@Autowired
	SampleItemService sampleItemService;
	
	@GetMapping("/mng")
	public String view(Model model) {
		return "setting/item";
	}

	@GetMapping("/get")
	@ResponseBody
	public Map<String, Object> get(@RequestParam Map<String, String> params) {
		return sampleItemService.selectAll(params);
	}

	@PostMapping("/add")
	@ResponseBody
	public Map<String, String> add(@RequestBody Map<String, String> datas) {
		return sampleItemService.save(datas);
	}

	@PostMapping("/save")
	@ResponseBody
	public Map<String, String> save(@RequestBody Map<String, String> datas) {
		return sampleItemService.save(datas);
	}
}
