package com.clinomics.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.clinomics.service.DleeService;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RequestMapping("/exp")
@RestController
public class ExpController {
    @Autowired
    DleeService dleeService;


	@GetMapping("/rdy/get")
	public Map<String, Object> getRdy(@RequestParam Map<String, String> params) {
		return dleeService.findSampleByExpRdyStatus(params);
	}

	@PostMapping("/rdy/start")
	public Map<String, String> startExp(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return dleeService.startExp(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/step1/get")
	public Map<String, Object> getStep1(@RequestParam Map<String, String> params) {
		return dleeService.findSampleByExpStep1Status(params);
	}

	@GetMapping("/step1/excel/form")
	public void exportExcelForm(@RequestParam Map<String, String> params, HttpServletResponse response) {
		XSSFWorkbook xlsx = dleeService.exportStep1ExcelForm(params);
		requestExcel(xlsx, response);
	}

	private void requestExcel(XSSFWorkbook xlsx, HttpServletResponse response) {
		response.setHeader("Content-Disposition", "attachment;filename=DnaQc_Template.xlsx");
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

	@PostMapping("/step1/excel/import")
	public Map<String, Object> importExcelSample(@RequestParam("file") MultipartFile multipartFile, MultipartHttpServletRequest request)
			throws InvalidFormatException, IOException {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String memberId = userDetails.getUsername();

		return dleeService.importStep1Excel(multipartFile, memberId);
	}

	@PostMapping("/step1/complete")
	public Map<String, String> completeStep1(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return dleeService.completeStep1(sampleIds, userDetails.getUsername());
	}

	@GetMapping("/step2/get")
	public Map<String, Object> getStep2(@RequestParam Map<String, String> params) {
		return dleeService.findSampleByExpStep2Status(params);
	}

	@PostMapping("/step2/qrtPcr/update")
	public Map<String, String> updateQrtPcr(@RequestParam("sampleIds[]") List<String> sampleIds) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return dleeService.updateQrtPcr(sampleIds, userDetails.getUsername());
	}
}
