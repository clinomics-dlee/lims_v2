// package com.clinomics.controller;

// import java.io.File;
// import java.io.FileInputStream;
// import java.io.IOException;
// import java.io.OutputStream;
// import java.net.URLEncoder;
// import java.util.List;
// import java.util.Map;

// import javax.servlet.ServletOutputStream;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;

// import org.apache.commons.io.FilenameUtils;
// import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.core.io.FileSystemResource;
// import org.springframework.core.io.Resource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.util.FileCopyUtils;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.ResponseBody;
// import org.springframework.web.multipart.MultipartFile;
// import org.springframework.web.multipart.MultipartHttpServletRequest;

// import com.clinomics.enums.StatusCode;
// import com.clinomics.service.BundleService;
// import com.clinomics.service.ExprService;
// import com.clinomics.service.ResultService;
// import com.clinomics.service.SampleService;
// import com.google.common.collect.Maps;

// @RequestMapping("/work/expr")
// @Controller
// public class ExprController {

// 	@Autowired
// 	ExprService exprService;
	
// 	@Autowired
// 	ResultService resultService;
	
// 	@Autowired
// 	BundleService bundleService;
	
// 	@Autowired
// 	SampleService sampleService;
	
// 	@GetMapping()
// 	public String list(Model model) {
// 		model.addAttribute("bundles", bundleService.selectAll());
// 		Map<String, String> statusCodeMap = Maps.newHashMap();
// 		for (StatusCode statusCode : StatusCode.values()) {
// 			statusCodeMap.put(statusCode.getKey(), statusCode.getValue());
// 		}
// 		model.addAttribute("statusCodes", statusCodeMap);
		
// 		return "work/expr";
// 	}
	
// //	@GetMapping("/get")
// //	@ResponseBody
// //	public Map<String, Object> get(@RequestParam Map<String, Object> params) {
// //		return resultUploadService.selectAll(params);
// //	}
// //	
// //	@GetMapping("/resultby/upload")
// //	@ResponseBody
// //	public Map<String, Object> getByBundle(@RequestParam Map<String, Object> params) {
// //		return resultService.selectByResultUploadId(params);
// //	}
	
// 	@GetMapping("/get")
// 	@ResponseBody
// 	public Map<String, Object> get(@RequestParam Map<String, Object> params) {
// 		return sampleService.findSampleByBundleAndDateResultNullStatusFail(params);
// 	}
	
// 	@PostMapping("/upload")
// 	@ResponseBody
// 	public Map<String, Object> uploadCelFile(@RequestParam("file") List<MultipartFile> multipartFiles, MultipartHttpServletRequest request)
// 			throws InvalidFormatException, IOException {
// 		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
// 		return exprService.uploadCelFile(multipartFiles, userDetails.getUsername());
// 	}
	
// 	@PostMapping("/analysis")
// 	@ResponseBody
// 	public Map<String, Object> analysis(@RequestBody List<Map<String, String>> datas)
// 			throws InvalidFormatException, IOException {
// 		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
// 		return exprService.analysis(datas, userDetails.getUsername());
// 	}
	
// 	@PostMapping("/excel/import")
// 	@ResponseBody
// 	public Map<String, Object> importExcelSample(@RequestParam("file") MultipartFile multipartFile, MultipartHttpServletRequest request)
// 			throws InvalidFormatException, IOException {
// 		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
// 		return exprService.importExcelSample(multipartFile, userDetails.getUsername());
// 	}
	
// 	@GetMapping("/excel/form")
// 	@ResponseBody
// 	public void exportExcelForm(@RequestParam Map<String, Object> params, HttpServletResponse response) {
// 		XSSFWorkbook xlsx = exprService.exportExcelForm(params);
// 		requestExcel(xlsx, response);
// 	}
	
// 	@GetMapping("/files")
// 	@ResponseBody
// 	public Map<String, Object> getCelFiles(@RequestParam Map<String, Object> params) {
// 		return exprService.getCelFileList(params);
// 	}
	
// 	@PostMapping("/delete/files")
// 	@ResponseBody
// 	public Map<String, Object> deleteFiles(@RequestParam("celFileNames[]") List<String> celFileNames) {
// 		return exprService.deleteCelFiles(celFileNames);
// 	}
	
