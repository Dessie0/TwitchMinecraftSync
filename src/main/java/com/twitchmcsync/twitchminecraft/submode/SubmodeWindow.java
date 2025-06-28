package com.twitchmcsync.twitchminecraft.submode;

import lombok.Getter;
import me.dessie.dessielib.annotations.storageapi.RecomposeConstructor;
import me.dessie.dessielib.annotations.storageapi.Stored;
import me.dessie.dessielib.annotations.storageapi.StoredList;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
public class SubmodeWindow {

    @StoredList(type = String.class, storeAs = "days")
    private final List<String> rawDays;

    @Stored(storeAs = "start")
    private final String rawStart;

    @Stored(storeAs = "end")
    private final String rawEnd;

    private final List<DayOfWeek> days;
    private final LocalTime start;
    private final LocalTime end;

    @RecomposeConstructor
    public SubmodeWindow(List<String> days, String start, String end) {
        this.rawDays = days;
        this.rawStart = start;
        this.rawEnd = end;

        this.days = days.stream().map(day -> DayOfWeek.valueOf(day.toUpperCase())).toList();
        this.start = LocalTime.parse(start);
        this.end = LocalTime.parse(end);
    }

    public boolean isInWindow(ZonedDateTime now) {
        return days.contains(now.getDayOfWeek()) &&
                !now.toLocalTime().isBefore(start) &&
                !now.toLocalTime().isAfter(end);
    }

}
