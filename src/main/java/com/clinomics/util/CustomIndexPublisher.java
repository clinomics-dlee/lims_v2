package com.clinomics.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Sample;
import com.clinomics.repository.lims.SampleRepository;

@Component
public class CustomIndexPublisher {

	private static String separator = "-";

	@Autowired
	private SampleRepository sampleRepository;
	
	public String getNextBarcodeByBundle(Bundle bundle) {
		String role = bundle.getBarcodeRole();
		if (role == null || role.isEmpty()) {
			return "";
		}
		String current = bundle.getBarcode();
		
		String index = getIndex(role.split(separator), current, getYYYYMMDD("yyyyMMdd"));
		bundle.setBarcode(index);
		return index;
	}

	public String getNextSequenceByBundle(Bundle bundle, LocalDate receivedDate) {
		String role = bundle.getSequenceRole();
		if (role == null || role.isEmpty()) {
			return "";
		}
		String current = bundle.getSequence();
		String index = "";

		if (bundle.isHospital()) {
			//Optional<Sample> last = sampleRepository.findTopByBundle_IdAndReceivedDateOrderByLaboratoryIdDesc(bundle.getId(), receivedDate);
			String lastLaboratoryId = sampleRepository.findMaxHospitalLaboratoryId(bundle.getId(), receivedDate.format(DateTimeFormatter.ofPattern("yyyyMM")));

			if (lastLaboratoryId == null) {
				//String lastLaboratoryId = last.get().getLaboratoryId();
				index = getIndex(role.split(separator), null, receivedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			} else {
				int zeroCount = StringUtils.countMatches(role, "0");
				int newIndexNumber = NumberUtils.toInt(StringUtils.right(lastLaboratoryId, zeroCount)) + 1;
	
				index = StringUtils.left(lastLaboratoryId, lastLaboratoryId.length() - zeroCount) + "" + String.format("%04d", newIndexNumber);
			}
			
		} else {

			String lastLaboratoryId = sampleRepository.findMaxLaboratoryId(bundle.getId());
			if (lastLaboratoryId == null) {
				index = getIndex(role.split(separator), null, getYYYYMMDD("yyyyMMdd"));
			} else {
				index = getIndex(role.split(separator), lastLaboratoryId, getYYYYMMDD("yyyyMMdd"));
			}

		}
		bundle.setSequence(index);

		return index;
	}

	public String getNextManagementByBundle(Bundle bundle, LocalDate receivedDate) {
		String role = bundle.getManagementRole();
		if (role == null || role.isEmpty()) {
			return "";
		}
		String index = "";
		String yyyymmdd = "";

		String lastManagementNumber = sampleRepository.findMaxManagementNumber(bundle.getId());

		// #. 병원용이 아닌 DTC 서비스에 경우만 채번함
		if (!bundle.isHospital()) {
			yyyymmdd = getYYYYMMDD("yyyyMMdd");
			index = getIndex(role.split(separator), lastManagementNumber, yyyymmdd);
		}

		return index;
	}
	
	private String getIndex(String[] arrRole, String current, String yyyymmdd) {
		String index = "";
		String yyyymm = StringUtils.left(yyyymmdd, 6);
		for (String r : arrRole) {
			if (r.startsWith("[")) {
				String t = r.replaceAll("[\\[|\\]]", "");
				if (t.equals("YYMM")) {
					index += separator + yyyymm.substring(2);
				} else if (t.equals("YYYYMMDD")) {
					index += separator + yyyymmdd;
				} else if (t.equals("YYMMDD")) {
					index += separator + yyyymmdd.substring(2);
				} else if (t.equals("YYYYMM")) {
					index += separator + yyyymm;
				} else if (t.equals("YYYY")) {
					index += separator + yyyymmdd.substring(0, 4);
				} else if (t.matches("[0]+")) {
					
					int zero_cnt = StringUtils.countMatches(t, "0");

					if (current == null || !current.startsWith(index.substring(1))) {
						index += separator + String.format("%0" + zero_cnt + "d", 1);
					} else {
						index += separator + String.format("%0" + zero_cnt + "d", NumberUtils.toInt(current.substring(current.length() - zero_cnt)) + 1);
					}
					index = index.substring(1);
					
					break;
					
				}
			} else {
				index += separator + r;
			}
		}
		
		return index;
	}

	private String getYYYYMMDD(String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Calendar c1 = Calendar.getInstance();
		return sdf.format(c1.getTime());
	}
}
