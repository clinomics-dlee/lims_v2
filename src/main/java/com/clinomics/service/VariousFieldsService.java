package com.clinomics.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Holiday;
import com.clinomics.entity.lims.Sample;
import com.clinomics.repository.lims.HolidayRepository;
import com.clinomics.repository.lims.SampleRepository;
import com.clinomics.util.CustomIndexPublisher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VariousFieldsService {

    @Autowired
    HolidayRepository holidayRepository;

	@Autowired
    CustomIndexPublisher customIndexPublisher;
	
	@Autowired
	SampleRepository sampleRepository;

    public void setFields(boolean existsSample, Sample sample, Map<String, Object> items) {
        Bundle bundle = sample.getBundle();
        String strCollectedDate = items.getOrDefault("collecteddate", "").toString();
        if (!strCollectedDate.isEmpty() && strCollectedDate.matches("^([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))$")) {

            sample.setCollectedDate(LocalDate.parse(strCollectedDate));
            items.remove("collecteddate");
        }

        String strReceivedDate = items.getOrDefault("receiveddate", "").toString();
        LocalDate receivedDate = null;
        if (strReceivedDate.matches("^([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))$")) {
            receivedDate = LocalDate.parse(strReceivedDate);
        }
        if (!strReceivedDate.isEmpty() && receivedDate != null) {

            items.put("tat", getTat(bundle, strReceivedDate));
            sample.setReceivedDate(LocalDate.parse(strReceivedDate));
            items.remove("receiveddate");
        }

        sample.setSampleType(items.getOrDefault("sampletype", "").toString());
        items.remove("sampletype");
        
        if (!existsSample && bundle.isAutoSequence()) {
            
            String seq = customIndexPublisher.getNextSequenceByBundle(bundle, receivedDate);
            if (!seq.isEmpty()) sample.setLaboratoryId(seq);
        } else if (items.containsKey("laboratory")) {
            sample.setLaboratoryId(items.get("laboratory").toString());
        }
    }

    private String getTat(Bundle bundle, String receivedDate) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate start = LocalDate.parse(receivedDate, formatter);
        LocalDate end = start.plusYears(1);

        List<Holiday> holidays = holidayRepository.findByDateBetween(start.format(yyyymmdd), end.format(yyyymmdd));

        List<String> hdays = holidays.stream().map(h -> { return h.getDate(); }).collect(Collectors.toList());

        LocalDate temp;
        int max = bundle.getTatDay();
        
        if (bundle.isTatTueThu()) {
            start = start.plusDays(getNextTueOrThu(start));
        }

        for (int i = 0; i <= max; i++) {
            temp = start.plusDays(i);
            if (hdays.contains(temp.format(yyyymmdd)) || isWeekend(temp)) {
                max++;
            }
        }

        String rtn = start.plusDays(max).format(formatter);
        //items.put("tat", rtn);
        return rtn;
        
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private int getNextTueOrThu(LocalDate date) {
        LocalDate rDate = date;
        int rtn = 0;
        while (rDate.getDayOfWeek() != DayOfWeek.TUESDAY && rDate.getDayOfWeek() != DayOfWeek.THURSDAY) {
            rDate = rDate.plusDays(1);
            rtn++;
        }
        return rtn;
    }
    
    
    
    public void setFieldsTest(boolean existsSample, Sample sample, Map<String, Object> items) {
        Bundle bundle = sample.getBundle();
        String strCollectedDate = items.getOrDefault("collecteddate", "").toString();
        if (!strCollectedDate.isEmpty() && strCollectedDate.matches("^([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))$")) {

            sample.setCollectedDate(LocalDate.parse(strCollectedDate));
            items.remove("collecteddate");
        }

        String strReceivedDate = items.getOrDefault("receiveddate", "").toString();
        LocalDate receivedDate = null;
        if (strReceivedDate.matches("^([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))$")) {
            receivedDate = LocalDate.parse(strReceivedDate);
        }
        if (!strReceivedDate.isEmpty() && receivedDate != null) {

            items.put("tat", getTat(bundle, strReceivedDate));
            sample.setReceivedDate(LocalDate.parse(strReceivedDate));
            items.remove("receiveddate");
        }

        sample.setSampleType(items.getOrDefault("sampletype", "").toString());
        items.remove("sampletype");
        
        if (!existsSample && bundle.isAutoSequence()) {
        	
        	String lastLaboratoryId = sampleRepository.findMaxTestLaboratoryId(bundle.getId());
        	String seq = "";
        	
        	String[] roles = bundle.getSequenceRole().split("-");
        	String seqRoleHead = "";
        	if (roles.length > 0) {
        		seqRoleHead = roles[0];
        	}
        	
        	if( lastLaboratoryId == null) {
        		
        		seq = seqRoleHead + "-TEST-0001";
        		
        	}else {
        		int newIndexNumber = NumberUtils.toInt(lastLaboratoryId) + 1;
				seq = seqRoleHead + "-TEST-" + String.format("-%04d", newIndexNumber);
        	}
           // String seq = customIndexPublisher.getNextSequenceByBundle(bundle, receivedDate);
            if (!seq.isEmpty()) sample.setLaboratoryId(seq);
        } else if (items.containsKey("laboratory")) {
            sample.setLaboratoryId(items.get("laboratory").toString());
        }
    }
}