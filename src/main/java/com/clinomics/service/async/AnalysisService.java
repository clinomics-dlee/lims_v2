package com.clinomics.service.async;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.clinomics.util.FileUtil;

@Service
public class AnalysisService {
	
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	@Value("${lims.filePath}")
	private String bioFilePath;
	
	@Value("${lims.workspacePath}")
	private String workspacePath;
	
	@Value("${lims.celFilePath}")
	private String celFilePath;
	
	@Value("${lims.apmraChipPath}")
	private String apmraChipPath;
	
	@Value("${lims.customChipPath}")
	private String customChipPath;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Async
	public void doPythonAnalysis(String chipType, String chipNumber, String analysisPath, List<Map<String, String>> infos) {
		// #. 명령어 실행
		FileOutputStream textFileOs = null;
		FileOutputStream excelFile = null;
//		String analysisPath = workspacePath + "/" + chipNumber;
		String textFilePath = analysisPath + "/" + chipNumber + ".txt";
		String excelFilePath = analysisPath + "/" + chipNumber + ".xlsx";
		try {
			
			textFileOs = new FileOutputStream(textFilePath);
			StringBuilder textFileSb = new StringBuilder();
			textFileSb.append("cel_files");
			// #. file 넣기
			List<String> files = FileUtil.getFileList(analysisPath);
			
			files = files.stream().filter(f -> f.toLowerCase().endsWith("cel")).collect(Collectors.toList());
			
			files.forEach(c -> {
				textFileSb.append("\r\n" + analysisPath + "/" + c);
			});
			
			XSSFWorkbook wb = getAnalysisExcel(infos);
			excelFile = new FileOutputStream(excelFilePath);
			wb.write(excelFile);
			
			// #. text 파일에 내용 추가
			textFileOs.write(textFileSb.toString().getBytes());
			
			StringBuilder commandsSb = new StringBuilder();
			commandsSb.append("python ");
			commandsSb.append(("customChip".equals(chipType) ? customChipPath : apmraChipPath) + " ");
			commandsSb.append(textFilePath + " "); // text 파일 경로 지정
			commandsSb.append(analysisPath + " "); // 작업공간 경로 지정
			commandsSb.append(excelFilePath); // mappingFile 경로 지정
	
			logger.info("execute cmd=" + commandsSb.toString());
			
			// #. 명령어 파일 경로로 명령어 파일 생성. 직접 실행시 pipe 명령어(|) 수행 불가
			String shellExt = ".sh";
			if (OS.indexOf("win") >= 0) {
				shellExt = ".bat";
	        }
			
			String commandFilePath = analysisPath + "/" + chipNumber + shellExt;
			BufferedWriter fw = new BufferedWriter(new FileWriter(commandFilePath));
			fw.write(commandsSb.toString());
			fw.close();
	
			logger.info("commands shell file path=" + commandFilePath);
			File shFile = new File(commandFilePath);
			if (!shFile.canExecute()) shFile.setExecutable(true); // 실행권한
			if (!shFile.canWrite()) shFile.setWritable(true); // 쓰기권한
	
			List<String> commands = new ArrayList<String>();
			commands.add(commandFilePath);
			
			ProcessBuilder processBuilder = new ProcessBuilder(commands);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			
			// #. 명령어 실행 표준 및 오류 처리
			BufferedReader standardErrorBr = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder standardErrorSb = new StringBuilder();
			String lineString = null;
			while ((lineString = standardErrorBr.readLine()) != null) {
				standardErrorSb.append(lineString);
				standardErrorSb.append("<br>");
			}
			
			logger.info(">> standardErrorSb=" + standardErrorSb.toString());
			
			process.destroy();
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
    }
	
	private XSSFWorkbook getAnalysisExcel(List<Map<String, String>> infos) {
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		CellStyle pink = workbook.createCellStyle();
		pink.setFillForegroundColor(HSSFColorPredefined.ROSE.getIndex());
		pink.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		XSSFSheet sheet = workbook.createSheet("mapping");

		XSSFRow row1 = sheet.createRow(0);

		XSSFCell cell1 = row1.createCell(0);
		XSSFCell cell2 = row1.createCell(1);
		cell1.setCellStyle(pink);
		cell2.setCellStyle(pink);
		cell1.setCellValue("Cel File Name");
		cell2.setCellValue("Sample Id");
		
		int index = 1;
		for (Map<String, String> info : infos) {
			XSSFRow rr = sheet.createRow(index++);
			XSSFCell c1 = rr.createCell(0);
			XSSFCell c2 = rr.createCell(1);
			c1.setCellValue(info.get("fileName"));
			c2.setCellValue(info.get("id"));
		}
		
		sheet.setColumnWidth(0, 3000);
		
		return workbook;
	}
}
