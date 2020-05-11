package com.clinomics.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import com.clinomics.entity.lims.Bundle;

@Component
public class CustomIndexPublisher {

	private static String separator = "-";
	
	public String getNextBarcodeByBundle(Bundle bundle) {
		String role = bundle.getBarcodeRole();
		if (role == null || role.isEmpty()) {
			return "";
		}
		String current = bundle.getBarcode();
		
		String index = getIndex(role.split(separator), current);
		bundle.setBarcode(index);
		return index;
	}

	public String getNextSequenceByBundle(Bundle bundle) {
		String role = bundle.getSequenceRole();
		if (role == null || role.isEmpty()) {
			return "";
		}
		String current = bundle.getSequence();
		
		String index = getIndex(role.split(separator), current);
		bundle.setSequence(index);
		return index;
	}
	
	private String getIndex(String[] arrRole, String current) {
		String index = "";
		for (String r : arrRole) {
			if (r.startsWith("[")) {
				String t = r.replaceAll("[\\[|\\]]", "");
				if (t.equals("YYMM")) {
					index += separator + getYYYYMMDD("yyyyMM").substring(2);
				} else if (t.equals("YYYYMMDD")) {
					index += separator + getYYYYMMDD("yyyyMMdd");
				} else if (t.equals("YYMMDD")) {
					index += separator + getYYYYMMDD("yyyyMMdd").substring(2);
				} else if (t.equals("YYYYMM")) {
					index += separator + getYYYYMMDD("yyyyMM");
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
