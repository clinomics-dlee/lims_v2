package com.clinomics.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.clinomics.service.InputService;
import com.clinomics.service.OutputService;
import com.clinomics.util.EmailSender;

@RequestMapping("/rest")
@RestController
public class ApiController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	InputService inputService;

	@Autowired
	OutputService outputService;
	
	@Autowired
	EmailSender emailSender;
	
	@RequestMapping(value = "/result/get")
	public Map<String, Object> getResultWithWaitStatus(@RequestParam Map<String, String> params) {
		HttpServletRequest req = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
		String ip = req.getHeader("X-FORWARDED-FOR");
		if (ip == null)	ip = req.getRemoteAddr();
		
		logger.info("☆☆☆☆☆ getResultWithWaitStatus ☆☆☆ request (/result/get) IP : [" + ip + "]");
		return outputService.getResultsForRest(params, ip);
	}

	@RequestMapping(value = "/status/update")
	public Map<String, Object> updateStatus(@RequestParam Map<String, String> params) {
		HttpServletRequest req = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
		String ip = req.getHeader("X-FORWARDED-FOR");
		if (ip == null)	ip = req.getRemoteAddr();
		
		logger.info("☆☆☆☆☆ getResultWithWaitStatus ☆☆☆ request (/status/update) IP : [" + ip + "]");
		return outputService.updateStatus(params, ip);
	}

	@RequestMapping(value = "/resultby/laboratory/get")
	public Map<String, Object> getResultByLaboratory(@RequestParam Map<String, String> params) {
		logger.info("☆☆☆☆☆ getResultByLaboratory ☆☆☆ IN interface : /resultby/laboratory/get ");
		HttpServletRequest req = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
		String ip = req.getHeader("X-FORWARDED-FOR");
		if (ip == null)	ip = req.getRemoteAddr();
		
		logger.info("☆☆☆☆☆ getResultByLaboratory ☆☆☆ request IP : [" + ip + "]");
		return outputService.getResultByLaboratoryForRest(params, ip);
	}

	@RequestMapping(value = "/resultby/params/get")
	public Map<String, Object> getResultByParams(@RequestParam Map<String, String> params) {
		logger.info("☆☆☆☆☆ getResultByParams ☆☆☆ IN interface : /resultby/params/get ");
		HttpServletRequest req = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
		String ip = req.getHeader("X-FORWARDED-FOR");
		if (ip == null)	ip = req.getRemoteAddr();
		
		logger.info("☆☆☆☆☆ getResultByParams ☆☆☆ request IP : [" + ip + "]");
		return outputService.getResultByParamsForRest(params, ip);
	}

	@RequestMapping(value = "/resultby/laboratoryids/get")
	public Map<String, Object> getResultByLaboratoryIds(@RequestBody Map<String, Object> params) {
		logger.info("☆☆☆☆☆ getResultByParams ☆☆☆ IN interface : /resultby/params/get ");
		HttpServletRequest req = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
		String ip = req.getHeader("X-FORWARDED-FOR");
		if (ip == null)	ip = req.getRemoteAddr();
		
		logger.info("☆☆☆☆☆ getResultByParams ☆☆☆ request IP : [" + ip + "]");
		return outputService.getResultByLaboratoryIdsForRest(params, ip);
	}

	@PostMapping(value = "/doc/save")
	public Map<String, Object> saveDocument(@RequestBody Map<String, Object> documentMap, HttpServletRequest req) {
		logger.info("☆☆☆☆☆ saveDocument ☆☆☆ IN interface : /doc/save ");
		String ip = req.getHeader("X-FORWARDED-FOR");
		if (ip == null)	ip = req.getRemoteAddr();
		
		logger.info("☆☆☆☆☆ saveDocument ☆☆☆ request IP : [" + ip + "]");
		return inputService.saveDocumentForRest(documentMap);
	}

	@RequestMapping(value = "/hospitalinfo/get")
	public Map<String, Object> getHospitalInfos() {
		logger.info("☆☆☆☆☆ getHospitalInfos ☆☆☆ IN interface : /hospitalinfo/get ");
		HttpServletRequest req = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
		String ip = req.getHeader("X-FORWARDED-FOR");
		if (ip == null)	ip = req.getRemoteAddr();
		
		logger.info("☆☆☆☆☆ getHospitalInfos ☆☆☆ request IP : [" + ip + "]");
		return outputService.getHospitalInfos(ip);
	}
	
//	@RequestMapping(value = "/mail/test")
//	public String sendMailTest() {
//		Sample sample = new Sample();
//		sample.setChipBarcode("test1");
//		sample.setLaboratoryId("TEST-0000-0001");
//		sample.setStatusMessage("Test is good.");
//		sample.setVersion(1);
//		List<Sample> samples = new ArrayList<Sample>();
//		samples.add(sample);
//		emailSender.sendMailToFail(samples);
//		return "asdf";
//	}
}
