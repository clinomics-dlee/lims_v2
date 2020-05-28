package com.clinomics.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.clinomics.entity.lims.Product;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;
import com.clinomics.repository.lims.ProductRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.service.SampleDbService;
import com.clinomics.specification.lims.SampleSpecification;
import com.clinomics.util.EmailSender;
import com.clinomics.util.ExcelReadComponent;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Scheduler {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	SampleRepository sampleRepository;
	
	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	SampleDbService sampleDbService;

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
            
            // #. 상태가 S410_ANLS_RUNNING(분석중) 인 목록 조회
            Specification<Sample> where = Specification
					.where(SampleSpecification.bundleIsActive())
                    .and(SampleSpecification.statusEqual(StatusCode.S410_ANLS_RUNNING))
                    .and(SampleSpecification.checkCelFileEqual("PASS"));
		
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
                    
                    if (!successRowData.isEmpty()) {
                        // #. data 하나를 가져와서 marker 체크
                        String sampleKeyColumn = headerDatas.get(0);
                        successRowData.remove(sampleKeyColumn);

                        // #. 분석완료 파일에 마커 목록
                        List<String> markers = successRowData.keySet().stream().collect(Collectors.toList());
                        
                        // #. 분석 성공 파일에 존재하는 경우 
                        // #. validate
                        Set<String> productTypes = Sets.newHashSet();
                        sample.getBundle().getProduct().stream().forEach(p -> {
                            Optional<Product> oProduct = productRepository.findById(p.getId());
                            Product product = oProduct.orElse(new Product());
                            productTypes.add(product.getType());
                        });
                        
                        // #. 상품목록이 가지고 있는 모든 마커 정보 조회
                        Map<String, List<Map<String, String>>> productTypeMarkerInfos = sampleDbService.getMarkerInfo(new ArrayList<String>(productTypes));
                        // #. marker 정보가 없는 경우
                        if (productTypeMarkerInfos.isEmpty()) {
                            // #. 조회한 마커 목록이 비어있는경우
                            logger.info(">> not found marker infomation error sample id=[" + genotypingId + "]");
                            sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                            sample.setStatusMessage("not found marker infomation");
                            sample.setAnlsEndDate(LocalDateTime.now());
                            sampleRepository.save(sample);
                            failSamples.add(sample);
                            continue;
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

                        // #. 마커 목록이 전부 있는 지 체크
                        for (String marker : checkMarkers) {
                            if (!markers.contains(marker)) {
                                notExistMarkers.add(marker);
                            }
                        }

                        if (notExistMarkers.size() > 0) {
                            // #. 마커가 존재하지 않는것이 있는 경우
                            logger.info(">> Not Exist Markers error sample id=[" + genotypingId + "]");
                            // #. resultUpload 상태 업데이트
                            sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                            sample.setStatusMessage("Not Exist Markers[" + genotypingId + "]=" + notExistMarkers.toString());
                            sample.setAnlsEndDate(LocalDateTime.now());
                            sampleRepository.save(sample);
                            failSamples.add(sample);
                            continue;
                        }
                        
                        // #. 마커는 있으나 값이 허용되지않은 값이 셋팅된것 체크
                        List<String> invalidMarkers = new ArrayList<String>();
                        for (Map<String, String> mi : allMarkerInfos) {
                            String name = mi.get("name");
                            String ref = mi.get("refValue");
                            String alt = mi.get("altValue");
                            // #. 마커는 있으나 값이 다른것
                            String value = (String) successRowData.get(name);
                            
                            // #. 4가지 조합이 아닌경우
                            if (!value.equals(ref + alt) && !value.equals(alt + alt)
                                    && !value.equals(ref + ref) && !value.equals(alt + ref )) {
                                invalidMarkers.add(name);
                            }
                        }
                        
                        if (invalidMarkers.size() > 0) {
                            // #. marker 값 유효하지 않은 경우
                            logger.info(">> Invalid Markers error sample id=[" + genotypingId + "]");
                            // #. resultUpload 상태 업데이트
                            sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                            sample.setStatusMessage("Invalid Markers Result[" + genotypingId + "]=" + invalidMarkers.toString());
                            sample.setAnlsEndDate(LocalDateTime.now());
                            sampleRepository.save(sample);
                            failSamples.add(sample);
                            continue;
                        }
                        
                        Map<String, Object> data = Maps.newHashMap();
                        logger.info("★★★★ successRowData=" + successRowData.toString());
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
                        sample.setAnlsEndDate(LocalDateTime.now());
                        sampleRepository.save(sample);
                        
                    } else if (!failRowData.isEmpty()) {
                        // #. 분석 실패 파일에 존재하는 경우
                        sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                        sample.setStatusMessage((String)failRowData.get(failHeaderDatas.get(1)));
                        sample.setAnlsEndDate(LocalDateTime.now());
                        sampleRepository.save(sample);
                        failSamples.add(sample);
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
                    sample.setStatusCode(StatusCode.S430_ANLS_FAIL);
                    sample.setStatusMessage("Error occurred during analysis[" + genotypingId + "]");
                    sample.setAnlsEndDate(LocalDateTime.now());
                    sampleRepository.save(sample);
                    failSamples.add(sample);
					logger.info(">> Error occurred during analysis=[" + genotypingId + "][" + errorFile.getAbsolutePath() + "]");
				}
            }
            
            if (failSamples.size() > 0) {
                emailSender.sendMailToFail(failSamples);
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
    
}