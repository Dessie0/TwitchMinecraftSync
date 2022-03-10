package com.twitchmcsync.twitchminecraft.lang;

import java.time.ZonedDateTime;

public class DateFormatter {
    public static String format(String date) {
        ZonedDateTime time = ZonedDateTime.parse(date);
        StringBuilder formatted = new StringBuilder();

        String month = time.getMonth().toString().substring(0, 1) + time.getMonth().toString().substring(1).toLowerCase();

        formatted.append(month)
                .append(" ")
                .append(time.getDayOfMonth())
                .append(", ")
                .append(time.getYear())
                .append(" at ");

        String hour;
        if (time.getHour() < 10) {
            hour = "0" + time.getHour();
        } else {
            hour = String.valueOf(time.getHour());
        }

        String minute;
        if (time.getMinute() < 10) {
            minute = "0" + time.getMinute();
        } else {
            minute = String.valueOf(time.getMinute());
        }

        formatted.append(hour).append(":").append(minute);

        return formatted.toString();
    }

}
