package com.clinomics.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TestExcelService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${lims.workspacePath}")
	private String workspacePath;

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ExcelReadComponent excelReadComponent;

	@Autowired
	SampleDbService sampleDbService;

	public Map<String, Object> importRsltExcel(MultipartFile multipartFile, String memberId) {
		Map<String, Object> rtn = Maps.newHashMap();
		XSSFWorkbook workbook = null;

		Optional<Member> oMember = memberRepository.findById(memberId);
		Member member = oMember.orElseThrow(NullPointerException::new);
		String roles = "";
		for (Role r : member.getRole()) {
			roles += "," + r.getCode();
		}
		roles = roles.substring(1);

		if (!roles.contains(RoleCode.ROLE_TEST_90.toString())
			&& !roles.contains(RoleCode.ROLE_IT_99.toString())) {
				
			rtn.put("result", ResultCode.NO_PERMISSION.get());
			rtn.put("message", ResultCode.NO_PERMISSION.getMsg());
			return rtn;
		}
		
		try {
			workbook = excelReadComponent.readWorkbook(multipartFile);
			
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
				rtn.put("result", ResultCode.EXCEL_EMPTY.get());
				rtn.put("message", ResultCode.EXCEL_EMPTY.getMsg());
				return rtn;
			}

			// #. 첫번째 열의 값은 genotypingId값으로 해당 열은 고정
			String genotypingIdCellName = sheet.getRow(0).getCell(0).getStringCellValue();
			
			List<Sample> savedSamples = new ArrayList<Sample>();
			for (Map<String, Object> sht : sheetList) {
				String genotypingId = (String)sht.get(genotypingIdCellName);
				
				String[] genotypingInfo = genotypingId.split("-V");
				// #. genotypingId양식이 틀린경우
				if (genotypingInfo.length != 2) {
					logger.info(">> Invalid Genotyping Id=[" + genotypingId + "]");
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "Genotyping ID 값을 확인해주세요.[" + genotypingId + "]");
					return rtn;
				}

				String laboratoryId = genotypingInfo[0];
				// #. version값이 숫자가아닌경우
				if (!NumberUtils.isCreatable(genotypingInfo[1])) {
					logger.info(">> Invalid Genotyping Version=[" + genotypingId + "]");
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "Genotyping ID Version 값을 확인해주세요.[" + genotypingId + "]");
					return rtn;
				}
				int version = NumberUtils.toInt(genotypingInfo[1]);

				Specification<Sample> where = Specification
						.where(SampleSpecification.laboratoryIdEqual(laboratoryId))
						.and(SampleSpecification.isTest())
						.and(SampleSpecification.versionEqual(version));
				List<Sample> samples = sampleRepository.findAll(where);
				Sample s = samples.get(0);
				// #. 검사실ID 또는 version이 잘못 입력된 경우
				if (s == null) {
					logger.info(">> not found sample id=[" + genotypingId + "]");
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "조회된 Genotyping ID 값이 없습니다.[" + genotypingId + "]");
					return rtn;
				}

				Map<String, Object> data = Maps.newHashMap();
				data.putAll(sht);

				// #. 첫번째열 genotyping id값은 제거
				data.remove(genotypingIdCellName);

				s.setData(data);
				s.setAnlsEndDate(LocalDateTime.now());

				savedSamples.add(s);
			}
		
			sampleRepository.saveAll(savedSamples);
			rtn.put("result", ResultCode.SUCCESS.get());
		} catch (InvalidFormatException e) {
			rtn.put("result", ResultCode.EXCEL_FILE_TYPE.get());
			rtn.put("message", ResultCode.EXCEL_FILE_TYPE.getMsg());
		} catch (IOException e) {
			rtn.put("result", ResultCode.FAIL_FILE_READ.get());
			rtn.put("message", ResultCode.FAIL_FILE_READ.getMsg());
		} catch (Exception e) {
			rtn.put("result", ResultCode.FAIL_UPLOAD);
		}

		return rtn;
	}
}
