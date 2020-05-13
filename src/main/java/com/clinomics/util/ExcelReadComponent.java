package com.clinomics.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Maps;

@Component
public class ExcelReadComponent {
	
	@Value("${lims.filePath}")
	private String bioFilePath;

	public XSSFWorkbook readWorkbook(MultipartFile multipartFile) throws IOException, InvalidFormatException {
		verifyFileExtension(multipartFile.getOriginalFilename());
		XSSFWorkbook workbook = multipartFileToWorkbook(multipartFile);
		
		return workbook;
	}
	
	public XSSFWorkbook readWorkbook(File file) throws IOException, InvalidFormatException {
		verifyFileExtension(file.getName());
		XSSFWorkbook workbook = multipartFileToWorkbook(file);
		
		return workbook;
	}
	
	private XSSFWorkbook multipartFileToWorkbook(MultipartFile multipartFile) throws IOException {
		
		return new XSSFWorkbook(multipartFile.getInputStream());
	}
	
	private XSSFWorkbook multipartFileToWorkbook(File file) {
		XSSFWorkbook workbook = null;
		try {
			workbook = new XSSFWorkbook(new FileInputStream(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return workbook;
	}

	private void verifyFileExtension(String fileName) throws InvalidFormatException {
		if (!isExcelExtension(fileName)) {
			throw new InvalidFormatException("This file extension is not verify");
		}
	}

	private boolean isExcelExtension(String fileName) {
		return fileName.toLowerCase().endsWith("xls") || fileName.toLowerCase().endsWith("xlsx");
	}
	
	public List<Map<String, Object>> readMapFromSheet(XSSFSheet sheet) {
		List<Map<String, Object>> list = new ArrayList<>();
		// sheet에서 유효한(데이터가 있는) 행의 개수를 가져온다.
		int numOfRows = sheet.getPhysicalNumberOfRows();
		int numOfCells = 0;
		String cellName = "";
		XSSFRow headRow = sheet.getRow(0);
		// 로우수 만큼 반복 (옵션값이 지정한 행부터 시작)
		for (int rowIndex = 1; rowIndex < numOfRows; rowIndex++) {

			// 하나의 로우를 가져온다
			XSSFRow row = sheet.getRow(rowIndex);
			if (row != null) {
				// 각 로우마다 데이터를 저장할 객체
				Map<String, Object> map = Maps.newHashMap();
				// 가져온 로우의 셀개수
				numOfCells = row.getLastCellNum();

				// 셀의 갯수 만큼 map에 데이터 저장
				String emptyCheck = "";
				for (int cellIndex = 0; cellIndex < numOfCells; cellIndex++) {
					// 셀가져오기
					XSSFCell headCell = headRow.getCell(cellIndex);
					XSSFCell cell = row.getCell(cellIndex);
					// 현제 셀의 이름 가져오기
					//int celNum = (cell != null) ? cell.getColumnIndex() : cellIndex;
					cellName = headCell.getStringCellValue();
					//cellName = CellReference.convertNumToColString(celNum);
					
					// 현제 셀의 값 가져오기
					String value = "";
					if (cell == null) {
						value = "";
					} else {
						if (cell.getCellTypeEnum() == CellType.FORMULA) { // 수식처리한 셀(A1+B1)을 가져올 경우
							value = cell.getCellFormula();
						} else if (cell.getCellTypeEnum() == CellType.NUMERIC) { // 숫자형의 경우 ( 기본:double)
							value = cell.getNumericCellValue() + ""; // int로 반환
						} else if (cell.getCellTypeEnum() == CellType.STRING) { // 문자형값
							value = cell.getStringCellValue().trim();
						} else if (cell.getCellTypeEnum() == CellType.BOOLEAN) { // BOOLEAN형
							value = cell.getBooleanCellValue() + "";
						} else if (cell.getCellTypeEnum() == CellType.ERROR) { // 에러바이트를 출력
							value = cell.getErrorCellValue() + "";
						} else if (cell.getCellTypeEnum() == CellType.BLANK) { // 값이 없을 경우
							value = "";
						} else {
							value = cell.getStringCellValue().trim();
						}
					}
					emptyCheck += value;
					map.put(cellName, value);
				} // 셀의 갯수만큼 for문
				
				// if row data is empty > not insert
				if (!emptyCheck.isEmpty()) {
					list.add(map);
				}


			}
		} // 로우 갯수만큼 for문
		return list;
	}
	
	public void saveWorkbook(XSSFWorkbook wb, String fileName) {
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(bioFilePath + "/" + fileName);
			wb.write(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