// 	@GetMapping("/resultsby/analyzing")
// 	@ResponseBody
// 	public Map<String, Object> getAnalyzingResults(@RequestParam Map<String, Object> params) {
// 		return exprService.getAnalyzingResults(params);
// 	}
	
// 	@GetMapping("/resultsby/failed")
// 	@ResponseBody
// 	public Map<String, Object> getFailedResults(@RequestParam Map<String, Object> params) {
// 		return exprService.getFailedResults(params);
// 	}
	
// //	@GetMapping("/csv/file")
// //	@ResponseBody
// //	public void exportCsvFile(@RequestParam Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) {
// //		// #. csv file 생성
// //		File file = exprExcelService.exportCsvFile(params);
// //		Map<String, Object> param = Maps.newHashMap();
// //		param.put("downloadFile", file);
// //		param.put("fileName", "Result.csv");
// //		param.put("isTempFile", true);
// //		
// //		requestFile(request, response, param);
// //	}
	
// 	@GetMapping("/get/mapping")
// 	@ResponseBody
// 	public Map<String, Object> getMappingList(@RequestParam Map<String, Object> params) {
// 		return sampleService.findSampleByBundleAndDate(params);
// 	}
	
// 	private void requestExcel(XSSFWorkbook xlsx, HttpServletResponse response) {
// 		response.setHeader("Content-Disposition", "attachment;filename=ResultForm.xlsx");
//         // 엑셀파일명 한글깨짐 조치
//         response.setHeader("Content-Transfer-Encoding", "binary;");
//         response.setHeader("Pragma", "no-cache;");
//         response.setHeader("Expires", "-1;");
//         response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

//         try {
// 	        ServletOutputStream out = response.getOutputStream();
	        
//         	xlsx.write(out);
//         	xlsx.close();
// 			out.flush();
// 		} catch (IOException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
// 	}
	
// 	private void requestFile(HttpServletRequest request, HttpServletResponse response, Map<String, Object> param) {
// 		try {
// 			File file = (File) param.get("downloadFile");
// 			String fileName = (String) param.get("fileName");
// 			Object obj = param.get("isTempFile");
// 			Resource resource = new FileSystemResource(file);
			
// 			boolean isTempFile = false;
// 			if (obj != null) {
// 				String value = obj.toString().trim();
// 				if (value.equalsIgnoreCase(Boolean.TRUE.toString()) || value.equalsIgnoreCase(Boolean.FALSE.toString())) {
// 					isTempFile = new Boolean(Boolean.parseBoolean(value));
// 				}
// 			}
	
// 			if (fileName != null) {
// 				fileName = fileName + "." + FilenameUtils.getExtension(fileName);
// 			} else {
// 				fileName = file.getName();
// 			}
	
// 			String userAgent = request.getHeader("User-Agent");
// 			boolean ie = userAgent.indexOf("MSIE") > -1 || userAgent.indexOf("Trident") > -1 || userAgent.indexOf("Edge") > -1;
// 			if (ie) {
// 				fileName = URLEncoder.encode(fileName, "utf-8");
// 			} else {
// 				fileName = new String(fileName.getBytes("utf-8"), "ISO-8859-1");
// 			}
			
// 			String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
// 			if (contentType == null) {
// 				contentType = "application/octet-stream";
// 			}
			
// 			response.setContentType(contentType);
// 			response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));
// 			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\";");
// 			response.setHeader("Content-Transfer-Encoding", "binary");
// 			// #. download 후 화면 처리를 위한 셋팅
// //			response.setHeader("Set-Cookie", "fileDownload=true; path=/");
	
// 			OutputStream out = response.getOutputStream();
// 			FileInputStream fis = null;
// 			try {
// 				fis = new FileInputStream(file);
// 				FileCopyUtils.copy(fis, out);
// 			} catch (Exception e) {
// 				e.printStackTrace();
// 			} finally {
// 				if (fis != null) {
// 					try {
// 						fis.close();
// 					} catch (Exception e) {
// 					}
// 				}
// 				// #. tempFile 인 경우 삭제
// 				if (isTempFile) {
// 					if (file.exists()) {
// 						file.delete();
// 					}
// 				}
// 			}
// 			out.flush();
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
// 	}
// }
