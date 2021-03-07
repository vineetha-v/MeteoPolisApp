package com.vv.meteopolis;

import retrofit2.http.GET;
import retrofit2.http.Query;
import io.reactivex.Observable;

public interface APIService {

    String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    @GET("weather")
    Observable<WeatherForecast> getWeatherForecast(@Query("lat") Double lat, @Query("lon") Double lon, @Query("appid") String AppId);

    @GET("weather")
    Observable<WeatherForecast> getWeatherForecastByCity(@Query("q") String city_name, @Query("appid") String AppId);
}
