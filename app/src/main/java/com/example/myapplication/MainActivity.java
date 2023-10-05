package com.example.myapplication;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.myapplication.aladdin.AladdinMainActivity;
import com.example.myapplication.book.BookMainActivity;
import com.example.myapplication.screen.MyForegroundService;
import com.example.myapplication.screen.ScreenOnReceiver;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{
    //네비게이션바
    private BottomNavigationView bottomNavigationView;

    //일일 솔루션
    private TextView Today_Sol;
    //firebase
    private FirebaseFirestore db;

    //독후감
    private TextView bookNum;
    private int finishBooknum;
    private int workNum = 0;

    //제한 시간
    private TextView limitTime;
    private long appUsageTime = 0;

    //앱 사용시간
    private final Handler handler = new Handler(Looper.getMainLooper());

    //추가
    private long lastUpdateTime;
    private long appUsageTimeStart;
    // 앱 사용시간 전역변수
    private String formattedAppUsageTime;
    private TextView appUseTimeTextView;

    //점수
    private TextView score_textview;
    private TextView jum;

    private int timeValue = 0;

    private long totalTime = 0;

    //알라딘
    private RelativeLayout aladdinLayout;

    private BarChart barChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 메뉴 하단바 삭제
        Utils.deleteMenuButton(this);

        // 잠금화면
        ScreenOnReceiver screenOnReceiver = new ScreenOnReceiver();
        IntentFilter screenOnFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenOnReceiver, screenOnFilter);

        // 앱 사용시간 자정 초기화
        initializeAppUsageTimeAtMidnight();
        Log.d("MainActivity","앱 사용시간 초기화 예약 성공");

        //일일 솔루션
        Today_Sol = findViewById(R.id.today_sol);

        //firebase
        db = FirebaseFirestore.getInstance();

        //알라딘 버튼
        aladdinLayout = findViewById(R.id.book_button);

        //Foreground 설정
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        //추가
        IntentFilter intentFilter = new IntentFilter("app_usage_time_intent_action");
        registerReceiver(appUsageTimeReceiver, intentFilter);

        // BottomNavigationView 초기화
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // 네비게이션 아이템 클릭 이벤트 처리
                int itemId = item.getItemId();

                if (itemId == R.id.menu_gpt) {
                    // "GPT" 아이템 클릭 시 동작
                    Intent intent1 = new Intent(MainActivity.this, ChatGpt.class);
                    startActivity(intent1);
                } else if (itemId == R.id.menu_book) {
                    // "Book" 아이템 클릭 시 동작
                    Intent intent2 = new Intent(MainActivity.this, BookMainActivity.class);
                    startActivity(intent2);
                } else if (itemId == R.id.menu_info) {
                    // "정보확인" 아이템 클릭 시 동작
                    Intent intent3 = new Intent(MainActivity.this, MyInfo.class);
                    startActivity(intent3);
                }
                return true;
            }
        });

        // 초기 선택된 네비게이션 아이템 설정.
        bottomNavigationView.setSelectedItemId(R.id.menu_home);

        // 레이더 차트 추가
        barChart = findViewById(R.id.chart);
        setData(barChart);

        //독후감 관련
        bookNum = findViewById(R.id.book_text);
        fetchFinishBookNum();

        // 제한 시간
        limitTime = findViewById(R.id.limit_time);
        fetchLimitTime(appUsageTime);

        //어플 사용 시간
        appUseTimeTextView = findViewById(R.id.app_use_time);

