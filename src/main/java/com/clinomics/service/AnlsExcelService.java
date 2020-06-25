package com.clinomics.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Product;
import com.clinomics.entity.lims.Role;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.GenotypingMethodCode;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.RoleCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
public class AnlsExcelService {
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

		if (!roles.contains(RoleCode.ROLE_EXP_20.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_40.toString())
			&& !roles.contains(RoleCode.ROLE_EXP_80.toString())) {
				
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

				// #. 조회된 검체의 상태가 분석성공이 아닌경우
				if (!s.getStatusCode().equals(StatusCode.S420_ANLS_SUCC)) {
					logger.info(">> Invalid status sample id=[" + genotypingId + "]");
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "상태값이 다른 검체가 존재합니다.[" + genotypingId + "]");
					return rtn;
				}

				// #. 조회된 검체의 Genotyping Methd가 QRT PCR이 아닌경우
				if (!s.getGenotypingMethodCode().equals(GenotypingMethodCode.QRT_PCR)) {
					logger.info(">> Invalid Genotyping Method sample id=[" + genotypingId + "]");
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "Genotyping Method 값이 Chip이 아닌 검체가 존재합니다.[" + genotypingId + "]");
					return rtn;
				}

				Set<String> productTypes = Sets.newHashSet();
				s.getBundle().getProduct().stream().forEach(p -> {
					productTypes.add(p.getType());
				});
							
				// #. 상품목록이 가지고 있는 모든 마커 정보 조회
				Map<String, List<Map<String, String>>> productTypeMarkerInfos = sampleDbService.getMarkerInfo(new ArrayList<String>(productTypes));
				// #. marker 정보가 없는 경우
				if (productTypeMarkerInfos.isEmpty()) {
					// #. 조회한 마커 목록이 비어있는경우
					logger.info(">> not found marker infomation error sample id=[" + genotypingId + "]");
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "마커 정보를 조회할 수 없습니다.[" + genotypingId + "]");
					return rtn;
				}

				// #. 해당 product에 마커정보 목록
				List<Map<String, String>> allMarkerInfos = new ArrayList<Map<String, String>>();
				for (String key : productTypeMarkerInfos.keySet()) {
					List<Map<String, String>> mks = productTypeMarkerInfos.get(key);
					for (Map<String, String> mi : mks) {
						if (!allMarkerInfos.contains(mi)) {
							allMarkerInfos.add(mi);
						}
					}
				}

				List<String> notExistMarkers = new ArrayList<String>();

				// #. 현재 상품에 마커 목록
				List<String> checkMarkers = new ArrayList<String>();
				for (Map<String, String> mi : allMarkerInfos) {
					checkMarkers.add(mi.get("name"));
				}

				// #. 분석완료 파일에 마커 목록
				List<String> markers = sht.keySet().stream().collect(Collectors.toList());

				// #. 마커 목록이 전부 있는 지 체크
				for (String marker : checkMarkers) {
					if (!markers.contains(marker)) {
						notExistMarkers.add(marker);
					}
				}

				if (notExistMarkers.size() > 0) {
					// #. 마커가 존재하지 않는것이 있는 경우
					logger.info(">> Not Exist Markers error sample id=[" + genotypingId + "]" + notExistMarkers.toString());
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "해당 마커 정보가 없습니다.[" + genotypingId + "]" + notExistMarkers.toString());
					return rtn;
				}

				// #. 마커는 있으나 값이 허용되지않은 값이 셋팅된것 체크
				List<String> invalidMarkers = new ArrayList<String>();
				for (Map<String, String> mi : allMarkerInfos) {
					String name = mi.get("name");
					String ref = mi.get("refValue");
					String alt = mi.get("altValue");
					// #. 마커는 있으나 값이 다른것
					String value = (String) sht.get(name);
					
					// #. 4가지 조합이 아닌경우
					if (!value.equals(ref + alt) && !value.equals(alt + alt)
							&& !value.equals(ref + ref) && !value.equals(alt + ref )) {
						invalidMarkers.add(name);
					}
				}
				
				if (invalidMarkers.size() > 0) {
					// #. marker 값 유효하지 않은 경우
					logger.info(">> Invalid Markers error sample id=[" + genotypingId + "]" + invalidMarkers.toString());
					rtn.put("result", ResultCode.FAIL_EXISTS_VALUE);
					rtn.put("message", "해당 마커에 값이 유효하지 않습니다.[" + genotypingId + "]" + invalidMarkers.toString());
					return rtn;
				}
				
				Map<String, Object> data = Maps.newHashMap();
				data.putAll(sht);

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
