package com.clinomics.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.clinomics.service.setting.BundleService;
import com.clinomics.service.InputService;
import com.clinomics.service.SampleDbService;
import com.clinomics.enums.StatusCode;
import com.clinomics.service.InputExcelService;
import com.clinomics.service.SampleItemService;

@RequestMapping("/input")
@Controller
public class InputController {

	@Autowired
	InputService inputService;
	
	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	InputExcelService inputExcelService;

	@Autowired
	SampleDbService sampleDbService;
	
	@Autowired
	BundleService bundleService;
	
	@GetMapping("/rvc")
	@ResponseBody
	public Map<String, Object> rvc(@RequestParam Map<String, String> params) {
		return inputService.find(params, Arrays.asList(new StatusCode[] { StatusCode.S000_INPUT_REG }));
	}
	
	@GetMapping("/aprv")
	@ResponseBody
	public Map<String, Object> aprv(@RequestParam Map<String, String> params) {
		if (params.containsKey("complete")) {
			if ("0".equals(params.get("complete"))) {
				return sampleDbService.find(params, StatusCode.S020_INPUT_RCV);
			}
		}
		return sampleDbService.find(params, 40);
	}
	
	@GetMapping("/db")
	@ResponseBody
	public Map<String, Object> db(@RequestParam Map<String, String> params) {
		return sampleDbService.find(params, 0);
	}
	
	@PostMapping("/save")
	@ResponseBody
	public Map<String, String> save(@RequestBody Map<String, String> datas) {
		
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		datas.put("memberId", userDetails.getUsername());
		
		return inputService.save(datas, false);
	}
	
	@PostMapping("/saveall")
	@ResponseBody
	public Map<String, String> saveall(@RequestBody List<Map<String, String>> datas) {
		
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
		return inputService.saveFromList(datas, userDetails.getUsername());
	}
	
	// @PostMapping("/receive")
	// @ResponseBody
	// public Map<String, String> receive(@RequestBody List<Integer> ids) {
	// 	UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
	// 	return inputService.receive(ids, userDetails.getUsername());
	// }
	
	@PostMapping("/approve")
	@ResponseBody
	public Map<String, String> approve(@RequestBody List<Integer> ids) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
		return inputService.approve(ids, userDetails.getUsername());
	}
	
	@PostMapping("/delete")
	@ResponseBody
	public Map<String, String> delete(@RequestBody List<Integer> ids) {
		
		return inputService.delete(ids);
	}
	
	@GetMapping("/itemby/sample/{id}")
	@ResponseBody
	public Map<String, Object> getItemBySample(@PathVariable String id) {
		return sampleItemService.findSampleItemBySample(id);
	}
	
	@GetMapping("/itemby/bundle/{id}")
	@ResponseBody
	public Map<String, Object> getItemByBundle(@PathVariable String id) {
		return sampleItemService.findSampleItemByBundle(id);
	}
	
	@GetMapping("/excel/form")
	@ResponseBody
	public void exportExcelForm(@RequestParam Map<String, String> params, HttpServletResponse response) {
		XSSFWorkbook xlsx = inputExcelService.exportExcelForm(params);
		requestExcel(xlsx, response);
	}
	
	private void requestExcel(XSSFWorkbook xlsx, HttpServletResponse response) {
		response.setHeader("Content-Disposition", "attachment;filename=sample.xlsx");
        // 엑셀파일명 한글깨짐 조치
        response.setHeader("Content-Transfer-Encoding", "binary;");
        response.setHeader("Pragma", "no-cache;");
        response.setHeader("Expires", "-1;");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try {
	        ServletOutputStream out = response.getOutputStream();
	        
        	xlsx.write(out);
        	xlsx.close();
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