//========================================= 차트 그래프 터치 ================================================
        // 점수
        score_textview = findViewById(R.id.score_textview);
        jum = findViewById(R.id.jum);

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof BarEntry) {
                    BarEntry barEntry = (BarEntry) e;
                    float value = barEntry.getY();
                    int intValue = Math.round(value);

                    if (intValue == 100) {
                        score_textview.setTextSize(50);
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) score_textview.getLayoutParams();
                        params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, getResources().getDisplayMetrics());
                        score_textview.setLayoutParams(params);

                        ViewGroup.MarginLayoutParams jumParams = (ViewGroup.MarginLayoutParams) jum.getLayoutParams();
                        jumParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
                        jum.setLayoutParams(jumParams);
                    }
                    else{
                        score_textview.setTextSize(68);
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) score_textview.getLayoutParams();
                        ViewGroup.MarginLayoutParams jumParams = (ViewGroup.MarginLayoutParams) jum.getLayoutParams();

                        params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
                        jumParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());

                        score_textview.setLayoutParams(params);
                        jum.setLayoutParams(jumParams);
                    }
                    // TextView에 값을 표시
                    score_textview.setText(String.valueOf(intValue));
                }
            }

            @Override
            public void onNothingSelected() {
                // 막대를 선택하지 않았을 때의 동작을 정의
            }
        });

        aladdinLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AladdinMainActivity.class);
                startActivity(intent);
            }
        });

        //앱 종료 확인
        ExitApp();

    }
//===============================================================================================================

    //=====================================일일 솔루션=================================================================
    private void solData(ArrayList<BarEntry> entries) {
        // 가장 낮은 점수와 해당 과목을 초기값으로 설정합니다.
        float lowestScore = Float.MAX_VALUE;
        int lowestScoreSubjectLabel = 0;

        for (int i = 0; i < entries.size(); i++) {
            BarEntry entry = entries.get(i);
            float value = entry.getY();
            int label = (int) entry.getX();

            // 현재 과목의 점수가 가장 낮은지 확인하고 업데이트합니다.
            if (value < lowestScore) {
                lowestScore = value;
                lowestScoreSubjectLabel = label;
            }
        }
        // 가장 낮은 과목에 해당하는 솔루션을 설정합니다.
        String lowestScoreSubject = getSubjectByLabel(lowestScoreSubjectLabel);
        String solution = generateSolutionForSubject(lowestScoreSubject);
        Today_Sol.setText(solution);
    }

    private String getSubjectByLabel(int label) {
        switch (label) {
            case 1:
                return "독해력";
            case 2:
                return "문해력";
            case 3:
                return "어휘력";
            default:
                return "알 수 없음";
        }
    }
    private String generateSolutionForSubject(String subject) {
        String solution = "";

        switch (subject) {
            case "독해력":
                solution = "독해력을 향상시키려면 매일 정독과 관련된 기사나 글을 읽어보세요.";
                break;
            case "문해력":
                solution = "문해력을 향상시키려면 다양한 글을 읽고 요약해보세요.";
                break;
            case "어휘력":
                solution = "어휘력을 향상시키려면 매일 새로운 단어를 학습하고 사용해보세요.";
                break;
            default:
                solution = "가장 낮은 점수에 대한 솔루션을 찾을 수 없습니다.";
                break;
        }
        return solution;
    }

//===============================================================================================================

    //=====================================레이더 차트==========================================================================
    private void setData(BarChart barChart) {
        fetchData(new MyInfo.FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<BarEntry> entries) {
                for (int i = 0; i < entries.size(); i++) {
                    entries.get(i).setX(i + 1);
                }

                BarDataSet dataSet = new BarDataSet(entries, "주간 데이터");

                List<Integer> colors = new ArrayList<>();
                int startColor = Color.parseColor("#FF003A");
                int endColor = Color.parseColor("#FF006D");
                dataSet.setGradientColor(startColor, endColor);

                BarData data = new BarData(dataSet);
                data.setBarWidth(0.5f);
                barChart.invalidate();
                barChart.setData(data);

                String[] labels = {"", "독해력", "문해력", "어휘력"};

                XAxis xAxis = barChart.getXAxis();
                xAxis.setDrawGridLines(false);
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                YAxis leftAxis = barChart.getAxisLeft();
                leftAxis.setGranularity(20f);
                leftAxis.setAxisMinimum(0f);
                leftAxis.setAxisMaximum(100f);
                leftAxis.setDrawGridLines(false);
                leftAxis.setLabelCount(6, true);
                leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
                leftAxis.setDrawLabels(false);

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

                dataSet.setDrawValues(false); // 값 레이블 그리기 비활성화

                barChart.getLegend().setEnabled(false);
                barChart.getDescription().setEnabled(false);
                barChart.setScaleEnabled(false);

                barChart.animateY(800);
                solData(entries);
            }
        });
    }

