package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.book.BookMainActivity;
import com.example.myapplication.book.ResetCountReceiver;
import com.example.myapplication.screen.MyForegroundService;
import com.example.myapplication.screen.ScreenOnReceiver;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{
    //네비게이션바
    private BottomNavigationView bottomNavigationView;

    //포인트 관련
    private EditText editTextNumber;
    private Button point;
    private TextView pointNum;

    // 포인트 잔액
    private int cashValue;

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

    private int timeValue = 0;



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

        //firebase
        db = FirebaseFirestore.getInstance();

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
        HorizontalBarChart barChart = findViewById(R.id.chart);
        setData(barChart);

        // 포인트 관련
        editTextNumber = findViewById(R.id.editTextNumber);
        point = findViewById(R.id.point);
        pointNum = findViewById(R.id.point_num);
        initPointListener();

        //독후감 관련
        bookNum = findViewById(R.id.book_text);
        setAlarmToResetCount();
        fetchFinishBookNum();

        // 제한 시간
        limitTime = findViewById(R.id.limit_time);
        fetchLimitTime(appUsageTime);

        //어플 사용 시간
        appUseTimeTextView = findViewById(R.id.app_use_time);

        //앱 종료 확인
        ExitApp();
//==================================Point 메서드===================================================================
        point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputPoint = Integer.parseInt(editTextNumber.getText().toString());

                if (inputPoint <= cashValue) {
                    int remainingPoint = cashValue - inputPoint;

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("UserCoin").document("Coin")
                            .update("point", remainingPoint)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("MainActivity", "DocumentSnapshot successfully updated!");
                                    showMessage(inputPoint + "포인트가 사용되었습니다"); // 메시지 표시

                                    // 사용된 포인트를 UseCoin 컬렉션에 추가
                                    Map<String, Object> usedPointData = new HashMap<>();
                                    usedPointData.put("usedPoint", inputPoint);
                                    usedPointData.put("timestamp", FieldValue.serverTimestamp()); // 서버 타임스탬프로 시간 기록

                                    db.collection("UseCoin")
                                            .add(usedPointData)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Log.d("MainActivity", "Used point added to UseCoin collection");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("MainActivity", "Error adding used point to UseCoin collection", e);
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("MainActivity", "Error updating document", e);
                                }
                            });
                    editTextNumber.setText(""); // 입력한 숫자 지우기
                } else {
                    showMessage("보유한 포인트보다 많은 포인트를 사용할 수 없습니다.");
                }
            }
        });
    }
//===============================================================================================================


//=====================================레이더 차트==========================================================================
private void setData(HorizontalBarChart barChart) {
    fetchData(new MyInfo.FirestoreCallback() {
        @Override
        public void onDataLoaded(ArrayList<BarEntry> entries) {
            // Update the index number for entries
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setX(i + 1);
            }

            BarDataSet dataSet = new BarDataSet(entries, "주간 데이터");

            // Set colors for each bar
            List<Integer> colors = new ArrayList<>();
            colors.add(Color.BLUE); // 독해력
            colors.add(Color.GREEN); // 문해력
            colors.add(Color.RED); // 어휘력
            dataSet.setColors(colors);

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.5f);
            barChart.setData(data);
            barChart.invalidate();

            String[] labels = {"", "독해력", "문해력", "어휘력"};

            XAxis xAxis = barChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

            // Disable YAxis labels
            YAxis leftAxis = barChart.getAxisLeft();
            leftAxis.setGranularity(20f);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setAxisMaximum(100f);
            leftAxis.setDrawGridLines(true);

            // Y축 라벨이 0, 20, 40, 60, 80, 100 위치에만 표시되게 하려면 granulartiy를 설정하세요.
            leftAxis.setLabelCount(6, true);

            YAxis rightYAxis = barChart.getAxisRight();
            rightYAxis.setDrawLabels(false);
            rightYAxis.setDrawGridLines(false); // 오른쪽 Y축 그리드 선을 숨깁니다.

            // Disable scaling
            barChart.setScaleEnabled(false);

            // 추가된 코드: 범례 숨기기
            barChart.getLegend().setEnabled(false);
        }
    });
}

//=====================================================================================================================

//=====================================포인트 관련==========================================================================
    private void initPointListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("UserCoin").document("Coin")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            cashValue = documentSnapshot.getLong("point").intValue();
                            pointNum.setText(Integer.toString(cashValue));
                            editTextNumber.setHint("최대 " + cashValue);
                        }
                    }
                });
    }
//===============================================================================================================


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

    private void requestUsageStatsPermission() {
        if (!hasUsageStatsPermission(getApplicationContext())) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    private boolean hasUsageStatsPermission(Context context) {
        final AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

//====================================================================================================

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
                            bookNum.setText("책: " + finishBooknum + " / " + workNum + " 권"); // TextView에 반영
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
                            // finishBooknum 값을 가져온 후 fetchWorkNum 메소드를 호출합니다.
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
                            limitTime.setText("시간: " + formattedAppUsageTime +" / " + timeValue + " 분");
                            appUseTimeTextView.setText("총 시간: " + usedTimeInMinutes + " 분");

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

    private void setAlarmToResetCount() {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ResetCountReceiver.class);

        PendingIntent alarmIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }
//===============================================================================================================


//=====================================핸드폰 사용시간==========================================================================
    public static final String APP_USAGE_TIME_KEY = "app_usage_time";

    private BroadcastReceiver appUsageTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long appUsageTime = intent.getLongExtra(APP_USAGE_TIME_KEY, 0);
            // 사용 시간 정보를 처리하는 코드를 여기에 작성하십시오.
            fetchLimitTime(appUsageTime);
        }
    };
//===============================================================================================================


//=====================================어플 사용시간==========================================================================
    private long getAppUsageTime(Context context, String myapplication) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // 오늘 자정 시간을 구합니다.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        // 현재 시간을 구합니다.
        long currentTime = System.currentTimeMillis();

        // 설정한 기간의 앱 사용 정보를 가져옵니다.
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
        );

        long totalTime = 0;

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

    private void updateAppUsageTime() {
//        TextView appUseTimeTextView = findViewById(R.id.app_use_time);
        long elapsedTime = System.currentTimeMillis() - appUsageTimeStart; // 어플 사용 시작 시간으로부터 경과한 시간 계산
        long appUsageTimeInMillis = getAppUsageTime(getApplicationContext(), getPackageName()) + elapsedTime; // 전체 사용 시간에 경과 시간 더하기
        formattedAppUsageTime = formatMillis(appUsageTimeInMillis);
//        appUseTimeTextView.setText("총 시간: " + formattedAppUsageTime);
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
}
