package com.vv.meteopolis.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Forecast {

    @SerializedName("list")
    private List<WeatherForecast> weatherForecastList;

    public List<WeatherForecast> getWeatherForecastList() {
        return weatherForecastList;
    }

    public void setWeatherForecastList(List<WeatherForecast> weatherForecastList) {
        this.weatherForecastList = weatherForecastList;
    }
}
