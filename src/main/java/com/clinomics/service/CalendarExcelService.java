package com.clinomics.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	public XSSFWorkbook exportHumanExcelForm(Map<String, Object> params) {
		logger.info(">> start writeExcelFileForGsHumanOrigin");
		// #. excel 읽기
		XSSFWorkbook wb = new XSSFWorkbook();

		// #. 발행완료일 기준으로 목록 조회
		Specification<Sample> where = Specification
				.where(SampleSpecification.customDateBetween("outputCmplDate", params))
				.and(SampleSpecification.bundleId(params))
				.and(SampleSpecification.bundleIsActive())
				.and(SampleSpecification.statusCodeGt(710));
		List<Sample> samples = sampleRepository.findAll(where);


		if (samples.size() < 1) {
			wb.createSheet();
			return wb;
		}

		List<String> monthlyList = new ArrayList<String>();
		Map<String, List<Sample>> monthlySamplesMap = Maps.newHashMap();
		for (Sample sample : samples) {
			String month = sample.getOutputCmplDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
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

		for (String month : monthlyList) {
			List<Sample> sList = monthlySamplesMap.get(month);
			// #. sheet 생성
			XSSFSheet sheet = wb.createSheet(month);
			int index = 0;
			int lastPageIndex = (sList.size() - 1) / pageSampleRowCount;
			for (Sample s : sList) {
				boolean isLastPage = (lastPageIndex == (index / pageSampleRowCount));
				int startRowIndex = (index / pageSampleRowCount) * pageTotalRowCount;
				int rowNum = startRowIndex + pageStartSampleRowIndex + (index % pageSampleRowCount);
				int lastPageRowNum = startRowIndex + lastPageStartSampleRowIndex + (index % pageSampleRowCount);

				// #. Sample을 25개씩 나눠서 표시 하기위해 
				if (index % pageSampleRowCount == 0) {
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

				String sampleTarget = "구강상피세포";
				String samplingSheep = "면봉 1ea, 가글 15mL";
				if ("Blood".equals(s.getSampleType())) {
					sampleTarget = "혈액";
					samplingSheep = "3mL";
				}

				row.getCell(0).setCellValue((index + 1)); // 일련번호
				row.getCell(1).setCellValue(s.getLaboratoryId()); // 관리번호
				row.getCell(2).setCellValue(sampleTarget); // 인체유래물등/검사대상물 종류
				row.getCell(3).setCellValue((s.getReceivedDate() != null ? s.getReceivedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "")); // 수증내역 - 연월일
				row.getCell(4).setCellValue(samplingSheep); // 수증내역 - 수증량
				row.getCell(5).setCellValue("보안책임자 별도관리"); // 수증내역 - 검체기증자 명(기관명)
				// #. 제공내용은 우선 작성안함
				// row.getCell(6).setCellValue(""); // 제공내용 - 연월일
				// row.getCell(7).setCellValue(""); // 제공내용 - 제공량
				// row.getCell(8).setCellValue(""); // 제공내용 - 제공 기관명
				row.getCell(9).setCellValue(s.getOutputCmplDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // 폐기내용 - 연월일
				row.getCell(10).setCellValue("전량 폐기"); // 폐기내용 - 폐기량
				row.getCell(11).setCellValue("-"); // 폐기내용 - 폐기방법 - 자가처리
				row.getCell(12).setCellValue("(주)보광환경"); // 폐기내용 - 폐기방법 - 위탁처리
				row.getCell(13).setCellValue("냉장"); // 기타 - 보관조건
				row.getCell(14).setCellValue(s.getOutputWaitMember().getName()); // 결재 - 담당
				row.getCell(15).setCellValue(s.getJdgmDrctApproveMember().getName()); // 결재 - 관리책임자

				index++;
			}
		}
		return wb;
	}

	private void createTable(XSSFWorkbook wb, XSSFSheet sheet, int startRowIndex) {
		// #. style 셋팅
		Font font8 = wb.createFont();
		font8.setFontHeightInPoints((short)8);
		font8.setFontName("맑은 고딕");

		Font font10 = wb.createFont();
		font10.setFontHeightInPoints((short)10);
		font10.setFontName("맑은 고딕");

		Font font16 = wb.createFont();
		font16.setFontHeightInPoints((short)16);
		font16.setFontName("맑은 고딕");

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
		font10.setFontName("맑은 고딕");

		Font font16 = wb.createFont();
		font16.setFontHeightInPoints((short)16);
		font16.setFontName("맑은 고딕");

		CellStyle style0 = wb.createCellStyle();
		style0.setFont(font10);
		style0.setAlignment(HorizontalAlignment.CENTER);
		style0.setVerticalAlignment(VerticalAlignment.CENTER);
		style0.setBorderRight(BorderStyle.MEDIUM);
		style0.setBorderBottom(BorderStyle.MEDIUM);
		style0.setBorderLeft(BorderStyle.MEDIUM);
		style0.setBorderTop(BorderStyle.MEDIUM);

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
			else if (ri >= 9 && ri <= 33) { row.setHeight((short)(25 * 15.1)); }
			else if (ri == 34) { row.setHeight((short)(40 * 15.1)); }
			for (int ci = 0; ci < cellCount; ci++) {
				XSSFCell cell = row.createCell(ci);

				if (ri > 4) cell.setCellStyle(defaultStyle);

				if (ri == 0 || ri == 1) {
					if (ci >= 12) {
						cell.setCellStyle(style0);
					}
				}

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
		CellRangeAddress region0 = new CellRangeAddress(startRowIndex + 2, startRowIndex + 2, 0, 15);
		RegionUtil.setBorderTop(BorderStyle.MEDIUM, region0, sheet);

		CellRangeAddress region1 = new CellRangeAddress(startRowIndex + 2, startRowIndex + 34, 15, 15);
		RegionUtil.setBorderRight(BorderStyle.MEDIUM, region1, sheet);

		CellRangeAddress region2 = new CellRangeAddress(startRowIndex + 4, startRowIndex + 4, 0, 15);
		RegionUtil.setBorderBottom(BorderStyle.THICK, region2, sheet);

		CellRangeAddress region3 = new CellRangeAddress(startRowIndex + 34, startRowIndex + 34, 0, 15);
		RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region3, sheet);

		// #. cell merge
		sheet.addMergedRegion(new CellRangeAddress(startRowIndex, startRowIndex + 1, 12, 12));
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
