package com.clinomics.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Holiday;
import com.clinomics.entity.lims.Sample;
import com.clinomics.entity.lims.SampleTest;
import com.clinomics.repository.lims.HolidayRepository;
import com.clinomics.util.CustomIndexPublisher;
import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

@Service
public class VariousFieldsService {

    @Autowired
    HolidayRepository holidayRepository;

	@Autowired
    CustomIndexPublisher customIndexPublisher;

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
    
    
    
    public void setFieldsTest(boolean existsSample, SampleTest sampleTest, Map<String, Object> items) {
        Bundle bundle = sampleTest.getBundle();
        String strCollectedDate = items.getOrDefault("collecteddate", "").toString();
        if (!strCollectedDate.isEmpty() && strCollectedDate.matches("^([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))$")) {

            sampleTest.setCollectedDate(LocalDate.parse(strCollectedDate));
            items.remove("collecteddate");
        }

        String strReceivedDate = items.getOrDefault("receiveddate", "").toString();
        LocalDate receivedDate = null;
        if (strReceivedDate.matches("^([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))$")) {
            receivedDate = LocalDate.parse(strReceivedDate);
        }
        if (!strReceivedDate.isEmpty() && receivedDate != null) {

            items.put("tat", getTat(bundle, strReceivedDate));
            sampleTest.setReceivedDate(LocalDate.parse(strReceivedDate));
            items.remove("receiveddate");
        }

        sampleTest.setSampleType(items.getOrDefault("sampletype", "").toString());
        items.remove("sampletype");
        
        if (!existsSample && bundle.isAutoSequence()) {
            
            String seq = customIndexPublisher.getNextSequenceByBundle(bundle, receivedDate);
            if (!seq.isEmpty()) sampleTest.setLaboratoryId(seq);
        } else if (items.containsKey("laboratory")) {
            sampleTest.setLaboratoryId(items.get("laboratory").toString());
        }
    }
}