package com.clinomics.service.async;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.ChipTypeCode;
import com.clinomics.enums.MountWorkerCode;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.util.FileUtil;
import com.google.common.io.Files;

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

		// #. Cel File 서버로 가져오기
		this.copyCelFiles(samples);

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
			commandsSb.append(chipTypeCode.getProgram() + " ");
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

	@Async
	public void doPythonReAnalysis(String chipBarcode, ChipTypeCode chipTypeCode, String analysisPath) {

		// #. 명령어 실행
		FileOutputStream textFileOs = null;
		FileOutputStream excelFileOs = null;
		try {
			// #. 명령어 파일 경로로 명령어 파일 생성. 직접 실행시 pipe 명령어(|) 수행 불가
			String shellExt = ".sh";
			if (OS.indexOf("win") >= 0) {
				shellExt = ".bat";
			}
			StringBuilder commandsSb = new StringBuilder();
			commandsSb.append(chipTypeCode.getReProgram() + " ");
			commandsSb.append(chipTypeCode.getReCmd() + " ");
			commandsSb.append(chipBarcode); // chip barcode parameter
			
			logger.info("reanalysis execute cmd=" + commandsSb.toString());
			if (chipTypeCode.getReProgram().length() > 0 && chipTypeCode.getReCmd().length() > 0) {
				
				String commandFilePath = analysisPath + "/re_" + chipBarcode + shellExt;
				BufferedWriter fw = new BufferedWriter(new FileWriter(commandFilePath));
				fw.write(commandsSb.toString());
				fw.close();
		
				logger.info("reanalysis commands shell file path=" + commandFilePath);
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
				
				logger.info(">> reanalysis standardErrorSb=" + standardErrorSb.toString());
				
				process.destroy();

			}

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
	 * mount된 경로에서 파일을 복사한다.
	 * @param samples
	 */
	private boolean copyCelFiles(List<Sample> samples) {
		boolean isCompleteCopy = false;
		logger.info("★★★★★★★★★★ Start copyCelFiles");
		for (Sample sample : samples) {
			File sourceFile = null;

			// #. 마운트 장비에서 해당 샘플에 cel 파일이 존재하는지 확인
			logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "]fileName=" + sample.getFileName());
			for (MountWorkerCode code : MountWorkerCode.values()) {
				File dir = new File(code.getValue());
				logger.info("★★★ [" + code.getValue() + "] dir=" + dir);
				logger.info("★★★ [" + code.getValue() + "] dir.list()=" + dir.list());
				if (dir.list() != null) {
					logger.info("★★★ [" + code.getValue() + "] dir.list().length=" + dir.list().length);
					// #. 파일이 존재한다면 sourceFile에 셋팅
					if (Arrays.asList(dir.list()).contains(sample.getFileName())) {
						sourceFile = new File(code.getValue(), sample.getFileName());
						break;
					}
				}
			}

			// #. 파일이 존재한다면 카피 진행
			if (sourceFile != null) {
				logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "] sourceFile size=[" + sourceFile.length() + " byte]");
				File copyFile = new File(sample.getFilePath(), sample.getFileName());

				try {
					Files.copy(sourceFile, copyFile);
					// #. file 복사 확인
					logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "] copyFile size=[" + copyFile.length() + " byte]");
					if (Files.asByteSource(sourceFile).contentEquals(Files.asByteSource(copyFile))) {
						isCompleteCopy = true;
						sample.setCheckCelFile("PASS");
						sampleRepository.save(sample);
						logger.info("★★★★★★★ [" + sample.getLaboratoryId() + "] success copy");
					} else {
						// #. 파일 복사가 잘못된 경우 
						logger.info("★★★★★★★ There was a problem copying the file=" + sample.getLaboratoryId());
						sample.setCheckCelFile("FAIL");
						sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
						sample.setStatusMessage("There was a problem copying the file.");
						sampleRepository.save(sample);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// #. 파일이 없는경우 
				logger.info("★★★★★★★ Not Found File=" + sample.getLaboratoryId());
				sample.setCheckCelFile("FAIL");
				sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
				sample.setStatusMessage("Not Found File.");
				sampleRepository.save(sample);
			}
		}
		logger.info("★★★★★★★★★★ finish copyCelFiles");
		return isCompleteCopy;
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