//=====================================================================================================================


    //=====================================클릭 메소드==========================================================================
    private void showMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

//===============================================================================================================


//=============================== 권한 요청 메서드====================================================================

    // 다른 앱 위에 표시 권한
    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("다른 앱 위에 표시 권한 요청");
        builder.setMessage("이 앱은 다른 앱 위에 표시되는 기능이 있습니다. 허용하시겠습니까?");

        builder.setPositiveButton("허용", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestOverlayPermission();
                requestUsageStatsPermission();
            }
        });

        builder.setNegativeButton("거부", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 거절 시 처리 코드
                finish();
            }
        });
        builder.show();
    }

    private void requestUsageStatsPermission() {
        if (!hasUsageStatsPermission(getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 100);
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 100);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showPermissionDialog();
            }
        }
    }

    private boolean hasUsageStatsPermission(Context context) {
        final AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

//====================================================================================================

    // 뒤로가기 버튼 막기
    public void onBackPressed(){

    }

    //=====================================Chart==========================================================================
    private void fetchData(MyInfo.FirestoreCallback callback) {
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
    //===============================================================================================================
    // 콜백 인터페이스 추가
    private interface FirestoreCallback {
        void onDataLoaded(ArrayList<RadarEntry> entries);
    }

    // =====================================독후감 책 관련 메서드==========================================================================
    private void fetchWorkNum() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Book").document("report")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            workNum = documentSnapshot.getLong("worknum").intValue();
                            bookNum.setText(finishBooknum + " / " + workNum + " 권"); // TextView에 반영
                        }
                    }
                });
    }

    private void fetchFinishBookNum() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Book").document("finish")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            finishBooknum = documentSnapshot.getLong("booknum").intValue();
                            fetchWorkNum();
                        }
                    }
                });
    }
//====================================================================================================================================================


    //=====================================제한 시간 메서드==========================================================================
    private void fetchLimitTime(long appUsageTime) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long usedTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(appUsageTime);
        db.collection("Time").document("SetTime")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String timeString = documentSnapshot.getString("time");
                            timeValue = convertTimeStringToMinutes(timeString);
                            limitTime.setText(formattedAppUsageTime +" / " + timeValue + " min");
                            appUseTimeTextView.setText(usedTimeInMinutes + " min");

                        }
                    }
                });

    }

    private int convertTimeStringToMinutes(String timeString) {
        String[] timeParts = timeString.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        return hours * 60 + minutes;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // BroadcastReceiver 해제
        unregisterReceiver(appUsageTimeReceiver);
    }
//===============================================================================================================


    //=====================================핸드폰 사용시간==========================================================================
    public static final String APP_USAGE_TIME_KEY = "app_usage_time";

    private BroadcastReceiver appUsageTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long appUsageTime = intent.getLongExtra(APP_USAGE_TIME_KEY, 0);

            fetchLimitTime(appUsageTime);
        }
    };
