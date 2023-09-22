package com.example.myapplication.screen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.Utils;
import com.example.myapplication.weater.WeatherApi;
import com.example.myapplication.weater.WeatherDescriptionConverter;
import com.example.myapplication.weater.WeatherResponse;
import com.squareup.picasso.Picasso;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

// 잠금화면
public class lockscreen extends AppCompatActivity {
    private TextView yearTextView;
    private TextView dateTextView;
    private TextView weatherDescTextView;
    private TextView temperatureTextView;
    private TextView humidityTextView;
    private ImageView weatherImageView;
    private static final String API_KEY = "9bc00b8817211dc754226b2faae450bc";

    private GestureDetector gestureDetector;

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

        // 날씨 정보 업데이트 메서드 호출
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
        // Retrofit 빌더 생성
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create());

        // OkHttp 클라이언트 생성 및 로깅 인터셉터 추가
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // 로그 레벨 설정 (BASIC, HEADERS, BODY 등)

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
                .build();

        // Retrofit에 OkHttp 클라이언트 설정
        Retrofit retrofit = retrofitBuilder.client(okHttpClient).build();

        // WeatherApi 인터페이스 생성
        WeatherApi weatherApi = retrofit.create(WeatherApi.class);

        // 도시 이름 설정
        String cityName = "Daejeon";  // 원하는 도시 이름으로 변경

        Call<WeatherResponse> call = weatherApi.getWeatherData(cityName, API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherData = response.body();
                    // 날씨 정보를 처리하고 UI에 표시
                    updateWeatherUI(weatherData);
                } else {
                    // 오류 처리
                    // response.code()를 사용하여 HTTP 응답 코드 확인 가능
                    Log.e("WeatherApp", "API 요청 실패. HTTP 응답 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // 네트워크 오류 또는 예외 처리
                Log.e("WeatherApp", "API 요청 실패. 예외 메시지: " + t.getMessage());
            }
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