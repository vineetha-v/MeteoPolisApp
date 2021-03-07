package com.vv.meteopolis.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {

    public static String convertKelvinToCelsius(Double kelvinTemp) {
        return ((new DecimalFormat("0.00")).format(kelvinTemp - 273.15))+"\u2103";
    }

    public static String convertMillisecToDate(long milliDate) {
        Date date = new Date(milliDate*1000L);
        DateFormat formatter = new SimpleDateFormat("dd MMM, EEE");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT-4"));
        return formatter.format(date);
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, EEE");
        return sdf.format(new Date());
    }
}
