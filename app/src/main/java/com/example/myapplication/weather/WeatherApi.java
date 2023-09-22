package com.example.myapplication.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("weather")
    Call<WeatherResponse> getWeatherData(
            @Query("q") String cityName,  // 도시 이름
            @Query("appid") String apiKey  // OpenWeatherMap API 키
    );
}
