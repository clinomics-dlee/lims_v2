package com.clinomics.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.clinomics.service.ExpService;
import com.clinomics.service.SampleDbService;
import com.google.common.collect.Maps;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

@RequestMapping("/exp")
@RestController
public class ExpController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
    @Autowired
    ExpService expService;

	@Autowired
    SampleDbService sampleDbService;

	@GetMapping("/rdy/get")
	public Map<String, Object> getRdy(@RequestParam Map<String, String> params) {
		return expService.findSampleByExpRdyStatus(params);
	}

	@PostMapping("/rdy/start")
	public Map<String, String> startExp(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.startExp(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/step1/get")
	public Map<String, Object> getStep1(@RequestParam Map<String, String> params) {
		return expService.findSampleByExpStep1Status(params);
	}

	@GetMapping("/step1/excel/form")
	public void exportExcelForm(@RequestParam Map<String, String> params, HttpServletResponse response) {
		XSSFWorkbook xlsx = expService.exportStep1ExcelForm(params);
		requestExcel(xlsx, "Dna_Qc_Template", response);
	}

	@PostMapping("/step1/excel/import")
	public Map<String, Object> importExcelSample(@RequestParam("file") MultipartFile multipartFile, MultipartHttpServletRequest request)
			throws InvalidFormatException, IOException {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String memberId = userDetails.getUsername();

		return expService.importStep1Excel(multipartFile, memberId);
	}

	@PostMapping("/step1/complete")
	public Map<String, String> completeStep1(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.completeStep1(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/step2/get")
	public Map<String, Object> getStep2(@RequestParam Map<String, String> params) {
		return expService.findSampleByExpStep2Status(params);
	}

	@PostMapping("/step2/qrtPcr/update")
	public Map<String, String> updateQrtPcr(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.updateQrtPcr(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/step2/excel/form")
	public void exportStep2ExcelForm(@RequestParam Map<String, String> params, HttpServletResponse response) {
		XSSFWorkbook xlsx = expService.exportStep2ExcelForm(params);
		requestExcel(xlsx, "Mapping_Template", response);
	}

	@PostMapping("/step2/mapping/saveall")
	public Map<String, Object> saveAllMapping(@RequestBody List<Map<String, String>> params)
			throws InvalidFormatException, IOException {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.saveAllMapping(params, userDetails.getUsername());
	}

	@PostMapping("/step2/complete")
	public Map<String, String> completeStep2(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.completeStep2(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/step3/get")
	public Map<String, Object> getStep3(@RequestParam Map<String, String> params) {
		return expService.findMappingInfosByExpStep3Status(params);
	}

	@PostMapping("/step3/chipInfo/update")
	public Map<String, String> updateChipInfo(@RequestParam Map<String, String> params) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.updateChipInfo(params, userDetails.getUsername());
	}

	@PostMapping("/step3/chipInfos/update")
	public Map<String, String> updateChipInfos(@RequestBody List<Map<String, String>> params) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.updateChipInfos(params, userDetails.getUsername());
	}

	@PostMapping("/step3/complete")
	public Map<String, String> completeStep3(@RequestParam("mappingNos[]") List<String> mappingNos) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return expService.completeStep3(mappingNos, userDetails.getUsername());
	}

	@GetMapping("/db/sample/get")
	public Map<String, Object> getSampleDb(@RequestParam Map<String, String> params) {
		return sampleDbService.find(params, 400);
	}

	@GetMapping("/db/mapping/get")
	public Map<String, Object> getMappingDb(@RequestParam Map<String, String> params) {
		return expService.findMappingInfosForDb(params);
	}

	@GetMapping("/mapping/sample/get/{mappingNo}")
	public Map<String, Object> getMappingSample(@RequestParam Map<String, String> params, @PathVariable String mappingNo) {
		return expService.findMappingSample(params, mappingNo);
	}

	// ############################ private
	private void requestExcel(XSSFWorkbook xlsx, String fileName, HttpServletResponse response) {
		if (fileName == null || fileName.trim().length() < 1) {
			fileName = "sample";
		}
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
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
			e.printStackTrace();
		}
	}
}
