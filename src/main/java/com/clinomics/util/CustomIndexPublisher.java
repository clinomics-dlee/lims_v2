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
			Optional<Sample> last = sampleRepository.findTopByBundle_IdAndReceivedDateOrderByLaboratoryIdDesc(bundle.getId(), receivedDate);
			if (last.isPresent()) {
				String lastLaboratoryId = last.get().getLaboratoryId();
				int zeroCount = StringUtils.countMatches(role, "0");
				int newIndexNumber = NumberUtils.toInt(StringUtils.right(lastLaboratoryId, zeroCount)) + 1;
	
				index = StringUtils.left(lastLaboratoryId, lastLaboratoryId.length() - zeroCount) + "" + String.format("%04d", newIndexNumber);
			} else {
				index = getIndex(role.split(separator), current, receivedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			}
			
		} else {
			Optional<Sample> last = sampleRepository.findTopByBundle_IdOrderByLaboratoryIdDesc(bundle.getId());
			if (last.isPresent()) {
				String lastLaboratoryId = last.get().getLaboratoryId();
				index = getIndex(role.split(separator), lastLaboratoryId, getYYYYMMDD("yyyyMMdd"));
			} else {
				index = getIndex(role.split(separator), current, getYYYYMMDD("yyyyMMdd"));
			}
			bundle.setSequence(index);
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
				} else if (t.matches("[0]+")) {
					
					if (current == null || !current.startsWith(index.substring(1))) {
						index += separator + String.format("%0" + t.length() + "d", 1);
					} else {
						index += separator + String.format("%0" + t.length() + "d", NumberUtils.toInt(current.substring(current.length() - 4)) + 1);
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
