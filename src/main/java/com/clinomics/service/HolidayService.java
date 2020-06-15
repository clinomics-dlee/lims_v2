package com.clinomics.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Holiday;
import com.clinomics.repository.lims.HolidayRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HolidayService {

    @Autowired
    HolidayRepository holidayRepository;

    public void setTat(Bundle bundle, Map<String, String> items) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate start = LocalDate.parse(items.get("sampleReceived"), formatter);
        LocalDate end = start.plusYears(1);

        List<Holiday> holidays = holidayRepository.findByDateBetween(start.format(yyyymmdd), end.format(yyyymmdd));

        List<String> hdays = holidays.stream().map(h -> { return h.getDate(); }).collect(Collectors.toList());

        LocalDate temp;
        int max = bundle.getTatDay();
        
        if (bundle.isTatTueThu()) {
            start = start.plusDays(getNextTueOrThu(start));
        }

        for (int i = 0; i < max; i++) {
            temp = start.plusDays(i);
            if (hdays.contains(temp.format(yyyymmdd)) || isWeekend(temp)) {
                max++;
            }
        }

        String rtn = start.plusDays(max).format(formatter);
        items.put("tat", rtn);
        
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
}