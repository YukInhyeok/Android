package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.myapplication.book.BookMainActivity;

import com.github.mikephil.charting.charts.BarChart;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.LogDescriptor;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MyInfo extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    //firebase
    private FirebaseFirestore db;

    private static final String WEEKLY_RESET_PREF = "WeeklyResetAlarmPref";
    private static final String WEEKLY_RESET_ALARM_SET = "WeeklyResetAlarmSet";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        TextView textView4 = findViewById(R.id.textView4);
        textView4.setText(getCurrentMonthAndWeek());

        TextView textView5 = findViewById(R.id.textView5);
        textView5.setText(getCurrentWeekDates());


        //firebase
        db = FirebaseFirestore.getInstance();

        // BottomNavigationView 초기화
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // 네비게이션 아이템 클릭 이벤트 처리
                int itemId = item.getItemId();

                if (itemId == R.id.menu_gpt) {
                    // "GPT" 아이템 클릭 시 동작
                    Intent intent1 = new Intent(MyInfo.this, ChatGpt.class);
                    startActivity(intent1);
                } else if (itemId == R.id.menu_book) {
                    // "숙제" 아이템 클릭 시 동작
                    Intent intent2 = new Intent(MyInfo.this, BookMainActivity.class);
                    startActivity(intent2);
                } else if (itemId == R.id.menu_home) {
                    // "정보확인" 아이템 클릭 시 동작
                    Intent intent3 = new Intent(MyInfo.this, MainActivity.class);
                    startActivity(intent3);
                }
                return true;
            }
        });

        // 초기 선택된 네비게이션 아이템 설정
        bottomNavigationView.setSelectedItemId(R.id.menu_info);

        // 레이더 차트 추가
        BarChart barChart = findViewById(R.id.info_chart);
        setData(barChart);

        //주간 차트 초기화
        SharedPreferences preferences = getSharedPreferences(WEEKLY_RESET_PREF, Context.MODE_PRIVATE);
        boolean isAlarmSet = preferences.getBoolean(WEEKLY_RESET_ALARM_SET, false);

        // onCreate() 메소드 내부에 있는 '주간 차트 초기화' 섹션을 아래 코드로 대체합니다.
        if (!isAlarmSet) {
            long initialDelay = calculateDelayForNextMonday();
            PeriodicWorkRequest resetWeeklyWorkRequest =
                    new PeriodicWorkRequest.Builder(WeeklyResetWorker.class, 7, TimeUnit.DAYS)
                            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                            .addTag("resetweeklydata")
                            .build();

            WorkManager.getInstance(this)
                    .enqueueUniquePeriodicWork("resetweeklydata", ExistingPeriodicWorkPolicy.KEEP, resetWeeklyWorkRequest);

            // SharedPreferences에 알람 설정 상태 저장
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(WEEKLY_RESET_ALARM_SET, true);
            editor.apply();
        }

    }

    private void setData(BarChart barChart) {
        fetchData(new FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<BarEntry> entries) {
                // Update the index number for entries
                for (int i = 0; i < entries.size(); i++) {
                    entries.get(i).setX(i + 1);
                }
                BarDataSet dataSet = new BarDataSet(entries, "주간 데이터");

                // Set colors for each bar
                List<Integer> colors = new ArrayList<>();
                colors.add(Color.BLUE); // 월
                colors.add(Color.GREEN); // 화
                colors.add(Color.RED); // 수
                colors.add(Color.CYAN); // 목
                colors.add(Color.MAGENTA); // 금
                dataSet.setColors(colors);

                BarData data = new BarData(dataSet);
                data.setBarWidth(0.5f);
                barChart.setData(data);
                barChart.invalidate();

                String[] labels = {"", "월", "화", "수", "목", "금"};

                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setDrawAxisLine(false);
//                xAxis.setDrawGridLines(false);
                xAxis.setAxisMinimum(0f);
                xAxis.setAxisMaximum(6f);

                // Disable YAxis labels
                YAxis leftAxis = barChart.getAxisLeft();
                leftAxis.setGranularity(20f);
                leftAxis.setAxisMinimum(0f);
                leftAxis.setAxisMaximum(100f);
                leftAxis.setDrawGridLines(false);
//                leftAxis.setDrawLabels(false);

                YAxis rightYAxis = barChart.getAxisRight();
                rightYAxis.setDrawLabels(false);

                // Disable scaling
                barChart.setScaleEnabled(false);
            }
        });
    }

    private String getCurrentMonthAndWeek() {
        Calendar calendar = Calendar.getInstance();

        // 현재 월 가져오기
        SimpleDateFormat monthFormat = new SimpleDateFormat("M", Locale.getDefault());
        String month = monthFormat.format(calendar.getTime());

        // 현재 주차 가져오기
        int weekOfYear = calendar.get(Calendar.WEEK_OF_MONTH);

        return month + "월 " + weekOfYear + "주차";
    }

    private String getCurrentWeekDates() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);

        int currentWeek = calendar.get(Calendar.WEEK_OF_MONTH);

        // 현재 주의 시작일과 종료일 계산
        calendar.set(Calendar.WEEK_OF_MONTH, currentWeek);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date startDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        Date endDate = calendar.getTime();

        // 날짜 형식 지정
        SimpleDateFormat dateFormat = new SimpleDateFormat("d", Locale.getDefault());

        // 해당 주의 날짜 목록 생성
        List<String> dates = new ArrayList<>();
        calendar.setTime(startDate);
        while (!calendar.getTime().after(endDate)) {
            dates.add(dateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return String.join("   ", dates);
    }


    private void fetchData(FirestoreCallback callback) {
        db.collection("WeekChart").orderBy("label")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<BarEntry> entries = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                int average = document.getLong("average").intValue();
                                int label = document.getLong("label").intValue();
                                // RadarEntry 대신 BarEntry 사용
                                entries.add(new BarEntry(label, average));
                            }

                            callback.onDataLoaded(entries);
                        } else {
                            Log.e("MyInfo", "Error fetching data", task.getException());
                        }
                    }
                });
    }

    // 콜백 인터페이스 추가
// 콜백 인터페이스 수정 (RadarEntry에서 BarEntry로 변경)
    public interface FirestoreCallback {
        void onDataLoaded(ArrayList<BarEntry> entries);
    }

    // 주간 데이터 초기화
// WorkManager 와 함께 사용될 다음 월요일 자정까지의 남은 시간을 계산합니다.
    private long calculateDelayForNextMonday() {
        Calendar now = Calendar.getInstance();
        Calendar nextMonday = (Calendar) now.clone();
        nextMonday.set(Calendar.HOUR_OF_DAY, 0);
        nextMonday.set(Calendar.MINUTE, 0);
        nextMonday.set(Calendar.SECOND, 0);
        nextMonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (now.after(nextMonday)) {
            nextMonday.add(Calendar.DATE, 7);
        }
        return nextMonday.getTimeInMillis() - now.getTimeInMillis();
    }

}