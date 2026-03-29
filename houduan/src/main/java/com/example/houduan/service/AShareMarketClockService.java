package com.example.houduan.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class AShareMarketClockService {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime MORNING_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MORNING_CLOSE = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_OPEN = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_CLOSE = LocalTime.of(15, 0);

    public LocalDateTime now() {
        return LocalDateTime.now(MARKET_ZONE);
    }

    public boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    public boolean isTradingSession(LocalDateTime dateTime) {
        if (!isTradingDay(dateTime.toLocalDate())) {
            return false;
        }

        LocalTime time = dateTime.toLocalTime();
        boolean inMorningSession = !time.isBefore(MORNING_OPEN) && time.isBefore(MORNING_CLOSE);
        boolean inAfternoonSession = !time.isBefore(AFTERNOON_OPEN) && time.isBefore(AFTERNOON_CLOSE);
        return inMorningSession || inAfternoonSession;
    }
}
