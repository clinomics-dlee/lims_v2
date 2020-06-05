package com.clinomics.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Sample;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class CalendarExcelService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ExcelReadComponent excelReadComponent;

	public XSSFWorkbook exportHumanExcelForm(Map<String, String> params) {
		// #. excel template 파일
		XSSFWorkbook wb = null;
		
		logger.info(">> start writeExcelFileForGsHumanOrigin");
		// #. excel 읽기
		wb = new XSSFWorkbook();

		// #. 분석완료일 기준으로 목록 조회
		Specification<Sample> where = Specification
				.where(SampleSpecification.anlsCmplDatebetween(params))
				.and(SampleSpecification.bundleIsActive())
				.and(SampleSpecification.statusCodeGt(600));
		List<Sample> samples = sampleRepository.findAll(where);


		// #. deprecated
		Member m = new Member();
		m.setName("Tester1");
		Member m2 = new Member();
		m.setName("Tester2");
		for (int i = 0; i < 200; i++) {
			Sample s = new Sample();
			s.setLaboratoryId("TEST-2004-" + String.format("%04d", i));
			s.setAnlsCmplDate(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0, 0));
			s.setAnlsCmplMember(m);
			s.setJdgmDrctApproveMember(m2);

			samples.add(s);
		}

		List<String> monthlyList = new ArrayList<String>();
		Map<String, List<Sample>> monthlySamplesMap = Maps.newHashMap();
		for (Sample sample : samples) {
			String month = sample.getAnlsCmplDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
			if (!monthlyList.contains(month)) {
				monthlyList.add(month);

				// #. 연월별 검체 목록을 map에 넣기
				List<Sample> sList = new ArrayList<Sample>();
				sList.add(sample);
				monthlySamplesMap.put(month, sList);
			} else {
				// #. 연월값이 리스트에 있는 경우
				// #. map에서 연월값으로 리스트를 조회하여 해당 리스트에 sample 추가
				List<Sample> sList = monthlySamplesMap.get(month);
				sList.add(sample);
			}
		}

		int pageStartSampleRowIndex = 7;
		int lastPageStartSampleRowIndex = 9;
		int pageSampleRowCount = 25;
		int pageTotalRowCount = 33;
		int lastPageTotalRowCount = 35;

		for (String month : monthlyList) {
			List<Sample> sList = monthlySamplesMap.get(month);
			// #. sheet 생성
			XSSFSheet sheet = wb.createSheet(month);
			int count = 0;
			int lastPageIndex = sList.size() / pageSampleRowCount;
			for (Sample s : sList) {
				// // #. 시작 rowIndex
				// int destStartRowIndex = (count / pageSampleRowCount) * pageTotalRowCount;
				// int destLastPageStartRowIndex = (i / pageSampleRowCount) * pageTotalRowCount;
				// // #. row 가져오기. rownum = 복사한 양식 시작 index + 페이지별 시작 row index + (i 를 25로 나눈나머지)
				// int rowNum = destStartRowIndex + pageStartSampleRowIndex + (i % pageSampleRowCount);
				// int lastPageRowNum = destLastPageStartRowIndex + lastPageStartSampleRowIndex + (i % pageSampleRowCount);
				
				// // #. 마지막 페이지 여부
				// boolean isLastPage = (lastPageIndex == (i / pageSampleRowCount));
				
				// // #. 25개 기준으로 1개의 양식을 셋팅하기 때문에 고려하여 양식 복사
				// if (i > 0 && i % pageSampleRowCount == 0) {
				// 	// #. 마지막장인 경우 상단에 결재 추가한 sheet 양식 복사
				// 	if (isLastPage) {
				// 		// #. 마지막 페이지 양식 복사
				// 		this.copyExcelRows(wb, wb.getSheetAt(1), destSheet, 0, (lastPageTotalRowCount - 1), destLastPageStartRowIndex);
				// 	} else {
				// 		// #. 기본 양식 복사
				// 		this.copyExcelRows(wb, wb.getSheetAt(0), destSheet, 0, (pageTotalRowCount - 1), destStartRowIndex);
				// 	}
				// }
				boolean isLastPage = (lastPageIndex == (count / pageSampleRowCount));
				int startRowIndex = (count / pageSampleRowCount) * pageTotalRowCount;
				int rowNum = startRowIndex + pageStartSampleRowIndex + (count % pageSampleRowCount);
				int lastPageRowNum = startRowIndex + lastPageStartSampleRowIndex + (count % pageSampleRowCount);

				// #. Sample을 25개씩 나눠서 표시 하기위해 
				if (count % pageSampleRowCount == 0) {
					if (isLastPage) {
						this.createLastTable(wb, sheet, startRowIndex);
					} else {
						this.createTable(wb, sheet, startRowIndex);
					}
				}
				XSSFRow row = null;
				if (isLastPage) {
					row = sheet.getRow(lastPageRowNum);
				} else {
					row = sheet.getRow(rowNum);
				}
				row.getCell(0).setCellValue((count + 1)); // 일련번호
				row.getCell(1).setCellValue(s.getLaboratoryId()); // 관리번호
				row.getCell(2).setCellValue("구강상피세포"); // 인체유래물등/검사대상물 종류
				row.getCell(3).setCellValue("TODO"); // 수증내역 - 연월일
				row.getCell(4).setCellValue("면봉 1ea, 가글 15mL"); // 수증내역 - 수증량
				row.getCell(5).setCellValue("보안책임자 별도관리"); // 수증내역 - 검체기증자 명(기관명)
				// #. 제공내용은 우선 작성안함
				// row.getCell(6).setCellValue(""); // 제공내용 - 연월일
				// row.getCell(7).setCellValue(""); // 제공내용 - 제공량
				// row.getCell(8).setCellValue(""); // 제공내용 - 제공 기관명
				row.getCell(9).setCellValue(s.getAnlsCmplDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // 폐기내용 - 연월일
				row.getCell(10).setCellValue("전량 폐기"); // 폐기내용 - 폐기량
				row.getCell(11).setCellValue("-"); // 폐기내용 - 폐기방법 - 자가처리
				row.getCell(12).setCellValue("(주)보광환경"); // 폐기내용 - 폐기방법 - 위탁처리
				row.getCell(13).setCellValue("냉장"); // 기타 - 보관조건
				row.getCell(14).setCellValue(s.getAnlsCmplMember().getName()); // 결재 - 담당
				row.getCell(15).setCellValue(s.getJdgmDrctApproveMember().getName()); // 결재 - 관리책임자

				count++;
			}
		}

		// #. 실험종료일 기준으로 월별로 list up
		// List<String> monthlyList = new ArrayList<String>();
		// Map<String, List<Map<String, Object>>> monthlySampleListMap = new HashMap<String, List<Map<String, Object>>>();

// 			for (Map<String, Object> dataMap : list) {
// 				String yearMonth = ((String)dataMap.get("resultDate")).substring(0, 7);
// 				if (!monthlyList.contains(yearMonth)) {
// 					// #. 연월값이 리스트에 없는 경우
// 					// #. 연월값을 리스트에 순차적으로 넣기
// 					monthlyList.add(yearMonth);
					
// 					// #. 연월별 검체 목록을 map에 넣기
// 					List<Map<String, Object>> samples = new ArrayList<Map<String, Object>>();
// 					samples.add(dataMap);
// 					monthlySampleListMap.put(yearMonth, samples);
// 				} else {
// 					// #. 연월값이 리스트에 있는 경우
// 					// #. map에서 연월값으로 리스트를 조회하여 해당 리스트에 sample 추가
// 					List<Map<String, Object>> samples = monthlySampleListMap.get(yearMonth);
// 					samples.add(dataMap);
// 				}
// 			}
			
// 			// #. 연월별 순차적으로 excel 만들기
// 			int pageStartSampleRowIndex = 7;
// 			int lastPageStartSampleRowIndex = 9;
// 			int pageSampleRowCount = 25;
// 			int pageTotalRowCount = 33;
// 			int lastPageTotalRowCount = 35;
// 			for (String month : monthlyList) {
// 				// #. 해당 연월에 실험종료된 목록
// 				List<Map<String, Object>> samples = monthlySampleListMap.get(month);
				
// 				// #. sheet 복사 생성
// 				Sheet destSheet = null;
// 				// #. 한페이지만 나오는 경우는 2번째 sheet복사
// 				if (samples.size() < 26) {
// 					destSheet = wb.cloneSheet(1, month);
// 				} else {
// 					destSheet = wb.cloneSheet(0, month);
// 				}
// 				logger.info(">>>>>>>>" + month + " : " + samples.size());
// 				int lastPageIndex = samples.size() / pageSampleRowCount;
// 				// #. 값 셋팅
// 				for (int i = 0; i < samples.size(); i++) {
// 					Map<String, Object> sample = samples.get(i);
// 					// #. 35개의 행을 복사하기 위해 
// 					int destStartRowIndex = (i / pageSampleRowCount) * pageTotalRowCount;
// 					int destLastPageStartRowIndex = (i / pageSampleRowCount) * pageTotalRowCount;
// 					// #. row 가져오기. rownum = 복사한 양식 시작 index + 페이지별 시작 row index + (i 를 25로 나눈나머지)
// 					int rowNum = destStartRowIndex + pageStartSampleRowIndex + (i % pageSampleRowCount);
// 					int lastPageRowNum = destLastPageStartRowIndex + lastPageStartSampleRowIndex + (i % pageSampleRowCount);
					
// 					// #. 마지막 페이지 여부
// 					boolean isLastPage = (lastPageIndex == (i / pageSampleRowCount));
					
// 					// #. 25개 기준으로 1개의 양식을 셋팅하기 때문에 고려하여 양식 복사
// 					if (i > 0 && i % pageSampleRowCount == 0) {
// 						// #. 마지막장인 경우 상단에 결재 추가한 sheet 양식 복사
// 						if (isLastPage) {
// 							// #. 마지막 페이지 양식 복사
// 							this.copyExcelRows(wb, wb.getSheetAt(1), destSheet, 0, (lastPageTotalRowCount - 1), destLastPageStartRowIndex);
// 						} else {
// 							// #. 기본 양식 복사
// 							this.copyExcelRows(wb, wb.getSheetAt(0), destSheet, 0, (pageTotalRowCount - 1), destStartRowIndex);
// 						}
// 					}
					
// 					XSSFRow row = destSheet.getRow(rowNum);
// 					if (isLastPage) {
// 						row = destSheet.getRow(lastPageRowNum);
// 					}
// 					row.getCell(0).setCellValue((i + 1)); // 일련번호
// 					row.getCell(1).setCellValue((String)sample.get("laboratory")); // 관리번호
// 					row.getCell(2).setCellValue("구강상피세포"); // 인체유래물등/검사대상물 종류
// 					row.getCell(3).setCellValue((String)sample.get("sampleReceived")); // 수증내역 - 연월일
// 					row.getCell(4).setCellValue("면봉 1ea, 가글 15mL"); // 수증내역 - 수증량
// 					row.getCell(5).setCellValue("보안책임자 별도관리"); // 수증내역 - 검체기증자 명(기관명)
// 					// #. 제공내용은 우선 작성안함
// //					row.getCell(6).setCellValue(""); // 제공내용 - 연월일
// //					row.getCell(7).setCellValue(""); // 제공내용 - 제공량
// //					row.getCell(8).setCellValue(""); // 제공내용 - 제공 기관명
// 					row.getCell(9).setCellValue(((String)sample.get("resultDate")).substring(0, 10)); // 폐기내용 - 연월일
// 					row.getCell(10).setCellValue("전량 폐기"); // 폐기내용 - 폐기량
// 					row.getCell(11).setCellValue("-"); // 폐기내용 - 폐기방법 - 자가처리
// 					row.getCell(12).setCellValue("(주)보광환경"); // 폐기내용 - 폐기방법 - 위탁처리
// 					row.getCell(13).setCellValue("냉장"); // 기타 - 보관조건
// 					row.getCell(14).setCellValue((String)sample.get("uploadUserName")); // 결재 - 담당
// 					row.getCell(15).setCellValue((String)sample.get("ifUserName")); // 결재 - 관리책임자
// 				}
// 			}
			
// 			// #. template sheet 2개 제거
// 			wb.removeSheetAt(0);
// 			wb.removeSheetAt(0);
			
		return wb;
	}

	private void createTable(XSSFWorkbook wb, XSSFSheet sheet, int startRowIndex) {
		// #. style 셋팅
		Font font8 = wb.createFont();
		font8.setFontHeightInPoints((short)8);
		font8.setFontName("맑은 고딕");

		Font font10 = wb.createFont();
		font10.setFontHeightInPoints((short)10);
		font8.setFontName("맑은 고딕");

		Font font16 = wb.createFont();
		font16.setFontHeightInPoints((short)16);
		font8.setFontName("맑은 고딕");

		CellStyle style1 = wb.createCellStyle();
		style1.setFont(font8);
		style1.setVerticalAlignment(VerticalAlignment.CENTER);

		CellStyle style2 = wb.createCellStyle();
		style2.setFont(font16);
		style2.setAlignment(HorizontalAlignment.CENTER);
		style2.setVerticalAlignment(VerticalAlignment.CENTER);

		CellStyle style3 = wb.createCellStyle();
		style3.setFont(font8);
		style3.setAlignment(HorizontalAlignment.RIGHT);
		style3.setVerticalAlignment(VerticalAlignment.CENTER);

		CellStyle defaultStyle = wb.createCellStyle();
		defaultStyle.setFont(font10);
		defaultStyle.setWrapText(true);
		defaultStyle.setAlignment(HorizontalAlignment.CENTER);
		defaultStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		defaultStyle.setBorderRight(BorderStyle.THIN);
		defaultStyle.setBorderBottom(BorderStyle.THIN);
		defaultStyle.setBorderLeft(BorderStyle.THIN);
		defaultStyle.setBorderTop(BorderStyle.THIN);

		int rowCount = 33;
		int cellCount = 16;

		for (int ri = 0; ri < rowCount; ri++) {
			XSSFRow row = sheet.createRow(startRowIndex + ri);
			// #. row hight
			if (ri == 0) { row.setHeight((short)(34 * 15.1)); }
			else if (ri == 1) { row.setHeight((short)(50 * 15.1)); }
			else if (ri == 2) { row.setHeight((short)(34 * 15.1)); }
			else if (ri >= 3 && ri <= 6) { row.setHeight((short)(25 * 15.1)); }
			else if (ri >= 7 && ri <= 31) { row.setHeight((short)(30 * 15.1)); }
			else if (ri == 32) { row.setHeight((short)(40 * 15.1)); }
			for (int ci = 0; ci < cellCount; ci++) {
				XSSFCell cell = row.createCell(ci);

				if (ri > 2) cell.setCellStyle(defaultStyle);

				if (ri == 0) {
					if (ci == 0) { 
						cell.setCellValue("■ 생명윤리 및 안전에 관한 법률 시행규칙 [별지 제35호서식]");
						cell.setCellStyle(style1);
					}
				} else if (ri == 1) {
					if (ci == 0) {
						cell.setCellValue("인체유래물등(검사대상물) 관리대장");
						cell.setCellStyle(style2);
					}
				} else if (ri == 3) {
					if (ci == 0) { cell.setCellValue("기관 명칭"); }
					else if (ci == 2) { cell.setCellValue("(주) 클리노믹스"); }
					else if (ci == 9) { cell.setCellValue("기관 허가(신고)번호"); }
					else if (ci == 11) { cell.setCellValue("제 218호"); }
				} else if (ri == 4) {
					if (ci == 0) {cell.setCellValue("일련번호");}
					else if (ci == 1) {cell.setCellValue("관리번호");}
					else if (ci == 2) {cell.setCellValue("인체유래물등/검사대상물 종류");}
					else if (ci == 3) {cell.setCellValue("수증내역");}
					else if (ci == 6) {cell.setCellValue("제공내용");}
					else if (ci == 9) {cell.setCellValue("폐기내용");}
					else if (ci == 13) {cell.setCellValue("기타");}
					else if (ci == 14) {cell.setCellValue("결재");}
				} else if (ri == 5) {
					if (ci == 3) {cell.setCellValue("연월일"); }
					else if (ci == 4) {cell.setCellValue("수증량"); }
					else if (ci == 5) {cell.setCellValue("검체기증자 명\n(기관명)"); }
					else if (ci == 6) {cell.setCellValue("연월일"); }
					else if (ci == 7) {cell.setCellValue("제공량"); }
					else if (ci == 8) {cell.setCellValue("제공 기관명"); }
					else if (ci == 9) {cell.setCellValue("연월일"); }
					else if (ci == 10) {cell.setCellValue("폐기량"); }
					else if (ci == 11) {cell.setCellValue("폐기 방법"); }
					else if (ci == 13) {cell.setCellValue("보관조건"); }
					else if (ci == 14) {cell.setCellValue("담당"); }
					else if (ci == 15) {cell.setCellValue("관리책임자"); }
				} else if (ri == 6) {
					if (ci == 11) {cell.setCellValue("자가처리"); }
					else if (ci == 12) {cell.setCellValue("위탁처리"); }
				} else if (ri == 32) {
					if (ci == 0) {
						cell.setCellValue("297mm×210mm[보존용지(1종) 70g/㎡]");
						cell.setCellStyle(style3);
					}
				}
			}
		}

		// #. Cell width height 조정
		sheet.setColumnWidth(0, 38 * 32);
		sheet.setColumnWidth(1, 112 * 32);
		sheet.setColumnWidth(2, 117 * 32);
		sheet.setColumnWidth(3, 81 * 32);
		sheet.setColumnWidth(4, 133 * 32);
		sheet.setColumnWidth(5, 192 * 32);
		sheet.setColumnWidth(6, 85 * 32);
		sheet.setColumnWidth(7, 72 * 32);
		sheet.setColumnWidth(8, 126 * 32);
		sheet.setColumnWidth(9, 78 * 32);
		sheet.setColumnWidth(10, 72 * 32);
		sheet.setColumnWidth(11, 77 * 32);
		sheet.setColumnWidth(12, 85 * 32);
		sheet.setColumnWidth(13, 85 * 32);
		sheet.setColumnWidth(14, 85 * 32);
		sheet.setColumnWidth(15, 85 * 32);

		// #. cell border 셋팅
		CellRangeAddress region = new CellRangeAddress(startRowIndex + 0, startRowIndex + 32, 15, 15);
		RegionUtil.setBorderRight(BorderStyle.MEDIUM, region, sheet);

		CellRangeAddress region2 = new CellRangeAddress(startRowIndex + 2, startRowIndex + 2, 0, 15);
		RegionUtil.setBorderBottom(BorderStyle.THICK, region2, sheet);

		CellRangeAddress region3 = new CellRangeAddress(startRowIndex + 32, startRowIndex + 32, 0, 15);
		RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region3, sheet);

		// #. cell merge
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex, startRowIndex, 0, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 1, startRowIndex + 1, 0, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 2, startRowIndex + 2, 0, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 3, startRowIndex + 3, 0, 1));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 3, startRowIndex + 3, 2, 8));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 3, startRowIndex + 3, 9, 10));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 3, startRowIndex + 3, 11, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 6, 0, 0));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 6, 1, 1));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 6, 2, 2));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 4, 3, 5));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 4, 6, 8));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 4, 9, 12));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 4, 14, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 3, 3));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 4, 4));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 5, 5));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 6, 6));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 7, 7));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 8, 8));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 9, 9));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 10, 10));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 5, 11, 12));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 13, 13));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 14, 14));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 6, 15, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 32, startRowIndex + 32, 0, 15));
	}

	private void createLastTable(XSSFWorkbook wb, XSSFSheet sheet, int startRowIndex) {
		// #. style 셋팅
		Font font8 = wb.createFont();
		font8.setFontHeightInPoints((short)8);
		font8.setFontName("맑은 고딕");

		Font font10 = wb.createFont();
		font10.setFontHeightInPoints((short)10);
		font8.setFontName("맑은 고딕");

		Font font16 = wb.createFont();
		font16.setFontHeightInPoints((short)16);
		font8.setFontName("맑은 고딕");

		CellStyle style1 = wb.createCellStyle();
		style1.setFont(font8);
		style1.setVerticalAlignment(VerticalAlignment.CENTER);

		CellStyle style2 = wb.createCellStyle();
		style2.setFont(font16);
		style2.setAlignment(HorizontalAlignment.CENTER);
		style2.setVerticalAlignment(VerticalAlignment.CENTER);

		CellStyle style3 = wb.createCellStyle();
		style3.setFont(font8);
		style3.setAlignment(HorizontalAlignment.RIGHT);
		style3.setVerticalAlignment(VerticalAlignment.CENTER);

		CellStyle defaultStyle = wb.createCellStyle();
		defaultStyle.setFont(font10);
		defaultStyle.setWrapText(true);
		defaultStyle.setAlignment(HorizontalAlignment.CENTER);
		defaultStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		defaultStyle.setBorderRight(BorderStyle.THIN);
		defaultStyle.setBorderBottom(BorderStyle.THIN);
		defaultStyle.setBorderLeft(BorderStyle.THIN);
		defaultStyle.setBorderTop(BorderStyle.THIN);

		int rowCount = 35;
		int cellCount = 16;

		for (int ri = 0; ri < rowCount; ri++) {
			XSSFRow row = sheet.createRow(startRowIndex + ri);
			// #. row hight
			if (ri == 0) { row.setHeight((short)(30 * 15.1)); }
			else if (ri == 1) { row.setHeight((short)(83 * 15.1)); }
			else if (ri == 2) { row.setHeight((short)(34 * 15.1)); }
			else if (ri == 3) { row.setHeight((short)(50 * 15.1)); }
			else if (ri == 4) { row.setHeight((short)(34 * 15.1)); }
			else if (ri >= 5 && ri <= 7) { row.setHeight((short)(25 * 15.1)); }
			else if (ri >= 9 && ri <= 33) { row.setHeight((short)(30 * 15.1)); }
			else if (ri == 34) { row.setHeight((short)(40 * 15.1)); }
			for (int ci = 0; ci < cellCount; ci++) {
				XSSFCell cell = row.createCell(ci);

				if (ri > 4) cell.setCellStyle(defaultStyle);

				if (ri == 0) {
					if (ci == 12) { 
						cell.setCellValue("결 재");
					} else if (ci == 13) {
						cell.setCellValue("담 당");
					} else if (ci == 14) {
						cell.setCellValue("검 토");
					} else if (ci == 15) {
						cell.setCellValue("승 인");
					}
				} else if (ri == 2) {
					if (ci == 0) { 
						cell.setCellValue("■ 생명윤리 및 안전에 관한 법률 시행규칙 [별지 제35호서식]");
						cell.setCellStyle(style1);
					}
				} else if (ri == 3) {
					if (ci == 0) {
						cell.setCellValue("인체유래물등(검사대상물) 관리대장");
						cell.setCellStyle(style2);
					}
				} else if (ri == 5) {
					if (ci == 0) { cell.setCellValue("기관 명칭"); }
					else if (ci == 2) { cell.setCellValue("(주) 클리노믹스"); }
					else if (ci == 9) { cell.setCellValue("기관 허가(신고)번호"); }
					else if (ci == 11) { cell.setCellValue("제 218호"); }
				} else if (ri == 6) {
					if (ci == 0) {cell.setCellValue("일련번호");}
					else if (ci == 1) {cell.setCellValue("관리번호");}
					else if (ci == 2) {cell.setCellValue("인체유래물등/검사대상물 종류");}
					else if (ci == 3) {cell.setCellValue("수증내역");}
					else if (ci == 6) {cell.setCellValue("제공내용");}
					else if (ci == 9) {cell.setCellValue("폐기내용");}
					else if (ci == 13) {cell.setCellValue("기타");}
					else if (ci == 14) {cell.setCellValue("결재");}
				} else if (ri ==7) {
					if (ci == 3) {cell.setCellValue("연월일"); }
					else if (ci == 4) {cell.setCellValue("수증량"); }
					else if (ci == 5) {cell.setCellValue("검체기증자 명\n(기관명)"); }
					else if (ci == 6) {cell.setCellValue("연월일"); }
					else if (ci == 7) {cell.setCellValue("제공량"); }
					else if (ci == 8) {cell.setCellValue("제공 기관명"); }
					else if (ci == 9) {cell.setCellValue("연월일"); }
					else if (ci == 10) {cell.setCellValue("폐기량"); }
					else if (ci == 11) {cell.setCellValue("폐기 방법"); }
					else if (ci == 13) {cell.setCellValue("보관조건"); }
					else if (ci == 14) {cell.setCellValue("담당"); }
					else if (ci == 15) {cell.setCellValue("관리책임자"); }
				} else if (ri == 8) {
					if (ci == 11) {cell.setCellValue("자가처리"); }
					else if (ci == 12) {cell.setCellValue("위탁처리"); }
				} else if (ri == 34) {
					if (ci == 0) {
						cell.setCellValue("297mm×210mm[보존용지(1종) 70g/㎡]");
						cell.setCellStyle(style3);
					}
				}
			}
		}

		// #. Cell width height 조정
		sheet.setColumnWidth(0, 38 * 32);
		sheet.setColumnWidth(1, 112 * 32);
		sheet.setColumnWidth(2, 117 * 32);
		sheet.setColumnWidth(3, 81 * 32);
		sheet.setColumnWidth(4, 133 * 32);
		sheet.setColumnWidth(5, 192 * 32);
		sheet.setColumnWidth(6, 85 * 32);
		sheet.setColumnWidth(7, 72 * 32);
		sheet.setColumnWidth(8, 126 * 32);
		sheet.setColumnWidth(9, 78 * 32);
		sheet.setColumnWidth(10, 72 * 32);
		sheet.setColumnWidth(11, 77 * 32);
		sheet.setColumnWidth(12, 85 * 32);
		sheet.setColumnWidth(13, 85 * 32);
		sheet.setColumnWidth(14, 85 * 32);
		sheet.setColumnWidth(15, 85 * 32);

		// #. cell border 셋팅
		CellRangeAddress region = new CellRangeAddress(startRowIndex, startRowIndex + 1, 12, 15);
		RegionUtil.setBorderRight(BorderStyle.MEDIUM, region, sheet);
		RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region, sheet);
		RegionUtil.setBorderLeft(BorderStyle.MEDIUM, region, sheet);
		RegionUtil.setBorderTop(BorderStyle.MEDIUM, region, sheet);

		CellRangeAddress region1 = new CellRangeAddress(startRowIndex + 2, startRowIndex + 34, 15, 15);
		RegionUtil.setBorderRight(BorderStyle.MEDIUM, region1, sheet);

		CellRangeAddress region2 = new CellRangeAddress(startRowIndex + 4, startRowIndex + 4, 0, 15);
		RegionUtil.setBorderBottom(BorderStyle.THICK, region2, sheet);

		CellRangeAddress region3 = new CellRangeAddress(startRowIndex + 34, startRowIndex + 34, 0, 15);
		RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region3, sheet);

		// #. cell merge
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex, startRowIndex + 1, 0, 0));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 2, startRowIndex + 2, 0, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 3, startRowIndex + 3, 0, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 4, startRowIndex + 4, 0, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 5, 0, 1));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 5, 2, 8));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 5, 9, 10));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 5, startRowIndex + 5, 11, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 6, startRowIndex + 8, 0, 0));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 6, startRowIndex + 8, 1, 1));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 6, startRowIndex + 8, 2, 2));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 6, startRowIndex + 6, 3, 5));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 6, startRowIndex + 6, 6, 8));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 6, startRowIndex + 6, 9, 12));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 6, startRowIndex + 6, 14, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 3, 3));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 4, 4));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 5, 5));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 6, 6));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 7, 7));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 8, 8));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 9, 9));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 10, 10));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 7, 11, 12));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 13, 13));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 14, 14));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 7, startRowIndex + 8, 15, 15));
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex + 34, startRowIndex + 34, 0, 15));
	}
}
