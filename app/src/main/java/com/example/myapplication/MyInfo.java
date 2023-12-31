package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.example.myapplication.aladdin.AladdinMainActivity;
import com.example.myapplication.book.BookMainActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyInfo extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    //firebase
    private FirebaseFirestore db;

    private TextView score_text, today_score, date_view, day_view, lit_text, voc_text, gra_text, wkscore_text, lit_score, voc_score, gra_score;

    private RelativeLayout BookBtn;

    private static final String WEEKLY_RESET_PREF = "WeeklyResetAlarmPref";
    private static final String WEEKLY_RESET_ALARM_SET = "WeeklyResetAlarmSet";

    private PeriodicWorkRequest resetWeeklyWorkRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        // 메뉴 하단바 삭제
        Utils.deleteMenuButton(this);
        
        //버튼
        BookBtn = findViewById(R.id.book_button);

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

        if (!isAlarmSet) {
            long initialDelay = calculateDelayForNextMonday();
            resetWeeklyWorkRequest =
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

            Log.d("MY_APP_TAG", "제발 정상 작동좀 해라");
        }
        if (resetWeeklyWorkRequest != null) {
            // WorkManager 상태 변경을 추적할 LiveData
            LiveData<WorkInfo> workInfoLiveData = WorkManager.getInstance(this).getWorkInfoByIdLiveData(resetWeeklyWorkRequest.getId());

            workInfoLiveData.observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    Log.d("MY_APP_TAG", "onChanged called: " + workInfo);
                    if (workInfo != null) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            // 작업이 성공적으로 완료되었을 때 수행할 동작을 여기에 추가하세요.
                            // 예: 알림 표시 또는 작업 완료 메시지 표시
                            Log.i("MY_APP_TAG", "주간 데이터 재설정이 완료되었습니다.");
                        } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                            // 작업이 실패한 경우 수행할 동작을 여기에 추가하세요.
                            // 예: 오류 메시지 표시 또는 재시도 로직 실행
                            Log.e("MY_APP_TAG", "주간 데이터 재설정이 실패했습니다.");
                        }
                    }
                }
            });
        }

        // ============================================== 오늘 점수 ================================================
        today_score = findViewById(R.id.today_score);
        fetchTodayDataAndCalculateAverage();

        // ============================================== 오늘 날짜 ================================================
        date_view = findViewById(R.id.date_view);
        day_view = findViewById(R.id.day_view);

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd", Locale.getDefault());
        String formattedDate = dateFormat.format(currentDate);
        date_view.setText(formattedDate);

        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        String formattedDay = dayFormat.format(currentDate);
        String formattedDayWithParentheses = "(" + formattedDay + ")";
        day_view.setText(formattedDayWithParentheses);

        //========================================= 차트 그래프 터치 ================================================
        // 점수
        score_text = findViewById(R.id.score_text);
        lit_text = findViewById(R.id.lit_text);
        voc_text = findViewById(R.id.voc_text);
        gra_text = findViewById(R.id.gra_text);
        wkscore_text = findViewById(R.id.wkscore_text);
        lit_score = findViewById(R.id.lit_score);
        voc_score = findViewById(R.id.voc_score);
        gra_score = findViewById(R.id.gra_score);
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof BarEntry) {
                    BarEntry barEntry = (BarEntry) e;
                    float value = barEntry.getY();
                    int intValue = Math.round(value);

                    score_text.setTextSize(18);
                    score_text.setText(String.valueOf(intValue));
                    lit_text.setTextColor(Color.BLACK);
                    voc_text.setTextColor(Color.BLACK);
                    gra_text.setTextColor(Color.BLACK);
                    wkscore_text.setTextColor(Color.BLACK);
                    lit_score.setTextColor(Color.BLACK);
                    voc_score.setTextColor(Color.BLACK);
                    gra_score.setTextColor(Color.BLACK);

                    String[] daysOfWeek = {"", "Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun"};
                    int selectedDayIndex = (int) barEntry.getX();
                    String selectedDay = daysOfWeek[selectedDayIndex];

                    fetchSubjectScores(selectedDay, new SubjectScoresCallback() {
                        @Override
                        public void onSubjectScoresLoaded(int literacy, int vocabulary, int grammar) {
                            // 각 과목별 점수를 출력
                            lit_score.setText(String.valueOf(literacy));
                            voc_score.setText(String.valueOf(vocabulary));
                            gra_score.setText(String.valueOf(grammar));
                        }
                    });
                }
            }

            @Override
            public void onNothingSelected() {

            }
        });


        //버튼 메소드
        BookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MyInfo.this, AladdinMainActivity.class);
                startActivity(intent);
            }
        });
    }

    // 파이어베이스에서 데이터 불러오기
    private void setData(BarChart barChart) {
        fetchData(new FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<BarEntry> entries) {
                for (int i = 0; i < entries.size(); i++) {
                    entries.get(i).setX(i + 1);
                }
                ArrayList<IBarDataSet> dataSets = new ArrayList<>();
                for (BarEntry entry : entries) {
                    BarDataSet dataSet = new BarDataSet(Arrays.asList(entry), "요일별 점수");

                    int startColor = Color.parseColor("#000000");
                    int endColor = Color.parseColor("#202C73");
                    dataSet.setGradientColor(startColor, endColor);
                    dataSet.setDrawValues(false);
                    dataSets.add(dataSet);
                }

                BarData data = new BarData(dataSets);
                data.setBarWidth(0.5f);
                barChart.setData(data);
                barChart.invalidate();

                String[] labels = {"", "Mon", "Tue", "Wed", "Thr", "Fri", "Sat", "Sun"};

                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setDrawAxisLine(false);
                xAxis.setAxisMinimum(0f);
                xAxis.setAxisMaximum(8f);
                xAxis.setDrawGridLines(false);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                YAxis leftAxis = barChart.getAxisLeft();
                leftAxis.setGranularity(20f);
                leftAxis.setAxisMinimum(0f);
                leftAxis.setAxisMaximum(100f);
                leftAxis.setDrawGridLines(false);
                leftAxis.setDrawLabels(false);
                leftAxis.setDrawAxisLine(false);

                YAxis rightYAxis = barChart.getAxisRight();
                rightYAxis.setGranularity(20f);
                rightYAxis.setAxisMinimum(0f);
                rightYAxis.setAxisMaximum(100f);
                rightYAxis.setDrawGridLines(false);
                leftAxis.setDrawAxisLine(false);
                rightYAxis.setDrawAxisLine(false);
                xAxis.setDrawAxisLine(false);
                rightYAxis.setLabelCount(6, true);
                rightYAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART); // 값 레이블을 차트 바깥쪽에 표시

                barChart.getLegend().setEnabled(false);
                barChart.getDescription().setEnabled(false);
                barChart.setScaleEnabled(false);

                barChart.animateY(800);

            }
        });
    }

    // 파이어베이스
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

    // 금일 데이터 불러오기
    private void fetchtodayData(FirestoreCallback callback) {
        db.collection("Chart").orderBy("label")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<BarEntry> entries = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                int average = document.getLong("value").intValue();
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

    private void fetchSubjectScores(String selectedDay, SubjectScoresCallback callback) {
        db.collection("WeekChart")
                .document(selectedDay) // 선택한 요일에 해당하는 문서를 가져옵니다.
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                int literacy = document.getLong("literacy").intValue();
                                int vocabulary = document.getLong("vocabulary").intValue();
                                int grammar = document.getLong("read").intValue();

                                // 각 과목별 점수를 콜백을 통해 반환
                                callback.onSubjectScoresLoaded(literacy, vocabulary, grammar);
                            } else {
                                Log.e("MyInfo", "No such document");
                            }
                        } else {
                            Log.e("MyInfo", "Error fetching document", task.getException());
                        }
                    }
                });
    }


    // 콜백 인터페이스 추가
    public interface FirestoreCallback {
        void onDataLoaded(ArrayList<BarEntry> entries);
    }
    public interface SubjectScoresCallback {
        void onSubjectScoresLoaded(int literacy, int vocabulary, int grammar);
    }


    // 주간 데이터 초기화
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

// 오늘의 점수 가져오기
    private void fetchTodayDataAndCalculateAverage() {
        fetchtodayData(new FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<BarEntry> entries) {
                int totalValue = 0;
                int dataCount = entries.size();

                // 데이터에서 value 필드 값을 추출하고 총합 계산
                for (BarEntry entry : entries) {
                    totalValue += (int) entry.getY();
                }

                // 데이터가 없을 경우 0을 출력하거나 다른 처리를 수행할 수 있습니다.
                if (dataCount == 0) {
                    // 예: today_score.setText("데이터 없음");
                } else {
                    // 평균 계산
                    int averageValue = totalValue / dataCount;

                    // 평균 값을 today_score 텍스트뷰에 출력
                    today_score.setText(String.valueOf(averageValue));
                }
            }
        });
    }

}