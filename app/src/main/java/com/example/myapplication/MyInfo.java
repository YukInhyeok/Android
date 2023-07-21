package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.example.myapplication.book.BookMainActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyInfo extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    //firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        TextView textView4 = findViewById(R.id.textView4);
        textView4.setText(getCurrentMonthAndWeek());

        TextView textView5 = findViewById(R.id.textView5);
        textView5.setText(getCurrentWeekDates());

        TextView goal_score = findViewById(R.id.goal_score);


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
        HorizontalBarChart barChart = findViewById(R.id.info_chart);
        TextView textBox7 = findViewById(R.id.sol);

        setData(barChart,textBox7);

        goal_score.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBWorkPopup();
            }
        }
    }


    private void showBWorkPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyInfo.this);
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.b_work_popup, null);
        builder.setView(popupView);

        final NumberPicker numberPicker = popupView.findViewById(R.id.b_work_num_picker);
        numberPicker.setMinValue(1); // 최소값 설정
        numberPicker.setMaxValue(100); // 최대값 설정
        Button bWorkBtn = popupView.findViewById(R.id.b_work_btn);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }


    private void setData(HorizontalBarChart barChart, TextView textBox7) {
        fetchData(new FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<BarEntry> entries) {
                // Update the index number for entries
                for (int i = 0; i < entries.size(); i++) {
                    entries.get(i).setX(i + 1);
                }

                // 가장 낮은 값의 인덱스 찾기
                int lowestIndex = 0;
                float lowestValue = Float.MAX_VALUE;
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getY() < lowestValue) {
                        lowestIndex = i;
                        lowestValue = entries.get(i).getY();
                    }
                }

                BarDataSet dataSet = new BarDataSet(entries, "주간 데이터");

                // Set colors for each bar
                List<Integer> colors = new ArrayList<>();
                colors.add(Color.BLUE); // 독해력
                colors.add(Color.GREEN); // 문해력
                colors.add(Color.RED); // 어휘력
                dataSet.setColors(colors);

                YAxis leftAxis = barChart.getAxisLeft();
                leftAxis.setGranularity(20f);
                leftAxis.setAxisMinimum(0f);
                leftAxis.setAxisMaximum(100f);
                leftAxis.setDrawGridLines(false);
                leftAxis.setTextColor(Color.BLACK);

                // Set Y axis style
                YAxis rightAxis = barChart.getAxisRight();
                rightAxis.setEnabled(false);

                BarData data = new BarData(dataSet);
                data.setBarWidth(0.5f);

                barChart.setData(data);
                barChart.invalidate();

                String[] labels = {"", "독해력", "문해력", "어휘력"};

                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setDrawAxisLine(false);
                xAxis.setDrawGridLines(false);

                YAxis rightYAxis = barChart.getAxisRight();
                rightYAxis.setDrawLabels(false);

                // Disable scaling
                barChart.setScaleEnabled(false);

                String lowestLabel = labels[lowestIndex + 1];
                textBox7.setText("평균보다 "+ lowestLabel + "이(가) 부족하므로 "); // 가장 낮은 값의 레이블을 textBox7에 설정
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
        db.collection("Chart").orderBy("label")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<BarEntry> entries = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                float value = document.getDouble("value").floatValue();
                                int label = document.getLong("label").intValue();
                                // RadarEntry 대신 BarEntry 사용
                                entries.add(new BarEntry(label, value));
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

}