// package com.clinomics.controller;

// import java.util.List;
// import java.util.Map;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.ResponseBody;

// import com.clinomics.enums.StatusCode;
// import com.clinomics.service.BundleService;
// import com.clinomics.service.CompleteService;
// import com.google.common.collect.Maps;

// @RequestMapping("/work/complete")
// @Controller

// public class CompleteController {

// 	@Autowired
// 	CompleteService completeService;

// 	@Autowired
// 	BundleService bundleService;
	
// 	@GetMapping()
// 	public String db(Model model) {
// 		model.addAttribute("bundles", bundleService.selectAll());
// 		Map<String, String> statusCodeMap = Maps.newHashMap();
// 		for (StatusCode statusCode : StatusCode.values()) {
// 			statusCodeMap.put(statusCode.getKey(), statusCode.getValue());
// 		}
// 		model.addAttribute("statusCodes", statusCodeMap);
		
// 		return "work/complete";
// 	}

// 	@GetMapping("/get")
// 	@ResponseBody
// 	public Map<String, Object> getList(@RequestParam Map<String, Object> params) {
// 		return completeService.getCompleteList(params);
// 	}

// 	@GetMapping("/history/{id}")
// 	@ResponseBody
// 	public Map<String, Object> history(@RequestParam Map<String, Object> params, @PathVariable String id) {
// 		return completeService.getSampleHistory(params, id);
// 	}
	
// 	@GetMapping("/recreate/pdf/{id}")
// 	@ResponseBody
// 	public Map<String, Object> reissue(@PathVariable String id) {
// 		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
// 		return completeService.changeResultStatusForRecreatedWaitPdf(id, userDetails.getUsername());
// 	}
	
// 	@PostMapping("/recreate/pdf/multi")
// 	@ResponseBody
// 	public Map<String, Object> reissueMulti(@RequestParam("resultIds[]") List<String> resultIds) {
// 		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
// 		return completeService.changeResultStatusForRecreatedWaitPdf(resultIds, userDetails.getUsername());
// 	}
	
// }
