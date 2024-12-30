package com.example.inventoryapp;

import android.icu.util.Calendar;

import java.util.ArrayList;
import java.util.Arrays;

// FreezerDate.java
public class FreezerDate implements Comparable<FreezerDate> {
    private int year;
    private int month; // 1-12

    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    public FreezerDate(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public static FreezerDate now() {
        Calendar cal = Calendar.getInstance();
        return new FreezerDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }

    public static FreezerDate plusYears(FreezerDate date, int years) {
        return new FreezerDate(date.getYear() + years, date.getMonth());
    }

    public String format() {
        return String.format("%d %s", year, MONTHS[month - 1].substring(0, 3));
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }

    @Override
    public int compareTo(FreezerDate other) {
        int yearCompare = Integer.compare(this.year, other.year);
        if (yearCompare != 0) return yearCompare;
        return Integer.compare(this.month, other.month);
    }

    public static ArrayList<String> getMonthsList() {
        return new ArrayList<>(Arrays.asList(MONTHS));
    }
}