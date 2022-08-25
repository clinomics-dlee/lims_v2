package com.clinomics.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExpExcelService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ExcelReadComponent excelReadComponent;

	public XSSFWorkbook exportStep1ExcelForm(Map<String, String> params) {
		XSSFWorkbook workbook = new XSSFWorkbook();
		CellStyle pink = workbook.createCellStyle();
		pink.setFillForegroundColor(HSSFColorPredefined.ROSE.getIndex());
		pink.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		XSSFSheet sheet = workbook.createSheet("sample");

		// #. excel header
		XSSFRow row1 = sheet.createRow(0);
		XSSFCell cell1_1 = row1.createCell(0);
		cell1_1.setCellValue("실험 관리번호");
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

		Optional<Member> oMember = memberRepository.findById(memberId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}
		
		XSSFWorkbook workbook = null;
		try {
			workbook = excelReadComponent.readWorkbook(multipartFile);
		} catch (InvalidFormatException e) {
			rtn.put("result", ResultCode.EXCEL_FILE_TYPE.get());
			rtn.put("message", ResultCode.EXCEL_FILE_TYPE.getMsg());
		} catch (IOException e) {
			rtn.put("result", ResultCode.FAIL_FILE_READ.get());
			rtn.put("message", ResultCode.FAIL_FILE_READ.getMsg());
		}
		
		if (workbook == null) {
			rtn.put("result", ResultCode.EXCEL_FILE_TYPE.get());
			rtn.put("message", ResultCode.EXCEL_FILE_TYPE.getMsg());
			return rtn;
		}
		
		XSSFSheet sheet = workbook.getSheetAt(0);
		List<Map<String, Object>> sheetList = excelReadComponent.readMapFromSheet(sheet);
		
		if (sheetList.size() < 1) {
			rtn.put("result", ResultCode.EXCEL_EMPTY.get());
			rtn.put("message", ResultCode.EXCEL_EMPTY.getMsg());
			return rtn;
		}
		
		int sheetNum = workbook.getNumberOfSheets();
		if (sheetNum < 1) {
			return rtn;
		}

		// #. 첫번째 열의 값은 laboratoryId값으로 해당 열은 고정
		String laboratoryIdCellName = sheet.getRow(0).getCell(0).getStringCellValue();
		
		List<Sample> savedSamples = new ArrayList<Sample>();
		for (Map<String, Object> sht : sheetList) {
			String laboratoryId = (String)sht.get(laboratoryIdCellName);
			// #. 검사실ID값으로 조회한 모든 검체 업데이트
			Specification<Sample> where = Specification.where(SampleSpecification.laboratoryIdEqual(laboratoryId));
			List<Sample> samples = sampleRepository.findAll(where);

			if (samples.size() < 1) {
				logger.info(">> not found laboratory Id=" + laboratoryId);
				rtn.put("result", ResultCode.FAIL_NOT_EXISTS.get());
				rtn.put("message", "실험 관리번호를 찾을 수 없습니다.[" + laboratoryId + "]");
				return rtn;
			}
			
			for (Sample sample : samples) {
				String a260280 = (String)sht.get("A 260/280");
				String cncnt = (String)sht.get("농도 (ng/μL)");
				String dnaQc = (StringUtils.isEmpty((String)sht.get("DNA QC")) ? "PASS" : (String)sht.get("DNA QC"));
				if (!NumberUtils.isCreatable(a260280) || !NumberUtils.isCreatable(cncnt)) {
					logger.info(">> A 260/280 and concentration can only be entered in numbers=" + laboratoryId);
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE.get());
					rtn.put("message", "A 260/280, 농도는 숫자만 입력가능합니다.[" + laboratoryId + "]");
					return rtn;
				}

				sample.setA260280(a260280);
				sample.setCncnt(cncnt);
				sample.setDnaQc(dnaQc);
				
				savedSamples.add(sample);
			}
		}
		
		sampleRepository.saveAll(savedSamples);
		
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}

	public XSSFWorkbook exportStep2ExcelForm(Map<String, String> params) {
		XSSFWorkbook workbook = new XSSFWorkbook();
		CellStyle orange = workbook.createCellStyle();
		orange.setFillForegroundColor(HSSFColorPredefined.LIGHT_ORANGE.getIndex());
		orange.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		orange.setAlignment(HorizontalAlignment.CENTER);
		orange.setBorderBottom(BorderStyle.THIN);
		orange.setBorderLeft(BorderStyle.THIN);
		orange.setBorderRight(BorderStyle.THIN);
		orange.setBorderTop(BorderStyle.THIN);

		CellStyle border = workbook.createCellStyle();
		border.setAlignment(HorizontalAlignment.CENTER);
		border.setBorderBottom(BorderStyle.THIN);
		border.setBorderLeft(BorderStyle.THIN);
		border.setBorderRight(BorderStyle.THIN);
		border.setBorderTop(BorderStyle.THIN);
		
		XSSFSheet sheet = workbook.createSheet("sample");

		// #. excel header
		XSSFRow row1 = sheet.createRow(0);
		XSSFCell cell1_1 = row1.createCell(0);
		cell1_1.setCellValue("Well Position");
		cell1_1.setCellStyle(orange);
		XSSFCell cell1_2 = row1.createCell(1);
		cell1_2.setCellValue("Genotyping ID");
		cell1_2.setCellStyle(orange);

		String excelType = params.get("type");

		List<String> prefixChars = new ArrayList<String>();
		int startNumber = 1;
		int endNumber = 1;
		int addNumber = 1;

		if ("96".equals(excelType)) {
			// #. APMRA 96 양식
			prefixChars = Arrays.asList(new String[] {
				"A", "B", "C", "D", "E", "F", "G", "H"
			});

			startNumber = 1;
			endNumber = 13;
			addNumber = 1;
		} else if ("24".equals(excelType)) {
			// #. APMRA 24 양식
			prefixChars = Arrays.asList(new String[] {
				"A", "B", "C", "D", "E", "F", "G", "H"
			});

			startNumber = 5;
			endNumber = 10;
			addNumber = 2;
		} else {
			// #. Custom 96 양식
			// #. well position 기본값 셋팅
			prefixChars = Arrays.asList(new String[] {
				"A", "C", "E", "G", "I", "K", "M", "O"
			});

			startNumber = 1;
			endNumber = 24;
			addNumber = 2;
		}

		int rowCount = 1;
		for (int i = startNumber; i < endNumber; i += addNumber) {
			for (String prefixChar : prefixChars) {
				String wp = prefixChar + String.format("%02d", i);
				XSSFRow row = sheet.createRow(rowCount);
				XSSFCell cell = row.createCell(0);
				XSSFCell cell2 = row.createCell(1);
				cell.setCellValue(wp);
				cell.setCellStyle(border);
				cell2.setCellStyle(border);

				rowCount++;
			}
		}

		sheet.setColumnWidth(0, 4000);
		sheet.setColumnWidth(1, 4000);
		
		return workbook;
	}

}
