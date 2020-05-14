package com.clinomics.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Product;
import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleItem;
import com.clinomics.enums.GenotypingMethodCode;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.DleeRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.specification.lims.DleeSpecification;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DleeService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	DleeRepository dleeRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	SampleItemService sampleItemService;

	@Autowired
	DataTableService dataTableService;

	@Autowired
	ExcelReadComponent excelReadComponent;

	public Map<String, Object> findSampleByExpRdyStatus(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		
		List<Order> orders = Arrays.asList(new Order[] { Order.desc("createdDate"), Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(DleeSpecification.betweenDate(params))
					.and(DleeSpecification.bundleId(params))
					.and(DleeSpecification.keywordLike(params))
					.and(DleeSpecification.existsStatusIn(Arrays.asList(StatusCode.EXP_READY)));
					
		
		total = dleeRepository.count(where);
		Page<Sample> page = dleeRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> startExp(List<String> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime nnow = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (String id : sampleIds) {
			Optional<Sample> oSample = dleeRepository.findById(NumberUtils.toInt(id));
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setStatusCode(StatusCode.EXP_STEP1);
			sample.setExpStartDate(nnow);
			sample.setExpStartMember(member);

			dleeRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findSampleByExpStep1Status(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		
		List<Order> orders = Arrays.asList(new Order[] { Order.desc("createdDate"), Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(DleeSpecification.betweenDate(params))
					.and(DleeSpecification.bundleId(params))
					.and(DleeSpecification.keywordLike(params))
					.and(DleeSpecification.existsStatusIn(Arrays.asList(StatusCode.EXP_STEP1)));
		
		total = dleeRepository.count(where);
		Page<Sample> page = dleeRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	public XSSFWorkbook exportStep1ExcelForm(Map<String, String> params) {
		XSSFWorkbook workbook = new XSSFWorkbook();
		CellStyle pink = workbook.createCellStyle();
		pink.setFillForegroundColor(HSSFColorPredefined.ROSE.getIndex());
		pink.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		XSSFSheet sheet = workbook.createSheet("sample");

		// #. excel header
		XSSFRow row1 = sheet.createRow(0);
		XSSFCell cell1_1 = row1.createCell(0);
		cell1_1.setCellValue("검사실 ID");
		cell1_1.setCellStyle(pink);
		XSSFCell cell1_2 = row1.createCell(1);
		cell1_2.setCellValue("A 260/280");
		cell1_2.setCellStyle(pink);
		XSSFCell cell1_3 = row1.createCell(2);
		cell1_3.setCellValue("농도 (ng/μL)");
		cell1_3.setCellStyle(pink);
		XSSFCell cell1_4 = row1.createCell(3);
		cell1_4.setCellValue("DNA QC");
		cell1_4.setCellStyle(pink);

		sheet.setColumnWidth(0, 3000);
		sheet.setColumnWidth(1, 3000);
		sheet.setColumnWidth(2, 3000);
		sheet.setColumnWidth(3, 3000);
		
		return workbook;
	}

	public Map<String, Object> importStep1Excel(MultipartFile multipartFile, String memberId) {
		
		Map<String, Object> rtn = Maps.newHashMap();
		
		XSSFWorkbook workbook = null;
		try {
			workbook = excelReadComponent.readWorkbook(multipartFile);
		} catch (InvalidFormatException e) {
			rtn.put("result", ResultCode.EXCEL_FILE_TYPE.get());
		} catch (IOException e) {
			rtn.put("result", ResultCode.FAIL_FILE_READ.get());
		}
		
		if (workbook == null) {
			rtn.put("result", ResultCode.EXCEL_FILE_TYPE.get());
			return rtn;
		}
		
		XSSFSheet sheet = workbook.getSheetAt(0);
		List<Map<String, Object>> sheetList = excelReadComponent.readMapFromSheet(sheet);
		
		if (sheetList.size() < 1) {
			rtn.put("result", ResultCode.EXCEL_EMPTY.get());
			return rtn;
		}

		Optional<Member> oMember = memberRepository.findById(memberId);
		Member member = oMember.orElse(new Member());
		
		int sheetNum = workbook.getNumberOfSheets();
		if (sheetNum < 1) {
			return rtn;
		}
		
		List<Sample> items = new ArrayList<Sample>();
		for (Map<String, Object> sht : sheetList) {
			// #. 검사실ID값으로 조회한 모든 검체 업데이트
			List<Sample> samples = dleeRepository.findByLaboratoryId((String)sht.get("검사실 ID"));

			if (samples.size() < 1) {
				rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get());
				return rtn;
			}
			
			for (Sample sample : samples) {
				String a260280 = (String)sht.get("A 260/280");
				String cncnt = (String)sht.get("농도 (ng/μL)");
				String dnaQc = (String)sht.get("DNA QC");
				
				if (!NumberUtils.isCreatable(a260280) || !NumberUtils.isCreatable(a260280)) {
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
					return rtn;
				}

				sample.setA260280(a260280);
				sample.setCncnt(cncnt);
				sample.setDnaQc(dnaQc);
				
				items.add(sample);
			}
		}
		
		dleeRepository.saveAll(items);
		
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	@Transactional
	public Map<String, String> completeStep1(List<String> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		LocalDateTime nnow = LocalDateTime.now();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (String id : sampleIds) {
			Optional<Sample> oSample = dleeRepository.findById(NumberUtils.toInt(id));
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setStatusCode(StatusCode.EXP_STEP2);
			sample.setExpStep1Date(nnow);
			sample.setExpStep1Member(member);

			dleeRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public Map<String, Object> findSampleByExpStep2Status(Map<String, String> params) {
		int draw = 1;
		// #. paging param
		int pageNumber = NumberUtils.toInt(params.get("pgNmb"), 1);
		int pageRowCount = NumberUtils.toInt(params.get("pgrwc"), 10);
		
		List<Order> orders = Arrays.asList(new Order[] { Order.desc("createdDate"), Order.asc("id") });
		// #. paging 관련 객체
		Pageable pageable = Pageable.unpaged();
		if (pageRowCount > 1) {
			pageable = PageRequest.of(pageNumber, pageRowCount, Sort.by(orders));
		}
		long total;
		
		Specification<Sample> where = Specification
					.where(DleeSpecification.betweenDate(params))
					.and(DleeSpecification.bundleId(params))
					.and(DleeSpecification.keywordLike(params))
					.and(DleeSpecification.existsStatusIn(Arrays.asList(StatusCode.EXP_STEP2)));
		
		total = dleeRepository.count(where);
		Page<Sample> page = dleeRepository.findAll(where, pageable);
		List<Sample> list = page.getContent();
		List<Map<String, Object>> header = sampleItemService.filterItemsAndOrdering(list, BooleanUtils.toBoolean(params.getOrDefault("all", "false")));
		long filtered = total;
		
		return dataTableService.getDataTableMap(draw, pageNumber, total, filtered, list, header);
	}

	@Transactional
	public Map<String, String> updateQrtPcr(List<String> sampleIds, String userId) {
		Map<String, String> rtn = Maps.newHashMap();
		Optional<Member> oMember = memberRepository.findById(userId);
		Member member = oMember.orElseThrow(NullPointerException::new);

		for (String id : sampleIds) {
			Optional<Sample> oSample = dleeRepository.findById(NumberUtils.toInt(id));
			Sample sample = oSample.orElseThrow(NullPointerException::new);

			sample.setGenotypingMethodCode(GenotypingMethodCode.QRT_PCR);

			dleeRepository.save(sample);
		}

		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}
}