//===============================================================================================================


    //=====================================어플 사용시간==========================================================================
    private long getAppUsageTime(Context context, String myapplication) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // 오늘 자정 시간 구하기
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        // 현재 시간을 구하기
        long currentTime = System.currentTimeMillis();

        // 시스템에서 앱 사용정보 가져오기
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
        );

        if (usageStatsList != null) {
            for (UsageStats usageStats : usageStatsList) {
                if (myapplication.equals(usageStats.getPackageName())) {
                    totalTime += usageStats.getTotalTimeInForeground();
                }
            }
        }
        return totalTime;
    }


    private String formatMillis(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);

        return String.format(Locale.getDefault(), "%02d", minutes);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called");
        appUsageTimeStart = System.currentTimeMillis(); // onResume() 호출 시 시작 시간 저장
        requestOrCheckPermission();
        lastUpdateTime = System.currentTimeMillis(); // 현재 시간을 기록
        // Runnable 시작
        handler.post(updateAppUsageTimeRunnable);
        long appUsageTimeInMinutes = Long.parseLong(formattedAppUsageTime);
        fetchLimitTime(appUsageTimeInMinutes);
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Runnable 정지 및 제거
        handler.removeCallbacks(updateAppUsageTimeRunnable);
        Log.d("MainActivity", "onPause called");
        SharedPreferences sharedPreferences = getSharedPreferences("AppData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("formattedAppUsageTime", formattedAppUsageTime);
        editor.putInt("finishBooknum", finishBooknum);
        editor.putLong("timeValue", timeValue);
        editor.putInt("workNum", workNum);
        editor.apply();
    }
    private void requestOrCheckPermission() {
        updateAppUsageTime();
    }

    // 어플 사용시간 가져오기

    private void updateAppUsageTime() {
        long elapsedTime = System.currentTimeMillis() - appUsageTimeStart; // 어플 사용 시작 시간으로부터 경과한 시간 계산
        long appUsageTimeInMillis = getAppUsageTime(getApplicationContext(), getPackageName()) + elapsedTime; // 전체 사용 시간에 경과 시간 더하기
        formattedAppUsageTime = formatMillis(appUsageTimeInMillis);
        Log.d("AppUsageTime", "App usage time: " + formattedAppUsageTime);
    }
    private final Runnable updateAppUsageTimeRunnable = new Runnable() {
        @Override
        public void run() {
            long updateTimeThreshold = 1000 * 60; // 1분
            if (System.currentTimeMillis() - lastUpdateTime >= updateTimeThreshold) {
                updateAppUsageTime();
                lastUpdateTime = System.currentTimeMillis();
                Log.d("AppUsageTime", "App usage time updated"); // 작동 로그 추가
            }
            handler.postDelayed(this, updateTimeThreshold); // 1초 간격으로 반복 실행
        }
    };
//===============================================================================================================

    //=========================================앱 종료 메서드=================================================
    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("앱 종료 확인");
        builder.setMessage("앱을 종료하시겠습니까?");

        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finishAffinity();
                System.exit(0);
            }
        });

        builder.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 취소 시 처리 코드
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }
    private void ExitApp(){
        SharedPreferences sharedPreferences = getSharedPreferences("AppData", Context.MODE_PRIVATE);
        String formattedAppUsageTime = sharedPreferences.getString("formattedAppUsageTime", "0");
        int finishBooknum = sharedPreferences.getInt("finishBooknum", 0);
        long timeValue = sharedPreferences.getLong("timeValue", 0);
        int workNum = sharedPreferences.getInt("workNum", 0);

        if (Long.parseLong(formattedAppUsageTime) >= timeValue && finishBooknum >= workNum){
            showExitDialog();
        }
        if (Long.parseLong(formattedAppUsageTime) >= timeValue){
            TargetTime(formattedAppUsageTime);
        }
    }
//========================================================================================================================


    //===============================firebase에 사용시간 추가====================================================================
    private void TargetTime(String formattedAppUsageTime){
        DocumentReference documentReference = db.collection("Time").document("TargetTime");

        documentReference.update("Ttime", formattedAppUsageTime)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("MainActivity", "App usage time successfully updated.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("MainActivity", "Error updating app usage time", e);
                    }
                });
    }
    // 추가: 자정까지 남은 시간을 계산하고 초기화를 수행하는 메서드
    private void initializeAppUsageTimeAtMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        // 자정까지 남은 시간을 계산합니다.
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long timeUntilMidnight = calendar.getTimeInMillis() + 24 * 60 * 60 * 1000 - System.currentTimeMillis();

        // 계산된 시간만큼 대기 후 초기화를 수행합니다.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 초기화 작업을 수행합니다.
                totalTime = 0;
                Log.d("MainActivity", "Total time reset to 0 at midnight.");
            }
        }, timeUntilMidnight);
    }
}