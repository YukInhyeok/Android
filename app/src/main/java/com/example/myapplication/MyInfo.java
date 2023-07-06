package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.myapplication.book.BookMainActivity;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyInfo extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        TextView textView4 = findViewById(R.id.textView4);
        textView4.setText(getCurrentMonthAndWeek());

        TextView textView5 = findViewById(R.id.textView5);
        textView5.setText(getCurrentWeekDates());

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
        RadarChart radarChart = findViewById(R.id.info_chart);
//        insertDummyData(); // DB에 더미 데이터를 추가 (처음 한 번만 실행해서 데이터를 넣어주세요)
        setData(radarChart);
    }

    private void setData(RadarChart radarChart) {
        ArrayList<RadarEntry> entries = fetchData(); // 가져온 데이터 사용

        RadarDataSet dataSet = new RadarDataSet(entries, "주간 데이터");
        dataSet.setColor(Color.RED); // 색상을 빨간색으로 설정
        RadarData data = new RadarData(dataSet);
        radarChart.setData(data);
        radarChart.invalidate();
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

    // 데이터 삽입 함수 추가
    private void insertDummyData() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        // 임시 데이터 추가
        float[] dummyValues = {8f, 4f, 1f, 5f, 9f};

        for (int i = 0; i < dummyValues.length; i++) {
            values.put("value", dummyValues[i]);
            values.put("label", i);
            db.insert("radar_chart_data", null, values);
        }

        db.close();
    }

    // 데이터 가져오기 함수 추가
    private ArrayList<RadarEntry> fetchData() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM radar_chart_data", null);

        ArrayList<RadarEntry> entries = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                float value = cursor.getFloat(cursor.getColumnIndexOrThrow("value"));
                int label = cursor.getInt(cursor.getColumnIndexOrThrow("label"));
                entries.add(new RadarEntry(value, label));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return entries;
    }
}