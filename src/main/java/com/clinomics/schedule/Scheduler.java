package com.clinomics.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.service.SampleDbService;
import com.clinomics.service.async.AnalysisService;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.EmailSender;
import com.google.common.collect.Maps;

@Component
public class Scheduler {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Value("${genoDataApi.url}")
	private String genoDataApiUrl;

	@Value("${genoDataApi.tokenName}")
	private String genoDataApiTokenName;

	@Value("${genoDataApi.token}")
	private String genoDataApiToken;

	@Value("${lims.managerEmail}")
	private String managerEmail;

	@Value("${lims.serviceManagerEmail}")
	private String serviceManagerEmail;

	@Autowired
	SampleRepository sampleRepository;
	
	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	SampleDbService sampleDbService;

    @Autowired
	AnalysisService analysisService;

	@Autowired
	EmailSender emailSender;
	
	@Scheduled(cron = "10 * * * * *")
	public void run() {
	}
	
	
	/**
	 * Chip Analysis 완료 스케쥴러
	 * 스케쥴 1 분마다 (60초 = 1000 * 60)
	 */
	@Transactional
	@Scheduled(fixedDelay = 1000 * 60)
	public void completeChipAnalysis() {
		try {
			Calendar cal = Calendar.getInstance();
			String resultDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
			
			//실패 목록
            List<Sample> failSamples = new ArrayList<Sample>();

            List<String> reAnalysisChipBarcodes = new ArrayList<String>();
            List<Sample> reAnalysisSamples = new ArrayList<Sample>();
            
            // #. 상태가 S410_ANLS_RUNNING(분석중) 인 목록 조회
            Specification<Sample> where = Specification
					// .where(SampleSpecification.bundleIsActive())
                    .where(SampleSpecification.statusEqual(StatusCode.S410_ANLS_RUNNING))
                    .and(SampleSpecification.checkCelFileEqual("PASS"))
                    .and(SampleSpecification.isGenoData(false));
		
            List<Sample> list = sampleRepository.findAll(where);
			logger.info("completeChipAnalysis[" + resultDate + "]");
			for (Sample sample : list) {
                String genotypingId = sample.getGenotypingId();
				String chipBarcode = sample.getChipBarcode();
				String filePath = sample.getFilePath();
				
				String resultFilePath = filePath + "/" + chipBarcode + ".All.csv";
				String failFilePath = filePath + "/" + chipBarcode + ".Fail.csv";
				String logFilePath = filePath + "/" + chipBarcode + ".log";
				String errorFilePath = filePath + "/Error.log";
				File resultFile = new File(resultFilePath);
				File failFile = new File(failFilePath);
				File logFile = new File(logFilePath);
				File errorFile = new File(errorFilePath);
				
				// #. result, log 파일이 있으면 분석완료
				if (resultFile.exists() && logFile.exists()) {
					// #. 분석 완료인 경우
					logger.info(">> exist complete analysis chipBarcode=[" + chipBarcode + "]");
					// #. 분석 완료파일을 읽어서 내용 가져오기. 분석 완료파일을 읽어서 내용 가져오기. header값들을 키값으로 만들 맵 목록
					List<String> headerDatas = getHeaderDatas(resultFile);
					List<Map<String, Object>> resultRowDatas = getCsvDatas(resultFile);
					
					// #. error 파일에 내용을 읽기
					List<Map<String, Object>> failRowDatas = new ArrayList<Map<String, Object>>();
					List<String> failHeaderDatas = new ArrayList<String>();
					if (failFile.exists()) {
						failHeaderDatas = getHeaderDatas(failFile);
						failRowDatas = getCsvDatas(failFile);
					}
					
                    Map<String, Object> successRowData = Maps.newHashMap();
                    Map<String, Object> failRowData = Maps.newHashMap();
                    if (resultRowDatas.size() > 0) {
                        // #. samplekey 컬럼값 가져오기
                        String sampleKeyColumn = headerDatas.get(0);
                        
                        for (Map<String, Object> row : resultRowDatas) {
                            String sampleKey = (String) row.get(sampleKeyColumn);
                            if (genotypingId.equals(sampleKey)) {
                                successRowData = row;
                                break;
                            }
                        }
                    }
                    if (failRowDatas.size() > 0) {
                        String failSampleKeyColumn = failHeaderDatas.get(0);
                        for (Map<String, Object> row : failRowDatas) {
                            String sampleKey = (String) row.get(failSampleKeyColumn);
                            if (genotypingId.equals(sampleKey)) {
                                failRowData = row;
                                break;
                            }
                        }
                    }
                    LocalDateTime now = LocalDateTime.now();
                    sample.setModifiedDate(now);

                    if (!successRowData.isEmpty()) {
                        // #. data 하나를 가져와서 marker 체크
                        String sampleKeyColumn = headerDatas.get(0);
                        successRowData.remove(sampleKeyColumn);

                        Map<String, Object> data = Maps.newHashMap();
                        data.putAll(successRowData);
                        // for (Map<String, String> mi : allMarkerInfos) {
                        //     String name = mi.get("name");
                        //     String nameCode = mi.get("nameCode");
                        //     String value = (String) successRowData.get(name);
                            
                        //     data.put(nameCode, value);
                        // }
                        
                        // #. result 완료 처리
                        sample.setData(data);
                        sample.setStatusCode(StatusCode.S420_ANLS_SUCC);
                        //LocalDateTime now = LocalDateTime.now();
                        sample.setAnlsEndDate(now);
                        sampleRepository.save(sample);

						// #. sample checkDuplicationSample 값이 '△'인경우 실험대기 포함한 이전 상태 중복검체를 조회해 수정
						if ("△".equals(sample.getCheckDuplicationSample())) {
							Map<String, String> params = Maps.newHashMap();
							params.put("h_name", (String)sample.getItems().get("h_name"));
							params.put("chart_number", (String)sample.getItems().get("chart_number"));
							params.put("birthyear", (String)sample.getItems().get("birthyear"));
							params.put("sex", (String)sample.getItems().get("sex"));
							params.put("order", ",id:desc");

							Specification<Sample> duplWhere = Specification
								.where(SampleSpecification.hospitalDuplication(params))
								.and(SampleSpecification.statusCodeLt(200))
								.and(SampleSpecification.isLastVersionTrue())
								.and(SampleSpecification.isNotTest())
								.and(SampleSpecification.bundleIsActive())
								.and(SampleSpecification.orderBy(params));

							// #. 병원용 중복 검사이고 출고가 완료된 상태의 검체목록
							List<Sample> duplicationSamples = sampleRepository.findAll(duplWhere);

							duplicationSamples.stream().forEach(ds -> {
								if ("△".equals(ds.getCheckDuplicationSample())) {
									ds.setCheckDuplicationSample("○");
									sampleRepository.save(ds);
								}
							});
						}
                        
                    } else if (!failRowData.isEmpty()) {
                        String failMessage = "";

                        if (failRowData.get(failHeaderDatas.get(1)) != null) {
                            failMessage += (String)failRowData.get(failHeaderDatas.get(1));
                        }

                        if (failRowData.get(failHeaderDatas.get(2)) != null) {
                            if (failMessage.length() > 0) failMessage += " : ";
                            failMessage += (String)failRowData.get(failHeaderDatas.get(2));
                        }

                        // #. 분석 실패 파일에 존재하는 경우
                        sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                        sample.setStatusMessage(failMessage);
                        sample.setAnlsEndDate(now);
                        sampleRepository.save(sample);
                        failSamples.add(sample);

                        // #. 재분석 자동실행에 사용될 chip barcode 목록에 추가. 중복제거
                        if (!reAnalysisChipBarcodes.contains(chipBarcode)) {
                            reAnalysisChipBarcodes.add(chipBarcode);
                            reAnalysisSamples.add(sample);
                        }
                    } else {
                        // #. 둘다 존재하지 않는 경우
                        logger.info(">> Missing sample ID in file.=[" + genotypingId + "]");
                        // #. 상태 업데이트
                        sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                        sample.setStatusMessage("Missing sample ID in file[" + genotypingId + "]");
                        sample.setAnlsEndDate(LocalDateTime.now());
                        sampleRepository.save(sample);
                        failSamples.add(sample);
                    }
				} else if (errorFile.exists()) {
					// #. 분석 실패인 경우
                    logger.info(">> Error occurred during analysis[" + genotypingId + "]");
                    sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                    sample.setStatusMessage(this.getErrorLogString(errorFile));
                    sample.setAnlsEndDate(LocalDateTime.now());
                    sampleRepository.save(sample);
                    failSamples.add(sample);

                    // #. 재분석 자동실행에 사용될 chip barcode 목록에 추가. 중복제거
                    if (!reAnalysisChipBarcodes.contains(chipBarcode)) {
                        reAnalysisChipBarcodes.add(chipBarcode);
                        reAnalysisSamples.add(sample);
                    }
				}
            }
            
            if (failSamples.size() > 0) {
                emailSender.sendMailToFail(failSamples);
            }

            // #. 재분석 실행
            if (reAnalysisSamples.size() > 0 ) {
                for (Sample reAnalysisSample : reAnalysisSamples) {
                    analysisService.doPythonReAnalysis(reAnalysisSample.getChipBarcode(), reAnalysisSample.getChipTypeCode(), reAnalysisSample.getFilePath());
                }
            }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    /**
	 * GenoData Chip Analysis 완료 스케쥴러
	 * 스케쥴 5 분마다 (300초 = 1000 * 300)
	 */
	@Transactional
	@Scheduled(fixedDelay = 1000 * 300)
	public void completeChipAnalysisForGenoData() {
		try {
			Calendar cal = Calendar.getInstance();
			String resultDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
			
			//실패 목록
            List<Sample> failSamples = new ArrayList<Sample>();
            
            // #. 상태가 S410_ANLS_RUNNING(분석중)이고 GenoData인 목록 조회
            Specification<Sample> where = Specification
					// .where(SampleSpecification.bundleIsActive())
                    .where(SampleSpecification.statusEqual(StatusCode.S410_ANLS_RUNNING))
                    .and(SampleSpecification.checkCelFileEqual("PASS"))
                    .and(SampleSpecification.isGenoData(true));
		
            List<Sample> list = sampleRepository.findAll(where);
			logger.info("completeChipAnalysisForGenoData[" + resultDate + "]");
			LocalDateTime now = LocalDateTime.now();

			for (Sample sample : list) {
                // #. sample 별로 API를 통해서 해당 검체가 분석중인지 체크
				String genoDataResult = getResultGenoDataAnalysisBySampleId(sample.getId());
				logger.info("☆☆☆ completeChipAnalysisForGenoData check sample id = [" + sample.getGenotypingId() + "]");
				if (genoDataResult.equals("OK")) {
					// #. 분석완료면 성공 결과 상태값만 업데이트
					Map<String, Object> data = Maps.newHashMap();
					data.put("result", "success");
					sample.setData(data);
					sample.setModifiedDate(now);
					sample.setStatusCode(StatusCode.S420_ANLS_SUCC);
					sample.setAnlsEndDate(now);
					sampleRepository.save(sample);
				} else if (genoDataResult.indexOf("FAIL") >= 0) {
					String[] rts = genoDataResult.split(":");
					String errorMsg = "";
					if (rts.length > 1) {
						errorMsg = rts[1];
					}

					// #. 실패면 문구 확인 및 상태 저장
					logger.info(">> FAIL occurred during analysis[" + sample.getGenotypingId() + "]");
					sample.setModifiedDate(now);
                    sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                    sample.setStatusMessage(errorMsg);
                    sample.setAnlsEndDate(now);
                    sampleRepository.save(sample);

					failSamples.add(sample);
				} else if (genoDataResult.contains("RUNNING")) {
					logger.info(">> RUNNING analysis[" + sample.getGenotypingId() + "]");
					// #. 분석중이면 그냥 continue
					continue;
				} else {
					// #. 그외 문자열이 오면 실패 처리 후 문자열 저장
					logger.info(">> error occurred during analysis[" + sample.getGenotypingId() + "]");
					sample.setModifiedDate(now);
                    sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                    sample.setStatusMessage(genoDataResult);
                    sample.setAnlsEndDate(now);
                    sampleRepository.save(sample);

					failSamples.add(sample);
				}
            }
            
            if (failSamples.size() > 0) {
                emailSender.sendMailToFail(failSamples);
            }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * GenoData 검체중에 30일이 지나면 결과파일 삭제 요청
	 * 스케쥴 매일 0시에 실행 (cron = "0 0 0 * * *")
	 */
	@Transactional
	@Scheduled(cron = "0 0 0 * * *")
	public void deleteResultFileForGenoData() {
		try {
			Calendar cal = Calendar.getInstance();
			String nowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
			cal.add(Calendar.DAY_OF_MONTH, -30);
			String beforeDay = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
			logger.info("deleteResultFileForGenoData now[" + nowDate + "]");
			logger.info("deleteResultFileForGenoData beforeDay[" + beforeDay + "]");

			Map<String, String> params = Maps.newHashMap();
			params.put("sDate", beforeDay);
			params.put("fDate", beforeDay);
			
            // #. 상태가 출고완료 GenoData인 목록 조회
            Specification<Sample> where = Specification
					// .where(SampleSpecification.bundleIsActive())
                    .where(SampleSpecification.mappingInfoGroupBy())
					.and(SampleSpecification.statusEqual(StatusCode.S710_OUTPUT_CMPL))
                    .and(SampleSpecification.customDateBetween("outputCmplDate", params))
                    .and(SampleSpecification.isGenoData(true));
		
            List<Sample> list = sampleRepository.findAll(where);
			
			for (Sample sample : list) {
				deleteResultGenoDataByChipBarcode(sample.getChipBarcode());
            }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 하루에 한번씩 지연된 검체가 있는 확인하고 메일발송
	 * 스케줄 매일 0시에 실행 (cron = "0 0 0 * * *")
	 */
	@Transactional
	@Scheduled(cron = "0 0 0 * * *")
	public void sendMailForDelayedSamples() {
		try {
			LocalDateTime now = LocalDateTime.now();
			String nowDateString = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			String beforeDateString = now.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			logger.info("sendMailForDelayedSamples nowDate[" + nowDateString + "]");
			logger.info("sendMailForDelayedSamples beforeDay[" + beforeDateString + "]");

			// #. 상태가 출고 완료 이전의 검체중 TAT 당일인 목록 조회
			Specification<Sample> nowWhere = Specification
			.where(SampleSpecification.isLastVersionTrue())
			.and(SampleSpecification.isNotTest())
			.and(SampleSpecification.bundleIsActive())
			.and(SampleSpecification.tatEqual(nowDateString))
			.and(SampleSpecification.statusCodeLt(700));
	
			List<Sample> nowList = sampleRepository.findAll(nowWhere);

            // #. 상태가 출고 완료 이전의 검체중 TAT가 하루 남은 목록 조회
            Specification<Sample> beforeWhere = Specification
				.where(SampleSpecification.isLastVersionTrue())
				.and(SampleSpecification.isNotTest())
				.and(SampleSpecification.bundleIsActive())
				.and(SampleSpecification.tatEqual(beforeDateString))
				.and(SampleSpecification.statusCodeLt(700));
		
            List<Sample> beforeList = sampleRepository.findAll(beforeWhere);


			// #. 당일인 목록은 총괄책임자 email, 입출고 서비스 email 로 발송
			if (nowList != null && nowList.size() > 0) {
				emailSender.sendMailToDelay(nowList, Arrays.asList(new String[] { managerEmail, serviceManagerEmail }));
			}

			// #. 하루 남은 목록은 입출고 서비스 email로 메일 발송
			if (beforeList != null && beforeList.size() > 0) {
				emailSender.sendMailToDelay(beforeList, Arrays.asList(new String[] { serviceManagerEmail }));
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private List<String> getHeaderDatas(File csvFile) {
		List<String> headers = new ArrayList<String>();
		BufferedReader br = null;
		try {
			// #. read csv 데이터 파일
			br = new BufferedReader(new FileReader(csvFile));
			String line = br.readLine();
			String[] items = line.split(",", -1);
			headers.addAll(Arrays.asList(items));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return headers;
	}
	
	/**
	 * header 값을 map key값으로 해서 map 만들기
	 * @param csvFile
	 * @return
	 */
	private List<Map<String, Object>> getCsvDatas(File csvFile) {
		List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
		BufferedReader br = null;
		try {
			// #. read csv 데이터 파일
			br = new BufferedReader(new FileReader(csvFile));
			String line = "";
			int row = 0;
			List<String> headerValues = new ArrayList<String>();

			while ((line = br.readLine()) != null) {
				// #. -1 옵션은 마지막 "," 이후 빈 공백도 읽기 위한 옵션
				String[] items = line.split(",", -1);
				// #. 첫번쨰 라인에 값을 키값으로 셋팅
				if (row == 0) {
					headerValues.addAll(Arrays.asList(items));
				} else {
					Map<String, Object> data = Maps.newHashMap();
					for (int i = 0; i < items.length; i++) {
						String key = headerValues.get(i);
						String value = items[i];
						data.put(key, value);
					}
					datas.add(data);
				}
				
				row++;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return datas;
	}
    
    private String getErrorLogString(File errorFile) {
		String errorString = "";
		BufferedReader br = null;
		try {
            // #. read csv 데이터 파일
            br = new BufferedReader(new FileReader(errorFile));
            String sLine = null;
            while((sLine = br.readLine()) != null) {
                errorString += sLine + ",";
            }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return errorString;
	}

	/**
	 * GenoData 분석서버에서 sampleId 값으로 상태조회
	 * @param sampleId
	 * @return
	 */
	private String getResultGenoDataAnalysisBySampleId(int sampleId) {
		String result = "";
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(genoDataApiTokenName, genoDataApiToken);
			
			HttpEntity<String> entity = new HttpEntity<String>("", headers);
	
			RestTemplate restTemplate = new RestTemplate();
			
			// #. 호출
			String apiUrl = genoDataApiUrl + "anls/chk/" + sampleId;
			logger.info("★★★ getResultGenoDataAnalysisBySampleId apiUrl=" + apiUrl);
			// #. get parameter 붙이는 경우
			// UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl).queryParam("experimentid", "GDX-T-2204-0012");
			ResponseEntity<String> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

			// #. 결과가 json이면 아래를 이용
			// ObjectMapper mapper = new ObjectMapper();
			// Map<Object, Object> resultMap = mapper.readValue(responseEntity.getBody(), new TypeReference<Map<Object, Object>>(){});
	
	
			logger.info("★★★ getResultGenoDataAnalysisBySampleId getBody=[" + responseEntity.getBody() + "]");
			result = responseEntity.getBody();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * GenoData 분석서버에서 chipBarcode로 결과파일 삭제요청
	 * @param sampleId
	 * @return
	 */
	private String deleteResultGenoDataByChipBarcode(String chipBarcode) {
		String result = "";
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(genoDataApiTokenName, genoDataApiToken);
			
			HttpEntity<String> entity = new HttpEntity<String>("", headers);
	
			RestTemplate restTemplate = new RestTemplate();
			
			// #. 호출
			String apiUrl = genoDataApiUrl + "delete/" + chipBarcode;
			logger.info("★★★ deleteResultGenoDataByChipBarcode apiUrl=" + apiUrl);
			// #. get parameter 붙이는 경우
			// UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl).queryParam("experimentid", "GDX-T-2204-0012");
			ResponseEntity<String> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

			// #. 결과가 json이면 아래를 이용
			// ObjectMapper mapper = new ObjectMapper();
			// Map<Object, Object> resultMap = mapper.readValue(responseEntity.getBody(), new TypeReference<Map<Object, Object>>(){});
	
	
			logger.info("★★★ deleteResultGenoDataByChipBarcode getBody=[" + responseEntity.getBody() + "]");
			result = responseEntity.getBody();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}
}