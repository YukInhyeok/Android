package com.example.myapplication.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("weather")
    Call<WeatherResponse> getOneCall(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey  // OpenWeatherMap API 키
    );
}
