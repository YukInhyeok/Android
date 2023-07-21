package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
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
import android.widget.NumberPicker;
import android.widget.TextView;

import com.example.myapplication.book.BookMainActivity;
import com.example.myapplication.receiver.WeeklyResetReceiver;
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

    private TextView textView7;

    private TextView goalScoreText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        TextView textView4 = findViewById(R.id.textView4);
        textView4.setText(getCurrentMonthAndWeek());

        TextView textView5 = findViewById(R.id.textView5);
        textView5.setText(getCurrentWeekDates());

        textView7 = findViewById(R.id.sol);

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
        setWeeklyResetAlarm();

        goalScoreText = findViewById(R.id.goal_score);
        goalScoreText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGoalScorePopup();
            }
        });
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
    private void setWeeklyResetAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, WeeklyResetReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);

        // 매주 월요일 자정에 대한 Calendar 객체 생성
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        // 매주 월요일에 평균 값이 0으로 변경되도록 알람 설정
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
    }

    private void showGoalScorePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyInfo.this);
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.goal_score_popup, null);
        builder.setView(popupView);

        final NumberPicker numberPicker = popupView.findViewById(R.id.b_work_num_picker);
        numberPicker.setMinValue(0); // 최소값 설정
        numberPicker.setMaxValue(100); // 최대값 설정
        Button goalScoreBtn = popupView.findViewById(R.id.b_work_btn);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        goalScoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedGoalScore = numberPicker.getValue();
                goalScoreText.setText("목표 점수: " + selectedGoalScore);
                alertDialog.dismiss();

                // 목표 점수로 차트 업데이트 (새로 추가된 코드)
                updateChartWithGoalScore(selectedGoalScore);
            }
        });
    }

    private void updateChartWithGoalScore(int goalScore) {
        fetchData(new FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<BarEntry> entries) {
                // 사용자가 선택한 목표 점수보다 작은 값의 레이블 추출
                List<String> labelsSmallerThanGoalScore = new ArrayList<>();

                for (BarEntry entry : entries) {
                    if (entry.getY() < goalScore) {
                        int index = Math.round(entry.getX());
                        // 레이블 배열은 1부터 시작함에 유의하세요.
                        if (index == 0) {
                            labelsSmallerThanGoalScore.add("독해력");
                        } else if (index == 1) {
                            labelsSmallerThanGoalScore.add("문해력");
                        } else if (index == 2) {
                            labelsSmallerThanGoalScore.add("어휘력");
                        }
                    }
                }

                // 결과 출력
                if (!labelsSmallerThanGoalScore.isEmpty()) {
                    String labelsStr = String.join(", ", labelsSmallerThanGoalScore);
                    textView7.setText("목표점수보다 낮은 유형은 " + labelsStr + " 입니다.");
                } else {
                    textView7.setText("축하합니다. 모두 목표점수보다 높습니다 ^^");
                }
            }
        });
    }
}