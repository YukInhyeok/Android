package com.example.myapplication.weather;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherResponse {
    @SerializedName("weather")
    private List<WeatherInfo> weatherInfoList;

    @SerializedName("main")
    private WeatherDetails weatherDetails;

    public List<WeatherInfo> getWeather() {
        return weatherInfoList;
    }

    public WeatherDetails getMain() {
        return weatherDetails;
    }

    public class WeatherInfo {
        @SerializedName("description")
        private String description;

        @SerializedName("id")
        private int id; // 날씨 상태 코드

        @SerializedName("icon") // 추가: 날씨 아이콘 코드
        private String icon;

        public String getDescription() {
            return description;
        }
        public int getId() {
            return id;
        }
        public String getIcon() {
            return icon;
        }
    }

    public class WeatherDetails {
        @SerializedName("temp")
        private double temp;

        @SerializedName("humidity")
        private int humidity;

        public double getTemp() {
            return temp;
        }

        public int getHumidity() {
            return humidity;
        }
    }
}
