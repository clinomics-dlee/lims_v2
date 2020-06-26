package com.clinomics.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.text.DateFormatter;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Member;
import com.clinomics.entity.lims.Product;
import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleItem;
import com.clinomics.enums.ResultCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.BundleRepository;
import com.clinomics.repository.lims.MemberRepository;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.util.CustomIndexPublisher;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;

@Service
public class InputExcelService {

	@Autowired
	SampleRepository sampleRepository;

	@Autowired
	BundleRepository bundleRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	VariousFieldsService variousDayService;

	@Autowired
	ExcelReadComponent excelReadComponent;

	@Autowired
	CustomIndexPublisher customIndexPublisher;
	
	public XSSFWorkbook exportExcelForm(Map<String, String> params) {
		String id = "";
		if (params.containsKey("bundleId")) {
			id = params.get("bundleId");
		}
		Optional<Bundle> oBundle = bundleRepository.findById(NumberUtils.toInt(id));
		Bundle bundle = oBundle.orElse(new Bundle());
		
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		bundle.getProduct().stream().forEach(p -> {
			
			Optional<Product> oProduct = productRepository.findById(p.getId());
			Product product = oProduct.orElse(new Product());
			
			sampleItems.addAll(product.getSampleItem());
			
		});
		
		List<SampleItem> sortedSampleItems = sampleItems.stream()
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		CellStyle pink = workbook.createCellStyle();
		pink.setFillForegroundColor(HSSFColorPredefined.ROSE.getIndex());
		pink.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		XSSFSheet sheet = workbook.createSheet("sample");

		XSSFRow row1 = sheet.createRow(0);
		XSSFRow row2 = sheet.createRow(1);

		int col = 0;
		
		for (SampleItem s : sortedSampleItems) {
			if (!("barcode".equals(s.getNameCode()) && bundle.isAutoBarcode()
				|| "laboratory".equals(s.getNameCode()) && bundle.isAutoSequence())) {
				
				XSSFCell cell1 = row1.createCell(col);
				XSSFCell cell2 = row2.createCell(col);
				if (s.isNotNull()) {
					cell1.setCellStyle(pink);
				}
				cell1.setCellValue(s.getName());
				cell2.setCellValue(s.getExampleValue());
				
				int width = NumberUtils.toInt(s.getWidth());
				sheet.setColumnWidth(col, ((width == 0) ? 3000 : width));
				
				col++;
			}
		}
		
		return workbook;
	}
	
	public Map<String, Object> importExcelSample(MultipartFile multipartFile, String bundleId, String memberId) {
		
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
		
		Optional<Bundle> oBundle = bundleRepository.findById(NumberUtils.toInt(bundleId));
		Bundle bundle = oBundle.orElse(new Bundle());
		
		Set<SampleItem> sampleItems = new HashSet<SampleItem>();
		bundle.getProduct().stream().forEach(p -> {
			
			Optional<Product> oProduct = productRepository.findById(p.getId());
			Product product = oProduct.orElse(new Product());
			
			sampleItems.addAll(product.getSampleItem());
			
		});
		
		List<SampleItem> sortedSampleItems = sampleItems.stream()
				.sorted(Comparator.comparing(SampleItem::getOrd))
				.collect(Collectors.toList());
		
		int sheetNum = workbook.getNumberOfSheets();
		if (sheetNum < 1) {
			return rtn;
		}
		
		Map<String, Object> sampleItem = Maps.newHashMap();
		List<Sample> items = new ArrayList<Sample>();
		for (Map<String, Object> sht : sheetList) {
			Sample sampleTemp = new Sample();
			for (SampleItem itm : sortedSampleItems) {
				String name = itm.getName();
//				if (sht.containsKey(name)) {
				sht.put(itm.getNameCode(), sht.get(name));
				
				if (!name.equals(itm.getNameCode())) {
					sht.remove(name);
				}
				sampleItem = sht;
//				}
			}
			sampleTemp.setBundle(bundle);
			
			variousDayService.setFields(false, sampleTemp, sampleItem);
			
			
			Map<String, Object> newItems = Maps.newHashMap();
			newItems.putAll(sampleItem);
			sampleTemp.setItems(newItems);

			sampleTemp.setCreatedMember(member);
			sampleTemp.setStatusCode(StatusCode.S000_INPUT_REG);
				
			items.add(sampleTemp);
		}
		
		sampleRepository.saveAll(items);
		
		rtn.put("result", ResultCode.SUCCESS.get());
		return rtn;
	}
	
}
