package com.clinomics.service;

import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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

import com.clinomics.entity.lims.Holiday;
import com.clinomics.entity.lims.Sample;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.HolidayRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;

@Service
public class CalendarExcelService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
    HolidayRepository holidayRepository;
	
	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ExcelReadComponent excelReadComponent;

	public XSSFWorkbook exportHumanExcelForm(Map<String, String> params) {
		logger.info(">> start writeExcelFileForGsHumanOrigin");
		// #. excel 읽기
		XSSFWorkbook wb = new XSSFWorkbook();

		// #. 발행완료일 기준으로 목록 조회
		Specification<Sample> where = Specification
				.where(SampleSpecification.customDateBetween("outputCmplDate", params))
				.and(SampleSpecification.isNotTest())
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

			// #. 2022-04 이전 이면 (주)보광환경, 이후면 (주)삼원산업
			String company = "(주)보광환경";
			LocalDate standard = LocalDate.parse("2022-03-31");
			LocalDate monthDate = LocalDate.parse(month + "-01");

			if (monthDate.isAfter(standard)) {
				company = "(주)삼원산업";
			}

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
				String sampleDiscardAmount = "면봉 1ea, 가글 15mL, DNA 전량";
				if ("Blood".equals(s.getSampleType())) {
					sampleTarget = "혈액";
					samplingSheep = "전혈 3mL";
					sampleDiscardAmount = "전혈 3mL, DNA 전량";
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
				row.getCell(9).setCellValue(this.getDiscardDate(s.getOutputCmplDate())); // 폐기내용 - 연월일
				row.getCell(10).setCellValue(sampleDiscardAmount); // 폐기내용 - 폐기량
				row.getCell(11).setCellValue("-"); // 폐기내용 - 폐기방법 - 자가처리
				row.getCell(12).setCellValue(company); // 폐기내용 - 폐기방법 - 위탁처리
				row.getCell(13).setCellValue("냉장"); // 기타 - 보관조건
				row.getCell(14).setCellValue("원미나"); // 결재 - 담당
				// row.getCell(15).setCellValue(s.getJdgmDrctApproveMember().getName()); // 결재 - 관리책임자
				row.getCell(15).setCellValue("김병철"); // 결재 - 관리책임자

				index++;
			}
		}
		return wb;
	}

	public XSSFWorkbook exportTestResultStatisticsExcelForm(Map<String, String> params) {
		logger.info(">> start exportTestResultStatisticsExcelForm");
		// #. excel 읽기
		XSSFWorkbook wb = new XSSFWorkbook();

		// #. 발행완료일 기준으로 목록 조회
		Specification<Sample> where = Specification
				.where(SampleSpecification.customDateBetween("outputCmplDate", params))
				.and(SampleSpecification.isNotTest())
				.and(SampleSpecification.isHospital(true))
				.and(SampleSpecification.isLastVersionTrue())
				.and(SampleSpecification.bundleId(params))
				.and(SampleSpecification.bundleIsActive())
				.and(SampleSpecification.statusCodeGt(710));
		List<Sample> samples = sampleRepository.findAll(where);

		if (samples.size() < 1) {
			wb.createSheet();
			return wb;
		}

		// #. 병원용 상품에 있는 마커 정보
		Map<String, String> markerMap = Maps.newHashMap();
		markerMap.put("rs10036748", "CT"); markerMap.put("rs1004467", "AG"); markerMap.put("rs10054504", "CT"); markerMap.put("rs1006737", "AG"); markerMap.put("rs10086568", "AG"); markerMap.put("rs10088218", "AG"); markerMap.put("rs1014971", "CT"); markerMap.put("rs1015213", "CT"); markerMap.put("rs10166942", "CT"); markerMap.put("rs10168266", "CT"); markerMap.put("rs1038304", "AG"); markerMap.put("rs10411210", "CT"); markerMap.put("rs10416218", "CT"); markerMap.put("rs1045642", "AG"); markerMap.put("rs10463311", "CT"); markerMap.put("rs10474352", "CT"); markerMap.put("rs10483727", "CT"); markerMap.put("rs10486776", "AG"); markerMap.put("rs10489202", "GT"); markerMap.put("rs10490924", "GT"); markerMap.put("rs10491734", "CT"); markerMap.put("rs10503253", "AC"); markerMap.put("rs10506868", "CT"); markerMap.put("rs10507248", "GT"); markerMap.put("rs10508372", "AG"); markerMap.put("rs10516487", "AG"); markerMap.put("rs1052133", "CG"); markerMap.put("rs10737680", "AC"); markerMap.put("rs10757278", "AG"); markerMap.put("rs10774214", "CT"); markerMap.put("rs10797576", "CT"); markerMap.put("rs10811661", "CT"); markerMap.put("rs10822013", "CT"); markerMap.put("rs10859871", "AC"); markerMap.put("rs10865331", "AG"); markerMap.put("rs10936599", "CT"); markerMap.put("rs10937625", "CT"); markerMap.put("rs10947262", "CT"); markerMap.put("rs10948363", "AG"); markerMap.put("rs10958409", "AG"); markerMap.put("rs10968576", "AG"); markerMap.put("rs10993994", "CT"); markerMap.put("rs11013860", "AC"); markerMap.put("rs11024102", "CT"); markerMap.put("rs11030101", "AT"); markerMap.put("rs110419", "AG"); markerMap.put("rs11052413", "GT"); markerMap.put("rs11064437", "CT"); markerMap.put("rs11076008", "AG"); markerMap.put("rs1111875", "CT"); markerMap.put("rs11134527", "AG"); markerMap.put("rs11136000", "CT"); markerMap.put("rs11150612", "AG"); markerMap.put("rs11172113", "CT"); markerMap.put("rs11177", "AG"); markerMap.put("rs11190870", "CT"); markerMap.put("rs11196172", "AG"); markerMap.put("rs11218343", "CT"); markerMap.put("rs11235604", "CT"); markerMap.put("rs1154155", "GT"); markerMap.put("rs11543198", "AG"); markerMap.put("rs1160312", "AG"); markerMap.put("rs11614913", "CT"); markerMap.put("rs11653176", "CT"); markerMap.put("rs117026326", "CT"); markerMap.put("rs11711441", "AG"); markerMap.put("rs1173771", "AG"); markerMap.put("rs11746443", "AG"); markerMap.put("rs11931074", "GT"); markerMap.put("rs11950646", "AG"); markerMap.put("rs12044852", "AC"); markerMap.put("rs12134279", "CT"); markerMap.put("rs12261843", "GT"); markerMap.put("rs1229984", "CT"); markerMap.put("rs1234315", "CT"); markerMap.put("rs12413409", "AG"); markerMap.put("rs12425791", "AG"); markerMap.put("rs12456492", "AG"); markerMap.put("rs12478601", "CT"); markerMap.put("rs12504628", "CT"); markerMap.put("rs1256328", "CT"); markerMap.put("rs12593813", "AG"); markerMap.put("rs1260326", "CT"); markerMap.put("rs12603526", "CT"); markerMap.put("rs12615545", "CT"); markerMap.put("rs12632110", "AG"); markerMap.put("rs12634229", "CT"); markerMap.put("rs1265181", "CG"); markerMap.put("rs12653946", "CT"); markerMap.put("rs12654264", "AT"); markerMap.put("rs12700667", "AG"); markerMap.put("rs12764378", "AG"); markerMap.put("rs12791447", "AG"); markerMap.put("rs1286041", "CT"); markerMap.put("rs1295686", "CT"); markerMap.put("rs12979860", "CT"); markerMap.put("rs13016963", "AG"); markerMap.put("rs13076312", "CT"); markerMap.put("rs13139571", "AC"); markerMap.put("rs132617", "CT"); markerMap.put("rs13266634", "CT"); markerMap.put("rs13277113", "AG"); markerMap.put("rs13278062", "GT"); markerMap.put("rs13336428", "AG"); markerMap.put("rs13361707", "CT"); markerMap.put("rs13376333", "CT"); markerMap.put("rs13382811", "CT"); markerMap.put("rs13385731", "CT"); markerMap.put("rs13394619", "AG"); markerMap.put("rs13405728", "AG"); markerMap.put("rs1373004", "GT"); markerMap.put("rs1378942", "AC"); markerMap.put("rs1385374", "CT"); markerMap.put("rs1412829", "AG"); markerMap.put("rs1419881", "AG"); markerMap.put("rs143383", "AG"); markerMap.put("rs1447295", "AC"); markerMap.put("rs1465618", "CT"); markerMap.put("rs1476679", "CT"); markerMap.put("rs1495965", "CT"); markerMap.put("rs1511412", "AG"); markerMap.put("rs1512268", "CT"); markerMap.put("rs1513670", "CT"); markerMap.put("rs1537377", "CT"); markerMap.put("rs1558902", "AT"); markerMap.put("rs1564981", "AG"); markerMap.put("rs1571878", "CT"); markerMap.put("rs1585471", "AG"); markerMap.put("rs1625579", "GT"); markerMap.put("rs163879", "CT"); markerMap.put("rs1676486", "AG"); markerMap.put("rs16940202", "CT"); markerMap.put("rs16952362", "CG"); markerMap.put("rs16969968", "AG"); markerMap.put("rs16976358", "CT"); markerMap.put("rs16998073", "AT"); markerMap.put("rs1701704", "GT"); markerMap.put("rs17085007", "CT"); markerMap.put("rs17095830", "AG"); markerMap.put("rs17145738", "CT"); markerMap.put("rs17249754", "AG"); markerMap.put("rs17319721", "AG"); markerMap.put("rs17401966", "AG"); markerMap.put("rs174537", "GT"); markerMap.put("rs1746048", "CT"); markerMap.put("rs17465637", "AC"); markerMap.put("rs17685", "AG"); markerMap.put("rs17728338", "AG"); markerMap.put("rs17782313", "CT"); markerMap.put("rs179785", "AG"); markerMap.put("rs1799945", "CG"); markerMap.put("rs1800414", "CT"); markerMap.put("rs1800470", "AG"); markerMap.put("rs1800497", "AG"); markerMap.put("rs1800630", "AC"); markerMap.put("rs1800871", "AG"); markerMap.put("rs1800896", "CT"); markerMap.put("rs1801133", "AG"); markerMap.put("rs1801260", "AG"); markerMap.put("rs1801274", "AG"); markerMap.put("rs182052", "AG"); markerMap.put("rs1837253", "CT"); markerMap.put("rs1883025", "CT"); markerMap.put("rs1886970", "CT"); markerMap.put("rs1894116", "AG"); markerMap.put("rs1906953", "CT"); markerMap.put("rs1938526", "AG"); markerMap.put("rs1938781", "AG"); markerMap.put("rs1983891", "CT"); markerMap.put("rs1994090", "GT"); markerMap.put("rs2004640", "GT"); markerMap.put("rs2014300", "AG"); markerMap.put("rs2046210", "AG"); markerMap.put("rs204993", "AG"); markerMap.put("rs20541", "AG"); markerMap.put("rs2059807", "AG"); markerMap.put("rs2070600", "CT"); markerMap.put("rs2071278", "AG"); markerMap.put("rs2073711", "AG"); markerMap.put("rs2075650", "AG"); markerMap.put("rs2075876", "AG"); markerMap.put("rs2106261", "CT"); markerMap.put("rs2107595", "AG"); markerMap.put("rs2131925", "GT"); markerMap.put("rs2155219", "GT"); markerMap.put("rs2179920", "CT"); markerMap.put("rs2180439", "CT"); markerMap.put("rs2187668", "CT"); markerMap.put("rs2191349", "GT"); markerMap.put("rs2200733", "CT"); markerMap.put("rs2228479", "AG"); markerMap.put("rs2230500", "AG"); markerMap.put("rs2230926", "GT"); markerMap.put("rs2231142", "GT"); markerMap.put("rs2235529", "CT"); markerMap.put("rs2237892", "CT"); markerMap.put("rs2239612", "AG"); markerMap.put("rs2239815", "CT"); markerMap.put("rs224136", "CT"); markerMap.put("rs2243250", "CT"); markerMap.put("rs2243407", "CT"); markerMap.put("rs2254546", "AG"); markerMap.put("rs2268361", "CT"); markerMap.put("rs2274223", "AG"); markerMap.put("rs2275294", "AG"); markerMap.put("rs2275913", "AG"); markerMap.put("rs2279744", "GT"); markerMap.put("rs2284038", "AG"); markerMap.put("rs2290203", "AG"); markerMap.put("rs2292239", "GT"); markerMap.put("rs2293370", "AG"); markerMap.put("rs2294008", "CT"); markerMap.put("rs2294693", "CT"); markerMap.put("rs229527", "AC"); markerMap.put("rs2297518", "AG"); markerMap.put("rs2301888", "AG"); markerMap.put("rs2305795", "AG"); markerMap.put("rs2310173", "GT"); markerMap.put("rs231775", "AG"); markerMap.put("rs2392510", "CT"); markerMap.put("rs2399399", "AG"); markerMap.put("rs2412971", "AG"); markerMap.put("rs2414739", "AG"); markerMap.put("rs243021", "AG"); markerMap.put("rs2439302", "CG"); markerMap.put("rs2467853", "GT"); markerMap.put("rs2479106", "AG"); markerMap.put("rs2596542", "CT"); markerMap.put("rs2647012", "CT"); markerMap.put("rs2651899", "CT"); markerMap.put("rs2660753", "CT"); markerMap.put("rs267939", "CT"); markerMap.put("rs2730260", "GT"); markerMap.put("rs2735839", "AG"); markerMap.put("rs2736100", "AC"); markerMap.put("rs2736340", "CT"); markerMap.put("rs2738048", "AG"); markerMap.put("rs27434", "AG"); markerMap.put("rs2836878", "AG"); markerMap.put("rs2856717", "AG"); markerMap.put("rs2856718", "CT"); markerMap.put("rs2872507", "AG"); markerMap.put("rs2895811", "CT"); markerMap.put("rs2896019", "GT"); markerMap.put("rs2903692", "AG"); markerMap.put("rs2932538", "AG"); markerMap.put("rs294958", "AG"); markerMap.put("rs2954029", "AT"); markerMap.put("rs2981582", "AG"); markerMap.put("rs3087243", "AG"); markerMap.put("rs3118470", "CT"); markerMap.put("rs3129943", "AG"); markerMap.put("rs3130542", "AG"); markerMap.put("rs31489", "AC"); markerMap.put("rs3194051", "AG"); markerMap.put("rs3212227", "GT"); markerMap.put("rs3213787", "AG"); markerMap.put("rs334353", "GT"); markerMap.put("rs339331", "CT"); markerMap.put("rs33972313", "CT"); markerMap.put("rs344081", "CT"); markerMap.put("rs35771982", "CG"); markerMap.put("rs3736228", "CT"); markerMap.put("rs3753841", "AG"); markerMap.put("rs3754334", "AG"); markerMap.put("rs3759223", "AG"); markerMap.put("rs3761959", "CT"); markerMap.put("rs3764261", "AC"); markerMap.put("rs3782886", "CT"); markerMap.put("rs3790844", "AG"); markerMap.put("rs3794087", "GT"); markerMap.put("rs3801387", "AG"); markerMap.put("rs3802842", "AC"); markerMap.put("rs3803662", "AG"); markerMap.put("rs3803800", "AG"); markerMap.put("rs3807989", "AG"); markerMap.put("rs3817963", "CT"); markerMap.put("rs3825942", "AG"); markerMap.put("rs3827760", "AG"); markerMap.put("rs3851179", "CT"); markerMap.put("rs38845", "AG"); markerMap.put("rs3903239", "AG"); markerMap.put("rs401681", "CT"); markerMap.put("rs402710", "CT"); markerMap.put("rs4065", "CT"); markerMap.put("rs4072037", "CT"); markerMap.put("rs4078288", "AG"); markerMap.put("rs4112788", "AG"); markerMap.put("rs4130590", "AG"); markerMap.put("rs4142110", "CT"); markerMap.put("rs422951", "CT"); markerMap.put("rs4236", "CT"); markerMap.put("rs4282438", "GT"); markerMap.put("rs429608", "AG"); markerMap.put("rs4307059", "CT"); markerMap.put("rs4379368", "CT"); markerMap.put("rs4380451", "CT"); markerMap.put("rs4402960", "GT"); markerMap.put("rs4410790", "CT"); markerMap.put("rs4420638", "AG"); markerMap.put("rs4430796", "AG"); markerMap.put("rs4488809", "CT"); markerMap.put("rs4513093", "AG"); markerMap.put("rs4607517", "AG"); markerMap.put("rs4626664", "AG"); markerMap.put("rs4628172", "GT"); markerMap.put("rs4633", "CT"); markerMap.put("rs463426", "CT"); markerMap.put("rs4656461", "AG"); markerMap.put("rs4678680", "GT"); markerMap.put("rs4698412", "AG"); markerMap.put("rs4712523", "AG"); markerMap.put("rs4728142", "AG"); markerMap.put("rs4744712", "AC"); markerMap.put("rs4773144", "AG"); markerMap.put("rs4779584", "CT"); markerMap.put("rs4785204", "CT"); markerMap.put("rs4917014", "GT"); markerMap.put("rs4938534", "AG"); markerMap.put("rs4939827", "CT"); markerMap.put("rs4947296", "CT"); markerMap.put("rs495337", "AG"); markerMap.put("rs4973768", "CT"); markerMap.put("rs4977574", "AG"); markerMap.put("rs4977756", "AG"); markerMap.put("rs4979462", "CT"); markerMap.put("rs498872", "AG"); markerMap.put("rs505922", "CT"); markerMap.put("rs5219", "CT"); markerMap.put("rs548234", "CT"); markerMap.put("rs556621", "GT"); markerMap.put("rs557011", "CT"); markerMap.put("rs568408", "AG"); markerMap.put("rs5759167", "GT"); markerMap.put("rs58542926", "CT"); markerMap.put("rs599839", "AG"); markerMap.put("rs6007897", "CT"); markerMap.put("rs6010620", "AG"); markerMap.put("rs6017342", "AC"); markerMap.put("rs6061231", "AC"); markerMap.put("rs610932", "GT"); markerMap.put("rs6265", "CT"); markerMap.put("rs638893", "AG"); markerMap.put("rs6426749", "CG"); markerMap.put("rs6469656", "AG"); markerMap.put("rs6469937", "AG"); markerMap.put("rs647161", "AC"); markerMap.put("rs6478109", "AG"); markerMap.put("rs6495308", "CT"); markerMap.put("rs6495309", "CT"); markerMap.put("rs6498169", "AG"); markerMap.put("rs652888", "AG"); markerMap.put("rs6542095", "CT"); markerMap.put("rs6556416", "AC"); markerMap.put("rs6570507", "AG"); markerMap.put("rs657152", "AC"); markerMap.put("rs6584555", "CT"); markerMap.put("rs660895", "AG"); markerMap.put("rs662799", "AG"); markerMap.put("rs6677604", "AG"); markerMap.put("rs671", "AG"); markerMap.put("rs6733839", "CT"); markerMap.put("rs6733840", "CT"); markerMap.put("rs6774494", "AG"); markerMap.put("rs6795735", "CT"); markerMap.put("rs6832151", "GT"); markerMap.put("rs6841581", "AG"); markerMap.put("rs687621", "AG"); markerMap.put("rs6885224", "CT"); markerMap.put("rs6929137", "AG"); markerMap.put("rs6939340", "AG"); markerMap.put("rs698", "CT"); markerMap.put("rs6983267", "GT"); markerMap.put("rs6983561", "AC"); markerMap.put("rs7004633", "AG"); markerMap.put("rs700651", "AG"); markerMap.put("rs704017", "AG"); markerMap.put("rs7041847", "AG"); markerMap.put("rs705702", "AG"); markerMap.put("rs7086803", "AG"); markerMap.put("rs710521", "CT"); markerMap.put("rs7105934", "AG"); markerMap.put("rs718314", "AG"); markerMap.put("rs7197475", "CT"); markerMap.put("rs7216064", "AG"); markerMap.put("rs7216389", "CT"); markerMap.put("rs7241918", "GT"); markerMap.put("rs7278468", "GT"); markerMap.put("rs73243351", "AG"); markerMap.put("rs7329174", "AG"); markerMap.put("rs7453920", "AG"); markerMap.put("rs751402", "AG"); markerMap.put("rs7521902", "AC"); markerMap.put("rs7524102", "AG"); markerMap.put("rs7574865", "GT"); markerMap.put("rs762551", "AC"); markerMap.put("rs763361", "CT"); markerMap.put("rs7639618", "CT"); markerMap.put("rs76418789", "AG"); markerMap.put("rs7652589", "AG"); markerMap.put("rs7665090", "AG"); markerMap.put("rs7671167", "CT"); markerMap.put("rs7679673", "AC"); markerMap.put("rs7701890", "AG"); markerMap.put("rs7709212", "CT"); markerMap.put("rs7741164", "AG"); markerMap.put("rs7779540", "AG"); markerMap.put("rs780094", "CT"); markerMap.put("rs784288", "AG"); markerMap.put("rs7903491", "AG"); markerMap.put("rs7914558", "AG"); markerMap.put("rs7927894", "CT"); markerMap.put("rs798766", "CT"); markerMap.put("rs8017161", "AG"); markerMap.put("rs8032158", "CT"); markerMap.put("rs8032939", "CT"); markerMap.put("rs8050136", "AC"); markerMap.put("rs8192917", "CT"); markerMap.put("rs823156", "AG"); markerMap.put("rs872071", "AG"); markerMap.put("rs873549", "CT"); markerMap.put("rs878860", "CT"); markerMap.put("rs880315", "CT"); markerMap.put("rs889312", "AC"); markerMap.put("rs907611", "AG"); markerMap.put("rs9268853", "CT"); markerMap.put("rs9271192", "AC"); markerMap.put("rs9271366", "AG"); markerMap.put("rs9271588", "CT"); markerMap.put("rs9275319", "AG"); markerMap.put("rs9275328", "CT"); markerMap.put("rs9275572", "AG"); markerMap.put("rs9277535", "AG"); markerMap.put("rs9296249", "CT"); markerMap.put("rs9298506", "AG"); markerMap.put("rs9303277", "CT"); markerMap.put("rs9303542", "AG"); markerMap.put("rs9344996", "CT"); markerMap.put("rs9355610", "AG"); markerMap.put("rs9357155", "AG"); markerMap.put("rs9357271", "CT"); markerMap.put("rs9390754", "AG"); markerMap.put("rs941823", "CT"); markerMap.put("rs943080", "CT"); markerMap.put("rs944289", "CT"); markerMap.put("rs9510787", "AG"); markerMap.put("rs951266", "AG"); markerMap.put("rs9543325", "CT"); markerMap.put("rs9642880", "GT"); markerMap.put("rs9652490", "AG"); markerMap.put("rs965513", "AG"); markerMap.put("rs966221", "AG"); markerMap.put("rs966423", "CT"); markerMap.put("rs968451", "GT"); markerMap.put("rs974819", "CT"); markerMap.put("rs9820110", "GT"); markerMap.put("rs9841504", "CG"); markerMap.put("rs9870207", "AG"); markerMap.put("rs9895661", "CT"); markerMap.put("rs9921222", "CT"); markerMap.put("rs9925481", "CT"); markerMap.put("rs9939609", "AT");

		Map<String, Integer> snpGeneCountMap = Maps.newHashMap();

		String managerName = "";
		// #. sample에 있는 데이터 값에서 마커별 개수를 정의한다.
		for (Sample sample : samples) {
			Map<String, Object> dataMap = sample.getData();
			
			if (managerName.length() < 1) {
				managerName = sample.getJdgmDrctApproveMember().getName();
			}

			for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
				String snpGeneString = entry.getKey() + "_" + entry.getValue();
				// #. 해당 snp에 유전형이 기존맵에 있으면 카운트 추가
				if (snpGeneCountMap.get(snpGeneString) != null && snpGeneCountMap.get(snpGeneString) > 0) {
					snpGeneCountMap.put(snpGeneString, snpGeneCountMap.get(snpGeneString) + 1);
				} else {
					// #. 해당 snp에 유전형이 기존맵에 없으면 카운트 1
					snpGeneCountMap.put(snpGeneString, 1);
				}
			}
		}

		// logger.info("★★★ snpGeneCountMap=" + snpGeneCountMap.toString());

		// #. marker 별 검체 건수 비율 값 설정
		List<String> snps = new ArrayList<>(markerMap.keySet());

		// #. 결과 List Map 생성
		List<Map<String, String>> geneCountInfoMaps = new ArrayList<Map<String, String>>();
        
		// #. 키 값으로 오름차순 정렬
        Collections.sort(snps);

		for (String snp : snps) {
			String gene = markerMap.get(snp);
			// #. 유전형 경우의 수 조합
			String[] genes = gene.split("");
			String gene1 = genes[0] + genes[0];
			String gene2 = genes[0] + genes[1];
			String gene3 = genes[1] + genes[1];

			String snpGeneString1 = snp + "_" + gene1;
			String snpGeneString2 = snp + "_" + gene2;
			String snpGeneString3 = snp + "_" + gene3;

			
			int snpGene1Count = (snpGeneCountMap.get(snpGeneString1) != null ? snpGeneCountMap.get(snpGeneString1) : 0);
			int snpGene2Count = (snpGeneCountMap.get(snpGeneString2) != null ? snpGeneCountMap.get(snpGeneString2) : 0);
			int snpGene3Count = (snpGeneCountMap.get(snpGeneString3) != null ? snpGeneCountMap.get(snpGeneString3) : 0);
			
			int totalCount = snpGene1Count + snpGene2Count + snpGene3Count;

			Map<String, String> snpGene1Map = Maps.newHashMap();
			Map<String, String> snpGene2Map = Maps.newHashMap();
			Map<String, String> snpGene3Map = Maps.newHashMap();

			// #. 전체 개수가 0이상이라면 백분율 구해서 각 유전형별 비율 구하기
			if (totalCount > 0) {
				double snpGene1Ratio = ((double)snpGene1Count / totalCount) * 100;
				String snpGene1RatioString = String.format("%.2f", snpGene1Ratio) + "%";

				snpGene1Map.put("snp", snp);
				snpGene1Map.put("gene", gene1);
				snpGene1Map.put("count", "" + snpGene1Count);
				snpGene1Map.put("ratio", snpGene1RatioString);

				double snpGene2Ratio = ((double)snpGene2Count / totalCount) * 100;
				String snpGene2RatioString = String.format("%.2f", snpGene2Ratio) + "%";

				snpGene2Map.put("snp", snp);
				snpGene2Map.put("gene", gene2);
				snpGene2Map.put("count", "" + snpGene2Count);
				snpGene2Map.put("ratio", snpGene2RatioString);

				double snpGene3Ratio = ((double)snpGene3Count / totalCount) * 100;
				String snpGene3RatioString = String.format("%.2f", snpGene3Ratio) + "%";

				snpGene3Map.put("snp", snp);
				snpGene3Map.put("gene", gene3);
				snpGene3Map.put("count", "" + snpGene3Count);
				snpGene3Map.put("ratio", snpGene3RatioString);
			} else {
				// #. 전체 개수가 0 이라면 각 유전형별 0 값 넣기
				snpGene1Map.put("snp", snp);
				snpGene1Map.put("gene", gene1);
				snpGene1Map.put("count", "0");
				snpGene1Map.put("ratio", "0.00%");
				
				snpGene2Map.put("snp", snp);
				snpGene2Map.put("gene", gene2);
				snpGene2Map.put("count", "0");
				snpGene2Map.put("ratio", "0.00%");

				snpGene3Map.put("snp", snp);
				snpGene3Map.put("gene", gene3);
				snpGene3Map.put("count", "0");
				snpGene3Map.put("ratio", "0.00%");
			}

			// #. 최종 목록에 map 넣기
			geneCountInfoMaps.add(snpGene1Map);
			geneCountInfoMaps.add(snpGene2Map);
			geneCountInfoMaps.add(snpGene3Map);

			// logger.info("★★★ snpGene1Map=" + snpGene1Map.toString());
			// logger.info("★★★ snpGene2Map=" + snpGene2Map.toString());
			// logger.info("★★★ snpGene3Map=" + snpGene3Map.toString());
		}

		// #. 기본 엑셀 양식 만들기
		// #. sheet 생성
		XSSFSheet sheet = wb.createSheet("sheet");

		DecimalFormat decimalFormat = new DecimalFormat("###,###");
		String totalCountString = decimalFormat.format(samples.size());
		String dateRangeString = "";
		if (params.containsKey("sDate") && params.containsKey("fDate")) {
			if (params.get("sDate").length() > 0 && params.get("fDate").length() > 0) {
				dateRangeString = params.get("sDate") + " ~ " + params.get("fDate");
			}
		}

		this.createTestResultStatisticsTable(wb, sheet, geneCountInfoMaps, managerName, totalCountString, dateRangeString);

		return wb;
	}

	// ################################## private ##################################

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
		sheet.setColumnWidth(10, 200 * 32);
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
		sheet.setColumnWidth(10, 200 * 32);
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

	/**
	 * 검사 결과 통계표 엑셀 테이블 생성
	 * @param wb
	 * @param sheet
	 */
	private void createTestResultStatisticsTable(XSSFWorkbook wb, XSSFSheet sheet, List<Map<String, String>> geneCountInfoMaps, String managerName, String totalSampleCountString, String dateRangeString) {
		// #. style 셋팅
		Font font11 = wb.createFont();
		font11.setFontHeightInPoints((short)11);
		font11.setFontName("맑은 고딕");

		Font font11Bold = wb.createFont();
		font11Bold.setFontHeightInPoints((short)10);
		font11Bold.setBold(true);
		font11Bold.setFontName("맑은 고딕");

		Font font20Bold = wb.createFont();
		font20Bold.setFontHeightInPoints((short)20);
		font20Bold.setBold(true);
		font20Bold.setFontName("맑은 고딕");

		CellStyle allBorderFont11Style = wb.createCellStyle();
		allBorderFont11Style.setFont(font11Bold);
		allBorderFont11Style.setAlignment(HorizontalAlignment.CENTER);
		allBorderFont11Style.setVerticalAlignment(VerticalAlignment.CENTER);
		allBorderFont11Style.setBorderRight(BorderStyle.THIN);
		allBorderFont11Style.setBorderBottom(BorderStyle.THIN);
		allBorderFont11Style.setBorderLeft(BorderStyle.THIN);
		allBorderFont11Style.setBorderTop(BorderStyle.THIN);

		CellStyle topLeftRightBorderFont11BoldStyle = wb.createCellStyle();
		topLeftRightBorderFont11BoldStyle.setFont(font11Bold);
		topLeftRightBorderFont11BoldStyle.setAlignment(HorizontalAlignment.CENTER);
		topLeftRightBorderFont11BoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		topLeftRightBorderFont11BoldStyle.setBorderRight(BorderStyle.THIN);
		topLeftRightBorderFont11BoldStyle.setBorderLeft(BorderStyle.THIN);
		topLeftRightBorderFont11BoldStyle.setBorderTop(BorderStyle.THIN);

		CellStyle bottomLeftRightBorderFont11Style = wb.createCellStyle();
		bottomLeftRightBorderFont11Style.setFont(font11);
		bottomLeftRightBorderFont11Style.setAlignment(HorizontalAlignment.CENTER);
		bottomLeftRightBorderFont11Style.setVerticalAlignment(VerticalAlignment.CENTER);
		bottomLeftRightBorderFont11Style.setBorderRight(BorderStyle.THIN);
		bottomLeftRightBorderFont11Style.setBorderBottom(BorderStyle.THIN);
		bottomLeftRightBorderFont11Style.setBorderLeft(BorderStyle.THIN);

		CellStyle leftRightBorderFont11Style = wb.createCellStyle();
		leftRightBorderFont11Style.setFont(font11);
		leftRightBorderFont11Style.setAlignment(HorizontalAlignment.CENTER);
		leftRightBorderFont11Style.setVerticalAlignment(VerticalAlignment.CENTER);
		leftRightBorderFont11Style.setBorderRight(BorderStyle.THIN);
		leftRightBorderFont11Style.setBorderLeft(BorderStyle.THIN);

		CellStyle allBorderFont11BoldStyle = wb.createCellStyle();
		allBorderFont11BoldStyle.setFont(font11Bold);
		allBorderFont11BoldStyle.setAlignment(HorizontalAlignment.CENTER);
		allBorderFont11BoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		allBorderFont11BoldStyle.setBorderRight(BorderStyle.THIN);
		allBorderFont11BoldStyle.setBorderBottom(BorderStyle.THIN);
		allBorderFont11BoldStyle.setBorderLeft(BorderStyle.THIN);
		allBorderFont11BoldStyle.setBorderTop(BorderStyle.THIN);

		CellStyle allBorderFont11BoldGrayBackgroundStyle = wb.createCellStyle();
		allBorderFont11BoldGrayBackgroundStyle.setFont(font11Bold);
		allBorderFont11BoldGrayBackgroundStyle.setAlignment(HorizontalAlignment.CENTER);
		allBorderFont11BoldGrayBackgroundStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		allBorderFont11BoldGrayBackgroundStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		allBorderFont11BoldGrayBackgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		allBorderFont11BoldGrayBackgroundStyle.setBorderRight(BorderStyle.THIN);
		allBorderFont11BoldGrayBackgroundStyle.setBorderBottom(BorderStyle.THIN);
		allBorderFont11BoldGrayBackgroundStyle.setBorderLeft(BorderStyle.THIN);
		allBorderFont11BoldGrayBackgroundStyle.setBorderTop(BorderStyle.THIN);

		CellStyle font20BoldStyle = wb.createCellStyle();
		font20BoldStyle.setFont(font20Bold);
		font20BoldStyle.setAlignment(HorizontalAlignment.CENTER);
		font20BoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		// #. Cell width height 조정
		sheet.setColumnWidth(0, 14 * 32);
		sheet.setColumnWidth(1, 100 * 32);
		sheet.setColumnWidth(2, 100 * 32);
		sheet.setColumnWidth(3, 100 * 32);
		sheet.setColumnWidth(4, 100 * 32);
		sheet.setColumnWidth(5, 30 * 32);
		sheet.setColumnWidth(6, 100 * 32);
		sheet.setColumnWidth(7, 100 * 32);
		sheet.setColumnWidth(8, 100 * 32);
		sheet.setColumnWidth(9, 100 * 32);
		sheet.setColumnWidth(10, 14 * 32);

		sheet.createRow(0).setHeight((short)(6 * 30));
		sheet.createRow(2).setHeight((short)(29 * 30));

		XSSFRow row1 = sheet.createRow(1);

		row1.createCell(1).setCellValue("검사 결과 통계표");
		row1.createCell(9).setCellValue("승인");

		// #. Style
		row1.getCell(1).setCellStyle(font20BoldStyle);
		row1.getCell(9).setCellStyle(allBorderFont11BoldStyle);

		XSSFRow row2 = sheet.getRow(2);
		row2.createCell(9).setCellValue(managerName); // 검체중 첫번째에 최종 승인 계정이름

		row2.getCell(9).setCellStyle(allBorderFont11BoldStyle);

		XSSFRow row4 = sheet.createRow(4);
		
		row4.createCell(1).setCellValue("총 검 체 건 수");
		row4.createCell(3).setCellValue(totalSampleCountString);
		row4.createCell(6).setCellValue("검 사 기 간");
		row4.createCell(8).setCellValue(dateRangeString);

		// #. Style
		row4.getCell(1).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row4.getCell(3).setCellStyle(allBorderFont11BoldStyle);
		row4.getCell(6).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row4.getCell(8).setCellStyle(allBorderFont11BoldStyle);

		row4.createCell(2).setCellStyle(allBorderFont11BoldStyle);
		row4.createCell(4).setCellStyle(allBorderFont11BoldStyle);
		row4.createCell(7).setCellStyle(allBorderFont11BoldStyle);
		row4.createCell(9).setCellStyle(allBorderFont11BoldStyle);

		row4.setHeight((short)(29 * 15));

		XSSFRow row6 = sheet.createRow(6);
		row6.createCell(1).setCellValue("SNP");
		row6.createCell(2).setCellValue("유전형");
		row6.createCell(3).setCellValue("검체 건수");
		row6.createCell(4).setCellValue("비율(%)");
		row6.createCell(6).setCellValue("SNP");
		row6.createCell(7).setCellValue("유전형");
		row6.createCell(8).setCellValue("검체 건수");
		row6.createCell(9).setCellValue("비율(%)");

		// #. Style
		row6.getCell(1).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row6.getCell(2).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row6.getCell(3).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row6.getCell(4).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row6.getCell(6).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row6.getCell(7).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row6.getCell(8).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);
		row6.getCell(9).setCellStyle(allBorderFont11BoldGrayBackgroundStyle);

		row6.setHeight((short)(29 * 15));

		// #. geneCountInfoMaps 정보를 가지고 각 라인은 만들기
		if (geneCountInfoMaps != null && geneCountInfoMaps.size() > 0) {
			// #. 총 개수에 절반을 구하기
			int halfIndex = (int)Math.round(((double)geneCountInfoMaps.size() / 2));

			for (int i = 0; i < geneCountInfoMaps.size(); i++) {
				Map<String, String> geneCountInfoMap = geneCountInfoMaps.get(i);
				
				// #. index 값이 전체 수에 절반 미만이면 1,2,3,4에 값 셋팅
				if (i < halfIndex) {
					XSSFRow row = sheet.createRow(7 + i);
					row.createCell(1);
					// #. index 값을 3으로 나눈 나머지가 0이면 값을 설정
					if (i % 3 == 0) {
						row.getCell(1).setCellValue(geneCountInfoMap.get("snp"));
					}

					row.createCell(2).setCellValue(geneCountInfoMap.get("gene"));
					row.createCell(3).setCellValue(geneCountInfoMap.get("count"));
					row.createCell(4).setCellValue(geneCountInfoMap.get("ratio"));

					if (i % 3 == 0) {
						// #. index를 나눈 나머지가 0이면 snp 자리에 topLeftRightBorderFont11BoldStyle 스타일 적용
						row.getCell(1).setCellStyle(topLeftRightBorderFont11BoldStyle);
					} else if (i % 3 == 1) {
						// #. index를 나눈 나머지가 1이면 snp 자리에 leftRightBorderFont11Style 스타일 적용
						row.getCell(1).setCellStyle(leftRightBorderFont11Style);
					} else if (i % 3 == 2) {
						// #. index를 나눈 나머지가 2이면 snp 자리에 bottomLeftRightBorderFont11Style 스타일 적용
						row.getCell(1).setCellStyle(bottomLeftRightBorderFont11Style);
					}

					row.getCell(2).setCellStyle(allBorderFont11Style);
					row.getCell(3).setCellStyle(allBorderFont11Style);
					row.getCell(4).setCellStyle(allBorderFont11Style);

				} else {
					XSSFRow row = sheet.getRow(7 + i - halfIndex);
					// #. index 값이 전체 수에 절반 이상이면 6,7,8,9에 값 셋팅
					row.createCell(6);
					// #. index 값을 3으로 나눈 나머지가 0이면 값을 설정
					if (i % 3 == 0) {
						row.getCell(6).setCellValue(geneCountInfoMap.get("snp"));
					}

					row.createCell(7).setCellValue(geneCountInfoMap.get("gene"));
					row.createCell(8).setCellValue(geneCountInfoMap.get("count"));
					row.createCell(9).setCellValue(geneCountInfoMap.get("ratio"));

					if (i % 3 == 0) {
						// #. index를 나눈 나머지가 0이면 snp 자리에 topLeftRightBorderFont11BoldStyle 스타일 적용
						row.getCell(6).setCellStyle(topLeftRightBorderFont11BoldStyle);
					} else if (i % 3 == 1) {
						// #. index를 나눈 나머지가 1이면 snp 자리에 leftRightBorderFont11Style 스타일 적용
						row.getCell(6).setCellStyle(leftRightBorderFont11Style);
					} else if (i % 3 == 2) {
						// #. index를 나눈 나머지가 2이면 snp 자리에 bottomLeftRightBorderFont11Style 스타일 적용
						row.getCell(6).setCellStyle(bottomLeftRightBorderFont11Style);
					}

					row.getCell(7).setCellStyle(allBorderFont11Style);
					row.getCell(8).setCellStyle(allBorderFont11Style);
					row.getCell(9).setCellStyle(allBorderFont11Style);

					row.setHeight((short)(29 * 15));
				}
			}
		}
		
		// #. cel 병합
		sheet.addMergedRegion(CellRangeAddress.valueOf("B2:H3"));
		sheet.addMergedRegion(CellRangeAddress.valueOf("B5:C5"));
		sheet.addMergedRegion(CellRangeAddress.valueOf("D5:E5"));
		sheet.addMergedRegion(CellRangeAddress.valueOf("G5:H5"));
		sheet.addMergedRegion(CellRangeAddress.valueOf("I5:J5"));

	}

	/**
	 * 폐기일은 출고완료일에 영업일 기준으로 +7일을 한다
	 * @param outputCmplDate
	 * @return
	 */
	private String getDiscardDate(LocalDateTime outputCmplDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate start = outputCmplDate.toLocalDate();
        LocalDate end = start.plusYears(1);

        List<Holiday> holidays = holidayRepository.findByDateBetween(start.format(yyyymmdd), end.format(yyyymmdd));
        List<String> hdays = holidays.stream().map(h -> { return h.getDate(); }).collect(Collectors.toList());

        LocalDate temp;
        int max = 7;
        
        for (int i = 0; i <= max; i++) {
            temp = start.plusDays(i);
            if (hdays.contains(temp.format(yyyymmdd)) || isWeekend(temp)) {
                max++;
            }
        }

        String rtn = start.plusDays(max).format(formatter);
        //items.put("tat", rtn);
        return rtn;
        
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
