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
import java.util.stream.Collectors;

import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ChipTypeCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.util.FileUtil;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	@Value("${lims.celFilePath}")
	private String celFilePath;
	
	@Value("${titan.ftp.address}")
	private String ftpAddress;
	
	@Value("${titan.ftp.port}")
	private int ftpPort;

	@Value("${titan.ftp.username}")
	private String ftpUsername;

	@Value("${titan.ftp.password}")
	private String ftpPassword;

	@Autowired
	SampleRepository sampleRepository;
	
	@Async
	public void doPythonAnalysis(List<Sample> samples) {
		ChipTypeCode chipTypeCode = samples.get(0).getChipTypeCode();
		String chipBarcode = samples.get(0).getChipBarcode();
		String analysisPath = samples.get(0).getFilePath();

		// #. Cell File 서버로 가져오기
		this.downloadCelFiles(samples);

		// #. 명령어 실행
		FileOutputStream textFileOs = null;
		FileOutputStream excelFileOs = null;
		String textFilePath = analysisPath + "/" + chipBarcode + ".txt";
		String excelFilePath = analysisPath + "/" + chipBarcode + ".xlsx";
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
			
			XSSFWorkbook wb = getAnalysisExcel(samples);
			excelFileOs = new FileOutputStream(excelFilePath);
			wb.write(excelFileOs);
			
			// #. text 파일에 내용 추가
			textFileOs.write(textFileSb.toString().getBytes());
			
			StringBuilder commandsSb = new StringBuilder();
			commandsSb.append("python ");
			commandsSb.append(chipTypeCode.getCmd() + " ");
			commandsSb.append(textFilePath + " "); // text 파일 경로 지정
			commandsSb.append(analysisPath + " "); // 작업공간 경로 지정
			commandsSb.append(excelFilePath); // mappingFile 경로 지정
	
			logger.info("execute cmd=" + commandsSb.toString());
			
			// #. 명령어 파일 경로로 명령어 파일 생성. 직접 실행시 pipe 명령어(|) 수행 불가
			String shellExt = ".sh";
			if (OS.indexOf("win") >= 0) {
				shellExt = ".bat";
	        }
			
			String commandFilePath = analysisPath + "/" + chipBarcode + shellExt;
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (textFileOs != null) textFileOs.close();
				if (excelFileOs != null) excelFileOs.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
    }
	
	private XSSFWorkbook getAnalysisExcel(List<Sample> samples) {
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
		cell2.setCellValue("Genotyping Id");
		
		int index = 1;
		for (Sample sample : samples) {
			XSSFRow rr = sheet.createRow(index++);
			XSSFCell c1 = rr.createCell(0);
			XSSFCell c2 = rr.createCell(1);
			c1.setCellValue(sample.getFileName());
			c2.setCellValue(sample.getGenotypingId());
		}
		
		sheet.setColumnWidth(0, 3000);
		
		return workbook;
	}

	/**
	 * ftp(진타이탄장비)에서 cel file 목록을 다운로드 하기
	 * @param samples
	 */
	private void downloadCelFiles(List<Sample> samples) {
		logger.info("★★★★★★★★★★ Start downloadCelFiles");
		FTPClient ftp = null;
		try {
			ftp = new FTPClient();
			ftp.setControlEncoding("UTF-8");

			ftp.connect(ftpAddress, ftpPort);
			ftp.login(ftpUsername, ftpPassword);

			ftp.setBufferSize(1024 * 1024);
			ftp.enterLocalPassiveMode();

			logger.info("★★★★★★★★★★★★ ftp.listNames()=" + ftp.listNames().length);
			for (Sample sample : samples) {
				boolean existFile = false;
				for (String fileName : ftp.listNames()) {
					if (sample.getFileName().equals(fileName)) {
						logger.info("★★★★★★★ fileName=" + fileName);
						File f = new File(sample.getFilePath(), fileName);
	
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(f);
							boolean isSuccess = ftp.retrieveFile(fileName, fos);
							logger.info("★★★★★★★ isSuccess=" + isSuccess);
							if (isSuccess) {
								sample.setCheckCelFile("PASS");
								sampleRepository.save(sample);
								existFile = true;
								// 다운로드 성공
								logger.info("★★★★★★★ successed file=" + fileName);
							} else {
								// 다운로드 실패
								logger.info("★★★★★★★ failed file=" + fileName);
							}
						} catch (IOException ex) {
							logger.info("★★★★★★★★★★ ex:" + ex.getMessage());
							ex.printStackTrace();
						} finally {
							logger.info("★★★★★★★★★★ download finally");
							if (fos != null) {
								try {
									fos.close();
								} catch (IOException ex) {
									ex.printStackTrace();
								}
							}
						}
						break;
					}
				}

				if (!existFile) {
					logger.info("★★★★★★★ Not exist File=" + sample.getLaboratoryId());
					sample.setCheckCelFile("FAIL");
					sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
					sampleRepository.save(sample);
				}
			}
			ftp.logout();
		} catch (IOException e) {
			logger.info("★★★★★★★★★★ IO:" + e.getMessage());
			e.printStackTrace();
		} finally {
			logger.info("★★★★★★★★★★ End downloadCelFiles");
			if (ftp != null && ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
