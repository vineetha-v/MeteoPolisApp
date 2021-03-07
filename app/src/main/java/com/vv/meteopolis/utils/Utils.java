package com.vv.meteopolis.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static String convertKelvinToCelsius(Double kelvinTemp) {
        return ((new DecimalFormat("0.00")).format(kelvinTemp - 273.15))+"\u2103";
    }

    public static String convertMillisecToDate(long milliDate) {
        DateFormat formatter = new SimpleDateFormat("dd MMM");
        Date resultDate = new Date(milliDate);
        return formatter.format(resultDate);
    }
}
