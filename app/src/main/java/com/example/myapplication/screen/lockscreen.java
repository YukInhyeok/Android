package com.example.myapplication.screen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.Utils;
import com.example.myapplication.weather.WeatherApi;
import com.example.myapplication.weather.WeatherDescriptionConverter;
import com.example.myapplication.weather.WeatherResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// 잠금화면
public class lockscreen extends AppCompatActivity {
    private TextView yearTextView;
    private TextView dateTextView;
    private TextView weatherDescTextView;
    private TextView temperatureTextView;
    private TextView humidityTextView;
    private TextView weatherlocationTextView;
    private ImageView weatherImageView;
    private static final String API_KEY = "9bc00b8817211dc754226b2faae450bc";

    private GestureDetector gestureDetector;

    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockscreen);

        // 메뉴 하단바 삭제
        Utils.deleteMenuButton(this);

        // 홈 버튼이나 뒤로가기 버튼 비활성화를 위한 플래그 추가
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // 스와이프 효과
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        // 현재 날짜 표시
        yearTextView = findViewById(R.id.year);
        dateTextView = findViewById(R.id.date);

        // 현재 날짜 및 요일 계산
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy년", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일 (E)", Locale.getDefault());

        String currentYear = yearFormat.format(calendar.getTime());
        String currentDate = dateFormat.format(calendar.getTime());

        // 년도와 날짜를 TextView에 설정
        yearTextView.setText(currentYear);
        dateTextView.setText(currentDate);

        // 날씨 정보를 표시할 TextView 참조
        weatherDescTextView = findViewById(R.id.weather_description);
        temperatureTextView = findViewById(R.id.temperature);
        humidityTextView = findViewById(R.id.humidity);
        weatherImageView = findViewById(R.id.weatherImageView);
        weatherlocationTextView = findViewById(R.id.weather_location);

        // 날씨 정보 업데이트 메서드 호출
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        updateWeatherInfo();

        ImageView gifImageView = findViewById(R.id.rain_img);
        Glide.with(this).load(R.drawable.rain2).into(gifImageView);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY) &&
                    Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    // 스와이프가 오른쪽으로 발생할 경우
                    animateActivityTransitionRight();
                } else {
                    // 스와이프가 왼쪽으로 발생할 경우
                    animateActivityTransitionLeft();
                }
            } else if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                    Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    // 스와이프가 아래쪽으로 발생할 경우
                    animateActivityTransitionDown();
                } else {
                    // 스와이프가 위쪽으로 발생할 경우
                    animateActivityTransitionUp();
                }
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        private void animateActivityTransition(float translationX, float translationY) {
            View mainLayout = findViewById(R.id.mainLayout);
            mainLayout.animate()
                    .translationX(translationX)
                    .translationY(translationY)
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            // 다른 액티비티로 전환하는 코드 추가
                            Intent intent = new Intent(lockscreen.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(0, 0);
                        }
                    })
                    .start();
        }

        private void animateActivityTransitionRight() {
            float translationX = 200f * (getResources().getDisplayMetrics().density);
            animateActivityTransition(translationX, 0);
        }

        private void animateActivityTransitionLeft() {
            float translationX = -200f * (getResources().getDisplayMetrics().density);
            animateActivityTransition(translationX, 0);
        }

        private void animateActivityTransitionUp() {
            float translationY = -200f * (getResources().getDisplayMetrics().density);
            animateActivityTransition(0, translationY);
        }

        private void animateActivityTransitionDown() {
            float translationY = 200f * (getResources().getDisplayMetrics().density);
            animateActivityTransition(0, translationY);
        }

    }

    //===================================================================================
    private void updateWeatherInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(lockscreen.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();

                        Geocoder geocoder = new Geocoder(lockscreen.this, Locale.getDefault());
                        try{
                            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                            if (addresses != null && addresses.size() > 0) {
                                Address address = addresses.get(0);

                                String adminArea = address.getAdminArea();
                                String subLocality = address.getSubLocality();
                                String thoroughfare = address.getThoroughfare();

                                StringBuilder locationTextBuilder = new StringBuilder();
                                if (adminArea != null) {
                                    locationTextBuilder.append(adminArea);
                                    Log.d("location1", "주소1: " + adminArea);
                                }
                                if (subLocality != null) {
                                    locationTextBuilder.append(" ").append(subLocality);
                                }
                                if (thoroughfare != null) {
                                    locationTextBuilder.append(" ").append(thoroughfare);
                                }
                                String locationText = locationTextBuilder.toString();

                                weatherlocationTextView.setText(locationText);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        getWeatherData(lat, lon);
                    }
                });
    }
    private void getWeatherData(double lat, double lon){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApi weatherApi = retrofit.create(WeatherApi.class);

        Call<WeatherResponse> call = weatherApi.getOneCall(lat, lon, API_KEY);

        Log.d("Retrofit", "URL: " + call.request().url());

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherData = response.body();
                    updateWeatherUI(weatherData);
                } else { /*...*/ }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) { /*...*/ }
        });
    }
    // updateWeatherUI 메서드 내부에 이미지 로드 코드 추가
    private void updateWeatherUI(WeatherResponse weatherData) {
        int weatherId = weatherData.getWeather().get(0).getId();
        String weatherDescription = WeatherDescriptionConverter.convertToKorean(weatherId);
        double temperature = weatherData.getMain().getTemp() - 273.15; // 켈빈 온도를 섭씨로 변환
        int humidity = weatherData.getMain().getHumidity();
        String weatherIcon = weatherData.getWeather().get(0).getIcon(); // 날씨 아이콘 코드 가져오기

        weatherDescTextView.setText(weatherDescription);
        temperatureTextView.setText(String.format(Locale.getDefault(), "온도: %.1f°C", temperature));
        humidityTextView.setText(String.format(Locale.getDefault(), "습도: %d%%", humidity));

        // 날씨 상태 코드를 기반으로 이미지 URL 생성
        String weatherImageUrl = "http://openweathermap.org/img/wn/" + weatherIcon + "@4x.png";
        Log.d("Picasso","이미지 주소: " + weatherImageUrl);

        Picasso.get()
                .load(weatherImageUrl)
                .into(weatherImageView, new com.squareup.picasso.Callback() { // 이 부분 수정
                    @Override
                    public void onSuccess() {
                        // 이미지가 성공적으로 로드될 때 실행되는 코드
                        Log.d("Picasso", "Image loaded successfully");
                    }
                    @Override
                    public void onError(Exception e) {
                        // 이미지 로딩 중 오류가 발생할 때 실행되는 코드
                        Log.e("Picasso", "Error loading image: " + e.getMessage());
                    }
                });

    }
    protected void onBackpressed(){

    }
}