package com.clinomics.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.clinomics.service.AnlsExcelService;
import com.clinomics.service.AnlsService;
import com.clinomics.service.SampleDbService;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RequestMapping("/anls")
@RestController
public class AnlsController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
    @Autowired
    AnlsService anlsService;

	@Autowired
    AnlsExcelService anlsExcelService;

	@Autowired
    SampleDbService sampleDbService;

	@GetMapping("/rdy/get")
	public Map<String, Object> getRdy(@RequestParam Map<String, String> params) {
		return anlsService.findMappingSampleByAnlsRdyStatus(params);
	}

	@GetMapping("/rdy/celfile/get")
	public Map<String, Object> getCelFiles(@RequestParam Map<String, String> params) {
		return anlsService.findCelFiles(params);
	}

	@PostMapping("/rdy/start")
	public Map<String, String> startAnls(@RequestBody List<String> mappingNos) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return anlsService.startAnls(mappingNos, userDetails.getUsername());
	}

	@GetMapping("/stts/get")
	public Map<String, Object> getStts(@RequestParam Map<String, String> params) {
		return anlsService.findSampleByAnlsSttsStatus(params);
	}

	@PostMapping("/reexpreg")
	public Map<String, String> reExpReg(@RequestBody List<Integer> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return anlsService.reExpReg(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/rslt/get")
	public Map<String, Object> getRslt(@RequestParam Map<String, String> params) {
		return anlsService.findSampleByAnlsSuccStatus(params);
	}

	@GetMapping("/databy/sample/{id}")
	public Map<String, Object> getDataBySample(@PathVariable String id) {
		return anlsService.findSampleDataBySampleId(id);
	}

	@PostMapping("/rslt/excel/import")
	public Map<String, Object> importRsltExcel(@RequestParam("file") MultipartFile multipartFile, MultipartHttpServletRequest request)
			throws InvalidFormatException, IOException {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String memberId = userDetails.getUsername();
		
		return anlsExcelService.importRsltExcel(multipartFile, memberId);
	}

	@PostMapping("/rslt/complete")
	public Map<String, String> completeAnls(@RequestBody List<Integer> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return anlsService.completeAnls(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/db/get")
	public Map<String, Object> getSampleDb(@RequestParam Map<String, String> params) {
		return sampleDbService.find(params, 440);
	}
}
